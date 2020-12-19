package com.djcrontab.code.controlsurface

import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.truncate


class DeviceState(val device: Int) {
    val remoteName = MutableStateFlow("")
    val name = remoteName.asStateFlow()
}

data class ControllerKey(val device: Int, val control: Int)

class ControllerState(
    val device: Int,
    val control: Int,
    val sendToBitwig: Channel<String>,
    val viewModelScope: CoroutineScope
) {
    val remoteName = MutableStateFlow("")
    val name = remoteName.asStateFlow()

    val remoteDisplayValue = MutableStateFlow("")
    val displayValue = remoteDisplayValue.asStateFlow()

    val remoteParameterValue = MutableStateFlow(0f)
    val parameterValue = remoteParameterValue.asStateFlow()

    private var pauseRemoteUpdates = false
        set(value) {
            if (value != field) {
                field = value
                if (value) remoteParameterValue.value = lastKnownValue
            }
        }
    private var lastKnownValue = 0f

    fun setValueFromRemote(value: Float) {
        val newValue = truncate(value * 1000f) / 1000f
        lastKnownValue = newValue
        if (!pauseRemoteUpdates) {
            remoteParameterValue.value = newValue
        }
    }

    fun onValueChanged(value: Float) {
        val newValue = truncate(value * 1000f) / 1000f

        // seems broken with local changes now ...
        if (newValue != remoteParameterValue.value) {
            remoteParameterValue.value = newValue
            lastKnownValue = newValue

            val message = "value,$device,$control,${remoteParameterValue.value}"

            CoroutineScope(Dispatchers.IO + viewModelScope.coroutineContext).launch {
                withContext(Dispatchers.IO) {
                    this@ControllerState.sendToBitwig.send(message)
                }
            }
        }
    }

    fun focus() {
        CoroutineScope(Dispatchers.IO + viewModelScope.coroutineContext).launch {
            val message = "focus,$device"
            Log.v("ControlSurface", message)
            withContext(Dispatchers.IO) {
                this@ControllerState.sendToBitwig.send(message)
            }
        }
    }

    var touched = MutableLiveData(false)

    init {
        touched.observeForever {
            Log.v("touched", "touched $it")
            this.pauseRemoteUpdates = it
            CoroutineScope(Dispatchers.IO + viewModelScope.coroutineContext).launch {
                val message = "touch,$device,$control,${if (it) 1 else 0}"
                Log.v("ControlSurface", message)
                withContext(Dispatchers.IO) {
                    this@ControllerState.sendToBitwig.send(message)
                }
            }
        }
    }
}

class ControllerStates : HashMap<ControllerKey, ControllerState>()

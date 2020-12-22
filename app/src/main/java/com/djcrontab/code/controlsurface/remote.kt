package com.djcrontab.code.controlsurface

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.truncate


class DeviceState(
    val device: Int,
    val sendToBitwig: Channel<String>
) {
    val remoteName = MutableStateFlow("")
    val name = remoteName.asStateFlow()

    val remoteColor = MutableStateFlow(Color(0,0,0))
    val color = remoteColor.asStateFlow()

    val remotePlaying = MutableStateFlow(false)
    val playing = remotePlaying.asStateFlow()

    fun nextPage() {
        CoroutineScope(Dispatchers.IO).launch {
            sendToBitwig.send("next,$device")
        }
    }

    fun previousPage() {
        CoroutineScope(Dispatchers.IO).launch {
            sendToBitwig.send("previous,$device")
        }
    }

    fun pin() {
        CoroutineScope(Dispatchers.IO).launch {
            sendToBitwig.send("pin,$device")
        }
    }
}

data class ControllerKey(val device: Int, val control: Int)

val NOTES = arrayOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
enum class ValueDisplayMode {
    REMOTE, NOTE, BYTE
}

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

    val displayMode = mutableStateOf(ValueDisplayMode.REMOTE)

    val remoteParameterValue = MutableStateFlow(0f)
    val parameterValue = remoteParameterValue.asStateFlow()

    private var pauseRemoteUpdates = false
        set(value) {
            if (value != field) {
                field = value
                if (value) {
                    // avoid last remote change to override the value that was just set when releasing,
                    // so send again
                    onValueChanged(lastKnownValue)
                }
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
        val truncatedValue = when (displayMode.value) {
            ValueDisplayMode.REMOTE -> {
                truncate(value * 1000f) / 1000f
            }
            ValueDisplayMode.NOTE, ValueDisplayMode.BYTE -> {
                // bitwig uses rounding, make sure we set it at fixed steps to make the display
                // consistent
                truncate(value * 127f) / 127f
            }
        }

        if (truncatedValue != remoteParameterValue.value) {
            // locally still use the full value to make sure UI is as fluid as it can be
            remoteParameterValue.value = value
            lastKnownValue = value

            val message = "value,$device,$control,${truncatedValue}"

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

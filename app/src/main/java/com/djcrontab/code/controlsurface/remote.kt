package com.djcrontab.code.controlsurface

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext
import kotlin.math.truncate
import kotlin.reflect.KMutableProperty0


fun conflateUpdate(jobProperty: KMutableProperty0<Job?>, context: CoroutineContext, setter: suspend () -> Unit): Job {
    val job : Job? = jobProperty.get()
    if (job != null && job.isActive) {
        job.cancel()
    }

    return CoroutineScope(context).launch {setter()}.apply {
        jobProperty.set(this)
    }
}

class DeviceState(val device: Int) {
    private val _name = MutableLiveData("")
    val name: LiveData<String> = _name

    var remoteNameChangedJob : Job? = null
    fun remoteNameChanged(value: String) {
        conflateUpdate(this::remoteNameChangedJob, Dispatchers.Main) {
            _name.value = value
        }
    }
}

data class ControllerKey(val device: Int, val control: Int)

class ControllerState(
    val device: Int,
    val control: Int,
    val sendToBitWig: Channel<String>,
    val viewModelScope: CoroutineScope
) {
    private val _name = MutableLiveData("")
    val name: LiveData<String> = _name

    private val _displayValue = MutableLiveData("")
    val displayValue: LiveData<String> = _displayValue

    private val _parameterValue = MutableLiveData(0f)
    val parameterValue: LiveData<Float> = _parameterValue

    var remoteNameChangedJob : Job? = null
    fun remoteNameChanged(value: String): Job? {
        return conflateUpdate(this::remoteNameChangedJob, Dispatchers.Main) {
            _name.value = value
        }
    }

    var remoteDisplayValueChangedJob : Job? = null
    fun remoteDisplayValueChanged(value: String): Job? {
        return conflateUpdate(this::remoteDisplayValueChangedJob, Dispatchers.Main) {
            _displayValue.value = value
        }
    }

    var pauseRemoteUpdates = false
        set(value) {
            if (value != field) {
                field = value
                if (value) _parameterValue.postValue(lastKnownRemoteValue)
            }
        }
    private var lastKnownRemoteValue = 0f

    var remoteValueChangedJob : Job? = null
    fun remoteValueChanged(value: Float): Job? {
        val newValue = truncate(value * 1000f) / 1000f
        lastKnownRemoteValue = newValue
        if (pauseRemoteUpdates) return null

        return conflateUpdate(this::remoteValueChangedJob, Dispatchers.Main) {
            _parameterValue.value = newValue
        }
    }

    var valueChangedJob : Job? = null
    fun onValueChanged(value: Float): Job? {
        val newValue = truncate(value * 1000f) / 1000f

        if (newValue != _parameterValue.value) {
            _parameterValue.value = newValue

            val message = "value,$device,$control,${_parameterValue.value}"

            return conflateUpdate(this::valueChangedJob, Dispatchers.IO + viewModelScope.coroutineContext) {
                this.sendToBitWig.send(message)
            }
        }
        return null
    }

    fun focus() {
        val sendToBitwig = this.sendToBitWig

        CoroutineScope(Dispatchers.IO + viewModelScope.coroutineContext).launch {
            val message = "focus,$device"
            Log.v("ControlSurface", message)
            sendToBitwig.send(message)
        }
    }
}

class ControllerStates : HashMap<ControllerKey, ControllerState>()

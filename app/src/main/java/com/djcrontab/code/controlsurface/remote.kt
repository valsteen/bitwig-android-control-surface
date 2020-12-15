package com.djcrontab.code.controlsurface

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.math.truncate

data class ControllerKey(val device: Int, val control: Int)

class ControllerState(val device: Int, val control: Int, val sendToBitWig: Channel<String>) {
    private val _name = MutableLiveData("")
    val name: LiveData<String> = _name

    private val _displayValue = MutableLiveData("")
    val displayValue: LiveData<String> = _displayValue

    private val _parameterValue = MutableLiveData(0f)
    val parameterValue: LiveData<Float> = _parameterValue

    fun remoteNameChanged(value: String) {
        _name.value = value
    }

    fun remoteDisplayValueChanged(value: String) {
        _displayValue.value = value
    }

    var pauseRemoteUpdates = false
        set(value) {
            if (value != field) {
                field = value
                if (value) _parameterValue.value = lastKnownRemoteValue
            }
        }
    private var lastKnownRemoteValue = 0f

    fun setValueFromRemote(value: Float) {
        val newValue = truncate(value * 1000f) / 1000f
        lastKnownRemoteValue = newValue
        if (pauseRemoteUpdates) return

        _parameterValue.value = newValue
    }

    fun onValueChanged(value: Float) {
        val newValue = truncate(value * 1000f) / 1000f

        if (newValue != _parameterValue.value) {
            _parameterValue.value = newValue

            val sendToBitwig = this.sendToBitWig
            CoroutineScope(Dispatchers.IO).launch {
                val message = "value,$device,$control,${_parameterValue.value}"
                Log.v("ControlSurface", message)
                sendToBitwig.send(message)
            }
        }
    }

    fun focus() {
        val sendToBitwig = this.sendToBitWig

        CoroutineScope(Dispatchers.IO).launch {
            val message = "focus,$device"
            Log.v("ControlSurface", message)
            sendToBitwig.send(message)
        }
    }
}

class ControllerStates : HashMap<ControllerKey, ControllerState>()

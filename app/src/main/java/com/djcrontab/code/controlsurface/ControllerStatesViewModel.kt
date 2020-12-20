package com.djcrontab.code.controlsurface

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect

val DEVICES_PER_PAGE = 10
val PAGES = 2

class ControllerStatesViewModel : ViewModel() {
    private val controllerStates = ControllerStates()
    private val deviceStates = mutableListOf<DeviceState>()

    fun get(device: Int, control: Int): ControllerState {
        return controllerStates[ControllerKey(device, control)]!!
    }

    fun getDevice(device: Int): DeviceState {
        return deviceStates[device]
    }

    init {
        val sendToBitwig = Channel<String>(1000);

        for (device in 0 until DEVICES_PER_PAGE * PAGES) {
            deviceStates.add(DeviceState(device, sendToBitwig))
            for (control in 0..7) {
                val controlState =
                    ControllerState(device, control, sendToBitwig = sendToBitwig, viewModelScope)
                controllerStates[ControllerKey(device, control)] = controlState
            }
        }

        val controllerStates = this.controllerStates

        CoroutineScope(Dispatchers.IO + viewModelScope.coroutineContext).launch {
            val connection = BitwigConnection(sendToBitwig, "192.168.2.102", 60123)
            withContext(Dispatchers.IO) {
                connection.start()

                connection.reader().collect { message ->
                    val parts = message.split(",")
                    val device = parts[0].toInt()

                    when (parts[1]) {
                        "devicename" -> {
                            deviceStates[device].remoteName.value = parts[2]
                            return@collect
                        }
                        "playing" -> {
                            val isPlaying = parts[2].toInt() > 0
                            deviceStates[device].remotePlaying.value = isPlaying
                            return@collect
                        }
                        "color" -> {
                            val red = parts[2].toInt()
                            val green = parts[3].toInt()
                            val blue = parts[4].toInt()
                            deviceStates[device].remoteColor.value = Color(red, green, blue)
                            return@collect
                        }
                    }

                    val control = parts[1].toInt()

                    when (parts[2]) {
                        "name" -> {
                            controllerStates[ControllerKey(device, control)]!!.remoteName.value = parts[3]
                        }
                        "value" -> {
                            val value = parts[3].toFloat()

                            controllerStates[ControllerKey(
                                    device,
                                    control
                            )]!!.setValueFromRemote(value)
                //                        Log.v(
                //                                "ControlSurface",
                //                                "set $device $control ${
                //                                    controllerStates[ControllerKey(
                //                                            device,
                //                                            control
                //                                    )]!!.name.value
                //                                } to $value"
                //                        )
                        }
                        "display" -> {
                            val value = parts[3]
                            Log.v(
                                    "ControlSurface",
                                    "set $device $control ${
                                        controllerStates[ControllerKey(
                                                device,
                                                control
                                        )]!!.name.value
                                    } to display $value"
                            )
                            controllerStates[ControllerKey(
                                    device,
                                    control
                            )]!!.remoteDisplayValue.value = value
                        }
                    }
                }
            }
        }
    }
}
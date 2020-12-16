package com.djcrontab.code.controlsurface

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

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
        val sendToBitwig = Channel<String>();
        val receiveFromBitwig = Channel<String>();

        for (device in 0..8) {
            deviceStates.add(DeviceState(device))
            for (control in 0..8) {
                val controlState = ControllerState(device, control, sendToBitWig = sendToBitwig)
                controllerStates[ControllerKey(device, control)] = controlState
            }
        }

        val controllerStates = this.controllerStates

        CoroutineScope(Dispatchers.IO).launch {
            BitwigConnection(sendToBitwig, receiveFromBitwig, "192.168.2.102", 60123)

            for (message in receiveFromBitwig) {
                val parts = message.split(",")
                val device = parts[0].toInt()
                CoroutineScope(Dispatchers.Main).launch main@{
                    if (parts[1] == "devicename") {
                        deviceStates[device].remoteNameChanged(parts[2])
                        return@main
                    }

                    val control = parts[1].toInt()
                    val action = parts[2]


                    if (action == "name") {
                        controllerStates[ControllerKey(device, control)]!!.remoteNameChanged(parts[3])
                    } else if (action == "value") {
                        val value = parts[3].toFloat()
                        Log.v(
                                "ControlSurface",
                                "set $device $control ${
                                    controllerStates[ControllerKey(
                                            device,
                                            control
                                    )]!!.name.value
                                } to $value"
                        )

                        controllerStates[ControllerKey(device, control)]!!.setValueFromRemote(value)
                    } else if (action == "display") {
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
                        controllerStates[ControllerKey(device, control)]!!.remoteDisplayValueChanged(value)
                    }
                }
            }
        }
    }
}
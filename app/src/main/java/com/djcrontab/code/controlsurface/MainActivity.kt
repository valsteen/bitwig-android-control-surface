package com.djcrontab.code.controlsurface

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.setContent
import com.djcrontab.code.common.MainWindow
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.math.truncate

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val controllerStates = ControllerStates()
        val sendToBitwig = Channel<String>();
        val receiveFromBitwig = Channel<String>();

        for (device in 0..8) {
            for (control in 0..8) {
                val mutableValue = object : MutableState<Float> {
                    override fun component1(): Float {
                        return value
                    }

                    override fun component2(): (Float) -> Unit {
                        return { x -> value = x }
                    }

                    override var value: Float
                        get() {
                            return backingValue
                        }
                        set(value) {
                            val newValue = truncate(value * 1000f) / 1000f
                            if (newValue != backingValue) {
                                backingValue = newValue

                                CoroutineScope(Dispatchers.IO).launch {
                                    sendToBitwig.send("$device $control $backingValue")
                                }
                            }
                        }

                    var backingValue : Float = 0f
                }

                controllerStates[ControlKey(device, control)] = ControlState(
                        mutableStateOf(control.toString()),
                        mutableValue)
            }
        }

        runBlocking(Dispatchers.IO) {
            BitwigConnection(sendToBitwig, receiveFromBitwig, "192.168.2.102", 60123)
        }

        super.onCreate(savedInstanceState)

        setContent {
            MainWindow(controllerStates).MainContent()
        }
    }
}

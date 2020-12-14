package com.djcrontab.code.controlsurface

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.setContent
import com.djcrontab.code.common.MainContent


class MainActivity : AppCompatActivity() {
    private val controllerStatesViewModel = ControllerStatesViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainContent(controllerStatesViewModel)
        }
    }
}

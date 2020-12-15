package com.djcrontab.code.controlsurface

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.setContent


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val controllerStatesViewModel: ControllerStatesViewModel by viewModels()

        super.onCreate(savedInstanceState)

        setContent {
            MainContent(controllerStatesViewModel)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

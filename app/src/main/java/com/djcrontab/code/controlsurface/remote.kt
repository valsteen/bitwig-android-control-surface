package com.djcrontab.code.controlsurface

import androidx.compose.runtime.MutableState

data class ControlKey(val device: Int, val control: Int)

class ControlState(var name: MutableState<String>, var value: MutableState<Float>)

class ControllerStates : HashMap<ControlKey, ControlState>()
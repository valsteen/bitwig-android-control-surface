package com.djcrontab.code.controlsurface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.gesture.doubleTapGestureFilter
import androidx.compose.ui.gesture.dragGestureFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.WithConstraints
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun Encoder(
        controlState: ControllerState
) {

    val name by controlState.name.observeAsState()
    val value by controlState.parameterValue.observeAsState()
    val displayValue by controlState.displayValue.observeAsState()
    val encoderColor = colorResource(id = R.color.encoder)

    Column {
        Box(Modifier.weight(.6f).fillMaxSize()) {
            WithConstraints(Modifier.fillMaxSize()) {
                val boxWidth = constraints.maxWidth
                val boxHeight = constraints.maxHeight

                Canvas(
                        Modifier
                                .doubleTapGestureFilter { controlState.focus() }
                                .dragGestureFilter(
                                        dragObserver = object : DragObserver {
                                            override fun onDrag(dragDistance: Offset): Offset {

                                                val relX = dragDistance.x / boxWidth.toFloat() / 4f
                                                val relY = -dragDistance.y / boxHeight.toFloat() / 4f

                                                controlState.onValueChanged(((value
                                                        ?: 0f) + relX + relY).coerceIn(0f, 1f))

                                                return dragDistance
                                            }

                                            override fun onStart(downPosition: Offset) {
                                                controlState.pauseRemoteUpdates = true
                                            }

                                            override fun onStop(velocity: Offset) {
                                                controlState.pauseRemoteUpdates
                                            }
                                        }
                                )
                ) {

                    val radius = size.width.coerceAtMost(size.height) * 0.9f / 2f
                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val calculatePhase = { value: Float, base: Float ->
                        (((0.90 * value) * base) - base / 5f + base / 2f)
                                .toFloat()
                    }
                    val phaseZero = calculatePhase(0f, 360f)
                    val valuePhase = calculatePhase(value ?: 0f, 360f)
                    val valuePhaseRadians = calculatePhase(value ?: 0f, (PI * 2f).toFloat())


                    drawCircle(
                            center = center,
                            color = encoderColor,
                            radius = radius,
                            style = Stroke(1.dp.toPx())
                    )

                    translate(topLeft.x, topLeft.y) {
                        drawArc(
                                color = encoderColor,
                                startAngle = phaseZero,
                                sweepAngle = valuePhase - phaseZero,
                                topLeft = Offset.Zero,
                                useCenter = false,
                                size = Size(radius * 2f, radius * 2f),
                                style = Stroke(width = 4.dp.toPx()),
                        )
                        translate(radius, radius) {
                            drawLine(
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Square,
                                    color = encoderColor,
                                    start = Offset.Zero,
                                    end = Offset(
                                            (cos(valuePhaseRadians) * radius),
                                            (sin(valuePhaseRadians) * radius)
                                    )
                            )
                        }
                    }

                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(.3f).height(with(AmbientDensity.current) { 32.sp.toDp() })) {
            Column() {
                Text(modifier = Modifier.weight(1f).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = name ?: "",
                        color = colorResource(id = R.color.encodertext),
                        fontSize = 14.sp,
                        lineHeight = 14.1.sp
                )

                Text(modifier = Modifier.weight(1f).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = displayValue ?: "",
                        color = colorResource(id = R.color.encodertext),
                        fontSize = 14.sp,
                        lineHeight = 14.1.sp
                )
            }
        }
    }
}


@Composable
fun MainContent(controllerStates: ControllerStatesViewModel) {
    MaterialTheme {
        Column {
            for (y in 0..3) {
                Box(Modifier.border(BorderStroke(1.dp, colorResource(id = R.color.border))).background(Color.Black).weight(1f)) {
                    Row {
                        for (x in 0..1) {
                            Box(Modifier.border(BorderStroke(1.dp, colorResource(id = R.color.border))).background(Color.Black).weight(1f)) {
                                Device(controllerStates, y * 2 + x)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun Device(controllerStates: ControllerStatesViewModel, device: Int) {

    Column() {
        Box(Modifier.weight(0.1f).fillMaxWidth().padding(2.dp)) {
            val deviceName by controllerStates.getDevice(device).name.observeAsState()
            Text(
                    deviceName ?: "",
                    color = colorResource(id = R.color.encodertext),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
            )
        }
        Box(Modifier.weight(0.9f)) {
            Column {
                for (y in 0..1) {
                    Box(Modifier.weight(1f)) {
                        Row {
                            for (x in 0..3) {
                                Box(Modifier.weight(1f)) {
                                    Encoder(controllerStates.get(device, y * 4 + x))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.djcrontab.code.controlsurface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.gesture.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
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
    controllerState: ControllerState,
    content: @Composable ColumnScope.() -> Unit
) {
    val name by controllerState.name.collectAsState("")
    val displayValue by controllerState.displayValue.collectAsState("")
    val touched by controllerState.touched.observeAsState(false)

    Column(Modifier.background(if (touched) colorResource(id = R.color.encodertouched) else Color.Black)) {

        Text(
            modifier = Modifier.weight(2.5f).fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = name,
            color = colorResource(id = R.color.encodertext),
            fontSize = 14.sp,
        )

        Text(
            modifier = Modifier.weight(3f).fillMaxWidth().padding(bottom = 4.dp),
            textAlign = TextAlign.Center,
            text = displayValue,
            color = colorResource(id = R.color.encodertext),
            fontSize = 14.sp
        )

        content()

        Spacer(modifier = Modifier.preferredHeight(3.dp).weight(1.9f))
    }
}


@Composable
fun Odometer(modifier: Modifier = Modifier, controllerState: ControllerState) {
    val value by controllerState.parameterValue.collectAsState(0f)
    val encoderColor = colorResource(id = R.color.encoder)
    val encoderColorFill = colorResource(id = R.color.encoderfill)
    val encoderColorFillOff = colorResource(id = R.color.encoderoff)
    val textHeight = with(AmbientDensity.current) { 22.sp.toDp() }

    Box(modifier) {
        WithConstraints(Modifier.fillMaxSize()) {
            val boxWidth = constraints.maxWidth
            Canvas(
                Modifier
                    .pressIndicatorGestureFilter(onStart = {
                        controllerState.touched.value = true
                    }, onStop = {
                        controllerState.touched.value = false
                    })
                    .doubleTapGestureFilter {
                        controllerState.focus()
                    }
                    .dragGestureFilter(
                        dragObserver = object : DragObserver {
                            override fun onDrag(dragDistance: Offset): Offset {

                                val relX = dragDistance.x / boxWidth.toFloat() / 4f
                                val relY = -dragDistance.y / boxWidth.toFloat() / 4f

                                controllerState.onValueChanged(
                                    (value + relX + relY).coerceIn(0f, 1f)
                                )

                                return dragDistance
                            }

                            override fun onStart(downPosition: Offset) {
                                controllerState.touched.value = true
                            }

                            override fun onStop(velocity: Offset) {
                                controllerState.touched.value = false
                            }
                        }
                    )

            ) {

                val radius = size.width * 0.9f / 2f
                val topLeft = Offset(center.x - radius, center.y - radius)
                val calculatePhase = { value: Float, base: Float ->
                    value * base / 2f - base / 2f
                }
                val phaseZero = calculatePhase(0f, 360f)
                val valuePhase = calculatePhase(value, 360f)
                val valuePhaseRadians = calculatePhase(value, (PI * 2f).toFloat())

                translate(topLeft.x, topLeft.y + textHeight.toPx()) {
                    drawArc(
                        color = encoderColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        topLeft = Offset.Zero,
                        useCenter = false,
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(1.dp.toPx()),
                    )

                    // half circle
                    drawArc(
                        color = encoderColorFillOff,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        topLeft = Offset.Zero,
                        useCenter = false,
                        size = Size(radius * 2f, radius * 2f),
                        style = Fill
                    )

                    // arc filler
                    drawArc(
                        color = encoderColorFill,
                        startAngle = phaseZero,
                        sweepAngle = valuePhase - phaseZero,
                        topLeft = Offset.Zero,
                        useCenter = false,
                        size = Size(radius * 2f, radius * 2f),
                    )

                    // value outline
                    drawArc(
                        color = encoderColor,
                        startAngle = phaseZero,
                        sweepAngle = valuePhase - phaseZero,
                        topLeft = Offset.Zero,
                        useCenter = false,
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 2.dp.toPx()),
                    )

                    // triangle to fill from arc filler to center
                    drawPath(Path().apply {
                        moveTo(0f, radius)
                        lineTo(radius, radius)
                        lineTo(
                            radius + cos(valuePhaseRadians) * radius,
                            sin(valuePhaseRadians) * radius + radius
                        )
                        close()
                    }, encoderColorFill)

                    drawLine(
                        strokeWidth = 2.dp.toPx(),
                        start = Offset(0f, radius),
                        end = Offset(radius, radius),
                        color = encoderColor,
                    )

                    translate(radius, radius) {
                        drawLine(
                            strokeWidth = 2.dp.toPx(),
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
}


@Composable
fun MainContent(controllerStates: ControllerStatesViewModel) {
    MaterialTheme {
        Column {
            for (deviceRow in 0 until DEVICES/2) {
                Box(
                    Modifier.background(Color.Black).weight(1f)
                ) {
                    Row {
                        for (deviceColumn in 0..1) {
                            val deviceState = controllerStates.getDevice(deviceRow * 2 + deviceColumn)
                            Box(
                                Modifier.border(
                                    BorderStroke(
                                        1.dp,
                                        colorResource(id = R.color.border)
                                    )
                                ).background(Color.Black).weight(1f)
                            ) {
                                Device(deviceState.name.collectAsState("")) {
                                    Column {
                                        for (encoderColumn in 0..1) {
                                            Box(Modifier.weight(1f)) {
                                                Row {
                                                    for (encoderRow in 0..3) {
                                                        val controllerState = controllerStates.get(deviceRow * 2 + deviceColumn, encoderColumn * 4 + encoderRow)
                                                        Box(Modifier.weight(1f)) {
                                                            Encoder(controllerState) {
                                                                Odometer(Modifier.weight(6f).fillMaxSize(), controllerState)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun Device(name: State<String>, content: @Composable BoxScope.() -> Unit) {
    Column() {
        Box(Modifier.weight(0.1f).fillMaxWidth().padding(top=4.dp, bottom=4.dp).height(with(AmbientDensity.current) { 26.sp.toDp() })) {
            val deviceName by name
            Text(
                deviceName,
                color = colorResource(id = R.color.encodertext),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(Modifier.weight(0.9f)) {
            content()
        }
    }
}

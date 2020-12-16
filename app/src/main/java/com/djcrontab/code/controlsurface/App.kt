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
    controlState: ControllerState
) {

    val name by controlState.name.observeAsState()
    val value by controlState.parameterValue.observeAsState()
    val displayValue by controlState.displayValue.observeAsState()
    val encoderColor = colorResource(id = R.color.encoder)
    val encoderColorFill = colorResource(id = R.color.encoderfill)
    val encoderColorFillOff = colorResource(id = R.color.encoderoff)

    Column {
        val textHeight = with(AmbientDensity.current) { 22.sp.toDp() }

        Spacer(modifier = Modifier.preferredHeight(2.dp).weight(1f))

        Box(Modifier.weight(2f).fillMaxWidth().height(textHeight).padding(top=3.dp)) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = name ?: "",
                color = colorResource(id = R.color.encodertext),
                fontSize = 14.sp,
            )
        }
        Spacer(modifier = Modifier.preferredHeight(3.dp).weight(1.9f))

        Box(Modifier.weight(6f).fillMaxSize()) {

            WithConstraints(Modifier.fillMaxSize()) {
                val boxWidth = constraints.maxWidth
                Canvas(
                    Modifier
                        .doubleTapGestureFilter { controlState.focus() }
                        .dragGestureFilter(
                            dragObserver = object : DragObserver {
                                override fun onDrag(dragDistance: Offset): Offset {

                                    val relX = dragDistance.x / boxWidth.toFloat() / 4f
                                    val relY = -dragDistance.y / boxWidth.toFloat() / 4f

                                    controlState.onValueChanged(
                                        ((value
                                            ?: 0f) + relX + relY).coerceIn(0f, 1f)
                                    )

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

                    val radius = size.width * 0.9f / 2f
                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val calculatePhase = { value: Float, base: Float ->
                        value * base / 2f - base / 2f
                    }
                    val phaseZero = calculatePhase(0f, 360f)
                    val valuePhase = calculatePhase(value ?: 0f, 360f)
                    val valuePhaseRadians = calculatePhase(value ?: 0f, (PI * 2f).toFloat())

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

        Spacer(modifier = Modifier.preferredHeight(4.dp).weight(2f))

        Text(
            modifier = Modifier.weight(1.5f).fillMaxWidth().height(textHeight).padding(bottom=2.dp),
            textAlign = TextAlign.Center,
            text = displayValue ?: "",
            color = colorResource(id = R.color.encodertext),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.preferredHeight(2.dp).weight(1.5f))
    }
}


@Composable
fun MainContent(controllerStates: ControllerStatesViewModel) {
    MaterialTheme {
        Column {
            for (y in 0 until DEVICES/2) {
                Box(
                    Modifier.border(BorderStroke(1.dp, colorResource(id = R.color.border)))
                        .background(Color.Black).weight(1f)
                ) {
                    Row {
                        for (x in 0..1) {
                            Box(
                                Modifier.border(
                                    BorderStroke(
                                        1.dp,
                                        colorResource(id = R.color.border)
                                    )
                                ).background(Color.Black).weight(1f)
                            ) {
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
        Box(Modifier.weight(0.1f).fillMaxWidth().padding(top=4.dp, bottom=4.dp).height(with(AmbientDensity.current) { 26.sp.toDp() })) {
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

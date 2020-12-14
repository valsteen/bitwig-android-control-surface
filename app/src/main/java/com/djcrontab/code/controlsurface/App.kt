package com.djcrontab.code.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.WithConstraints
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.unit.*
import com.djcrontab.code.controlsurface.ControllerState
import com.djcrontab.code.controlsurface.ControllerStatesViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.gesture.dragGestureFilter

@Composable
fun Encoder(
    controlState: ControllerState,
    modifier: Modifier = Modifier,
    ) {
    var boxSize by remember { mutableStateOf(IntSize(0, 0)) }
    val name by controlState.name.observeAsState()
    val value by controlState.value.observeAsState()

    Box(modifier) {
        Canvas(
            modifier
                .onSizeChanged {
                    if (it.width != 0 && it.height != 0) {
                        boxSize = IntSize(it.width, it.height)
                    }
                }
                .dragGestureFilter(
                    dragObserver = object : DragObserver {
                        override fun onDrag(dragDistance: Offset): Offset {
                            if (boxSize.width == 0 || boxSize.height == 0) {
                                return dragDistance
                            }
                            val relX = dragDistance.x / boxSize.width.toFloat() / 4f
                            val relY = -dragDistance.y / boxSize.height.toFloat() / 4f

                            controlState.onValueChanged(((value ?: 0f) + relX + relY).coerceIn(0f, 1f))

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
            drawCircle(
                center = center,
                color = Color.Black,
                radius = radius,
                style = Stroke(1.dp.toPx())

            )

            val calculatePhase = { value: Float, base: Float ->
                (((0.90 * value) * base) - base / 5f + base / 2f)
                    .toFloat()
            }
            val phaseZero = calculatePhase(0f, 360f)
            val valuePhase = calculatePhase(value ?: 0f, 360f)
            val valuePhaseRadians = calculatePhase(value ?: 0f, (PI * 2f).toFloat())

            translate(topLeft.x, topLeft.y) {
                drawArc(
                    color = Color.Black,
                    startAngle = phaseZero,
                    sweepAngle = valuePhase - phaseZero,
                    topLeft = Offset.Zero,
                    useCenter = false,
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 8.dp.toPx()),
                )
                translate(radius, radius) {
                    drawLine(
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Square,
                        color = Color.Black,
                        start = Offset.Zero,
                        end = Offset(
                            (cos(valuePhaseRadians) * radius),
                            (sin(valuePhaseRadians) * radius)
                        )
                    )
                }
            }
        }

        Text(
            name ?: "",
            Modifier.align(Alignment.BottomCenter),
            Color(0, 0, 100),
            15.sp
        )
    }
}


@Composable
fun MainContent(controllerStates: ControllerStatesViewModel) {
    MaterialTheme {
        WithConstraints(modifier = Modifier.fillMaxSize()) {
            val deviceWidth = with(AmbientDensity.current) { constraints.maxWidth / 4 }
            val deviceHeight = with(AmbientDensity.current) { constraints.maxHeight / 8 }
            for (y in 0..3) {
                for (x in 0..1) {
                    Box(
                        Modifier.offset(x = deviceWidth.dp * x, y = deviceHeight.dp * y)
                    ) {
                        Device(controllerStates, y*2+x)
                    }
                }
            }
        }
    }
}


@Composable
fun Device(controllerStates: ControllerStatesViewModel, device: Int) {
    WithConstraints(modifier = Modifier.fillMaxSize()) {
        val boxWidth = with(AmbientDensity.current) { constraints.maxWidth / 16 }
        val boxHeight = with(AmbientDensity.current) { constraints.maxHeight / 16 }
        for (y in 0..1) {
            for (x in 0..3) {
                Box(
                    Modifier.offset(x = boxWidth.dp * x, y = boxHeight.dp * y)
                ) {
                    Encoder(
                        controllerStates.get(device, y*4+x),
                        Modifier.size(boxWidth.dp, boxHeight.dp)
                    )
                }
            }
        }
    }
}
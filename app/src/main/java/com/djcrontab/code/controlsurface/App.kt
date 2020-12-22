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
import androidx.compose.ui.Alignment
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
import kotlin.reflect.KFunction0


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
    var page by mutableStateOf(0)

    fun nextPage() {
        page = (page + 1) % PAGES
    }

    fun previousPage() {
        page -= 1
        if (page < 0) page += PAGES
    }

    MaterialTheme() {
        Column(Modifier.background(Color.Black)) {
            Row(Modifier.weight(0.2f).fillMaxWidth()) {
                Box(Modifier.fillMaxHeight().weight(0.2f).pressIndicatorGestureFilter({
                    previousPage()
                }))
                Box(Modifier.fillMaxHeight().weight(0.2f).pressIndicatorGestureFilter({
                    nextPage()
                }))
            }

            for (deviceRow in 0 until DEVICES_PER_PAGE / 2) {
                Box(
                    Modifier.weight(1f)
                ) {
                    Row {
                        for (deviceColumn in 0..1) {
                            val deviceState =
                                controllerStates.getDevice(deviceRow * 2 + deviceColumn + page * DEVICES_PER_PAGE)
                            Box(
                                Modifier.border(
                                    BorderStroke(
                                        1.dp,
                                        colorResource(id = R.color.border)
                                    )
                                ).background(Color.Black).weight(1f)
                            ) {
                                Device(
                                    deviceState.name.collectAsState(""),
                                    nextPage = deviceState::nextPage,
                                    previousPage = deviceState::previousPage,
                                    pin = deviceState::pin,
                                    color = deviceState.color.collectAsState(Color(0,0,0)),
                                    isPlaying = deviceState.playing.collectAsState(false)
                                ) {
                                    Column {
                                        for (encoderColumn in 0..1) {
                                            Box(Modifier.weight(1f)) {
                                                Row {
                                                    for (encoderRow in 0..3) {
                                                        val controllerState = controllerStates.get(
                                                            deviceRow * 2 + deviceColumn + page * DEVICES_PER_PAGE,
                                                            encoderColumn * 4 + encoderRow
                                                        )
                                                        Box(Modifier.weight(1f)) {
                                                            Encoder(controllerState) {
                                                                Odometer(
                                                                    Modifier.weight(6f)
                                                                        .fillMaxSize(),
                                                                    controllerState
                                                                )
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
            Row(Modifier.weight(0.2f).fillMaxWidth()) {
                Box(Modifier.fillMaxHeight().weight(0.2f).pressIndicatorGestureFilter({
                    previousPage()
                }))
                Box(Modifier.fillMaxHeight().weight(0.2f).pressIndicatorGestureFilter({
                    nextPage()
                }))
            }
        }
    }   
}


@Composable
fun Device(
    name: State<String>,
    nextPage: KFunction0<Unit>,
    previousPage: KFunction0<Unit>,
    pin: KFunction0<Unit>,
    color: State<Color>,
    isPlaying: State<Boolean>,
    content: @Composable() (BoxScope.() -> Unit),
) {
    WithConstraints(Modifier.fillMaxSize()) {
        val center = constraints.maxWidth / 2
        var pressIndicatorDirectionIsNext = false

        Column() {
            Box(
                Modifier.weight(0.1f).fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)
                    .height(with(AmbientDensity.current) { 26.sp.toDp() }).dragGestureFilter(
                        dragObserver = object : DragObserver {
                            override fun onStop(velocity: Offset) {
                                if (velocity.x > 0) {
                                    previousPage()
                                } else if (velocity.x < 0) {
                                    nextPage()
                                }
                                super.onStop(velocity)
                            }
                        }
                    ).longPressGestureFilter {
                        pin()
                    }.pressIndicatorGestureFilter(onStart = {
                        pressIndicatorDirectionIsNext = it.x < center
                    }, onStop = {
                        if (pressIndicatorDirectionIsNext) {
                            nextPage()
                        } else {
                            previousPage()
                        }
                    }
                    )
            ) {
                val deviceName by name
                val deviceColor by color
                val playing by isPlaying

                Box(
                    Modifier.padding(start = 10.dp).height(5.dp).width(1.dp)
                        .background(color = deviceColor).align(Alignment.CenterStart)
                )
                Box(
                    Modifier.padding(end = 10.dp).height(5.dp).width(1.dp)
                        .align(Alignment.CenterEnd)
                        .background(color = if (playing) colorResource(id = R.color.encodertext) else Color.Black)
                )

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
}

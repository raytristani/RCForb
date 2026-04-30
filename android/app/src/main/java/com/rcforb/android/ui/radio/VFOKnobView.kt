package com.rcforb.android.ui.radio

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rcforb.android.R
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun VFOKnobView(
    size: Int,
    vfo: String,
    step: Int,
    frequency: Int,
    onFrequencyChange: (Int) -> Unit
) {
    var rotation by remember { mutableFloatStateOf(0f) }
    val sizeDp = size.dp
    val center = size / 2f

    // Use refs so the pointer input block doesn't restart on every frequency change
    val freqRef = rememberUpdatedState(frequency)
    val stepRef = rememberUpdatedState(step)
    val callbackRef = rememberUpdatedState(onFrequencyChange)

    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var lastAngle = Math.toDegrees(
                        atan2(
                            (down.position.y - center).toDouble(),
                            (down.position.x - center).toDouble()
                        )
                    ).toFloat()

                    // Accumulate frequency locally during this gesture
                    var dragFreq = freqRef.value
                    var lastSentFreq = dragFreq

                    do {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull() ?: break

                        if (pointer.changedToUp()) {
                            break
                        }

                        val angle = Math.toDegrees(
                            atan2(
                                (pointer.position.y - center).toDouble(),
                                (pointer.position.x - center).toDouble()
                            )
                        ).toFloat()

                        var delta = angle - lastAngle
                        if (delta > 180) delta -= 360
                        if (delta < -180) delta += 360

                        rotation += delta

                        val steps = (delta / 25).roundToInt()
                        if (steps != 0) {
                            dragFreq += steps * stepRef.value
                            if (dragFreq > 0 && dragFreq != lastSentFreq) {
                                lastSentFreq = dragFreq
                                callbackRef.value(dragFreq)
                            }
                        }

                        lastAngle = angle
                        pointer.consume()
                    } while (true)
                }
            }
    ) {
        Image(
            painter = painterResource(R.drawable.knob_xlarge),
            contentDescription = "VFO $vfo Knob",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
        )
    }
}

package cc.tomko.outify.ui.screens.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.tomko.outify.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MIN_SPEED = 0.5f
private const val MAX_SPEED = 3f
private const val SPEED_STEP = 0.05f

/** Snaps to the nearest 0.05 so values don't carry float noise; sticks to 1x when close. */
private fun Float.snapSpeed(): Float {
    val snapped = ((this / SPEED_STEP).roundToInt() * SPEED_STEP).coerceIn(MIN_SPEED, MAX_SPEED)
    return if (abs(snapped - 1f) < 0.03f) 1f else snapped
}

private fun Float.speedLabel(): String =
    String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.') + "x"

/**
 * Always-visible playback speed slider (0.5x - 3x).
 * Changes apply live while dragging; tap the speed label to reset to 1x.
 */
@Composable
fun PlaybackSpeedControl(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderValue by remember { mutableFloatStateOf(currentSpeed.coerceIn(MIN_SPEED, MAX_SPEED)) }
    var dragging by remember { mutableStateOf(false) }

    // Follow external speed changes (e.g. from a media controller) when not dragging.
    LaunchedEffect(currentSpeed) {
        if (!dragging) sliderValue = currentSpeed.coerceIn(MIN_SPEED, MAX_SPEED)
    }

    val shownSpeed = sliderValue.snapSpeed()
    val isNormal = shownSpeed == 1f
    val playbackSpeedLabel = stringResource(R.string.player_playback_speed)

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 6.dp, end = 14.dp),
        ) {
            Surface(
                onClick = {
                    sliderValue = 1f
                    onSpeedChange(1f)
                },
                shape = RoundedCornerShape(50),
                color = if (isNormal) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier
                    .height(32.dp)
                    .width(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = shownSpeed.speedLabel(),
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = if (isNormal) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                    )
                }
            }

            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    dragging = true
                    val previous = sliderValue.snapSpeed()
                    sliderValue = value
                    val snapped = value.snapSpeed()
                    if (snapped != previous) onSpeedChange(snapped)
                },
                onValueChangeFinished = {
                    dragging = false
                    val snapped = sliderValue.snapSpeed()
                    sliderValue = snapped
                    onSpeedChange(snapped)
                },
                valueRange = MIN_SPEED..MAX_SPEED,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .semantics { contentDescription = playbackSpeedLabel },
            )
        }
    }
}

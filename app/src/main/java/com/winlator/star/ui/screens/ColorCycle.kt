package com.winlator.star.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Continuously animated colors that drift through several shades of one hue family —
 * used everywhere a button or icon would otherwise be a flat blue or red. No purple,
 * no glow/shadow effects — just the color itself shifting over time.
 *
 * The first shade is repeated at the end of each list so the loop point is seamless
 * (the animation restarts at a color identical to where it just was).
 */

private val BLUE_SHADES = listOf(
    Color(0xFF00E5FF), // cyan
    Color(0xFF2979FF), // bright blue
    Color(0xFF0072CE), // Winlator blue
    Color(0xFF00B8D4), // teal-blue
    Color(0xFF00E5FF), // back to cyan — seamless loop
)

private val RED_SHADES = listOf(
    Color(0xFFFF1744), // vivid red
    Color(0xFFE60012), // AnTuTu red
    Color(0xFFFF5252), // lighter red
    Color(0xFFD50000), // deep red
    Color(0xFFFF1744), // back to vivid red — seamless loop
)

/** A blue that continuously cycles through several shades of blue. Never purple. */
@Composable
fun cyclingBlue(periodMillis: Int = 900): Color = rememberCyclingColor(BLUE_SHADES, periodMillis)

/** A red that continuously cycles through several shades of red. Never purple. */
@Composable
fun cyclingRed(periodMillis: Int = 900): Color = rememberCyclingColor(RED_SHADES, periodMillis)

@Composable
private fun rememberCyclingColor(shades: List<Color>, periodMillisPerStep: Int): Color {
    val steps = shades.size - 1
    val transition = rememberInfiniteTransition(label = "colorCycle")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = steps.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillisPerStep * steps, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "colorCycleT",
    )
    val index = t.toInt().coerceIn(0, steps - 1)
    val fraction = (t - index).coerceIn(0f, 1f)
    return lerp(shades[index], shades[index + 1], fraction)
}

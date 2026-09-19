package com.winlator.star.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winlator.star.R

@Composable
fun SplashScreen(
    progress: Int,
    showProceed: Boolean = false,
    onProceed: () -> Unit = {},
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val displayedProgress by animateIntAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "counter",
    )

    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.05f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logoScale",
    )

    val shimmerPos by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue  = 1.3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp),
        ) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .size(140.dp)
                        .scale(logoScale),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Winlator AnTuTu",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(Modifier.height(32.dp))

            GlowingProgressBar(
                progress    = displayedProgress / 100f,
                shimmerPos  = shimmerPos,
                isComplete  = progress >= 100,
                modifier    = Modifier.fillMaxWidth().height(16.dp),
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "$displayedProgress%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFAAAAAA),
            )

            AnimatedVisibility(
                visible = showProceed,
                enter   = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.92f),
            ) {
                Column {
                    Spacer(Modifier.height(32.dp))
                    GradientButton(
                        text = "Proceed",
                        onClick = onProceed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * The physically-correct blue/red mixing ramp, used by both the button and the progress
 * bar: ten shades of blue (deep -> bright) darkening into the middle, ten shades of red
 * (bright -> deep) picking up on the way out, and — instead of ever crossing through
 * purple/violet — the middle third blends each side toward white, the way overlapping
 * blue and red LIGHT actually mixes (additive mixing), not the way blue and red PAINT
 * mixes (subtractive, which is what gives you purple). That's the "real colors mixing"
 * look: light, airy, near-white in the center, not a magenta smear.
 */
private val BLUE_RAMP = listOf(
    Color(0xFF001B44), // deep navy
    Color(0xFF00297A),
    Color(0xFF0039A6),
    Color(0xFF0050C8),
    Color(0xFF0072CE), // Winlator blue
    Color(0xFF1E90E8),
    Color(0xFF42A9F5),
    Color(0xFF6FC2FF),
    Color(0xFF9DD6FF),
    Color(0xFFCDEBFF), // pale, almost-white blue
)

private val RED_RAMP = listOf(
    Color(0xFFFFD9D2), // pale, almost-white red
    Color(0xFFFFB3A1),
    Color(0xFFFF8A70),
    Color(0xFFFF6347),
    Color(0xFFFF3B1F),
    Color(0xFFE8280F),
    Color(0xFFD01B0A),
    Color(0xFFB01206),
    Color(0xFF8C0C03),
    Color(0xFF6B0801), // deep, near-black red
)

private val WHITE_HOT = Color(0xFFFFFFFF)

/**
 * Samples the full ramp at [t] in 0f..1f. First third runs through BLUE_RAMP (dark to
 * light), middle third blends the lightest blue up through near-white and back down into
 * the lightest red (additive-mixing look, no purple), final third runs through RED_RAMP
 * (light to dark).
 */
private fun mixedRampColor(t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return when {
        clamped < 0.4f -> {
            val local = clamped / 0.4f * (BLUE_RAMP.size - 1)
            val i = local.toInt().coerceIn(0, BLUE_RAMP.size - 2)
            lerp(BLUE_RAMP[i], BLUE_RAMP[i + 1], local - i)
        }
        clamped < 0.6f -> {
            // The "mixing" zone: lightest blue -> white -> lightest red. This is what
            // stands in for two colors of light overlapping instead of two paints.
            val local = (clamped - 0.4f) / 0.2f
            if (local < 0.5f) {
                lerp(BLUE_RAMP.last(), WHITE_HOT, local / 0.5f)
            } else {
                lerp(WHITE_HOT, RED_RAMP.first(), (local - 0.5f) / 0.5f)
            }
        }
        else -> {
            val local = (clamped - 0.6f) / 0.4f * (RED_RAMP.size - 1)
            val i = local.toInt().coerceIn(0, RED_RAMP.size - 2)
            lerp(RED_RAMP[i], RED_RAMP[i + 1], local - i)
        }
    }
}

/**
 * A button that continuously cycles through the same blue -> white-mix -> red ramp as
 * the progress bar, instead of a flat theme color or a static gradient.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradientButton")
    val cyclePos by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gradientButtonCycle",
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.horizontalGradient(
                    // Sample a moving 5-stop window of the ramp so the whole gradient
                    // slowly drifts through blue -> mix -> red over time.
                    colors = List(5) { i ->
                        mixedRampColor((cyclePos + i * 0.08f) % 1f)
                    }
                ),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Not private: reused by FileManagerScreen.kt for its own copy/extract/compress
 * progress overlay, so both screens share one progress bar implementation.
 *
 * Bigger and more polished than before: thicker track, a soft drop shadow under the
 * fill, a brighter core highlight running down the center of the fill, and an improved
 * two-layer shimmer (a broad soft pass plus a tight bright pass) instead of one flat
 * streak.
 */
@Composable
fun GlowingProgressBar(
    progress: Float,
    shimmerPos: Float,
    isComplete: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val barH   = size.height
        val barW   = size.width
        val radius = barH / 2f
        val fillW  = (barW * progress).coerceIn(0f, barW)

        // Track
        drawRoundRect(
            color        = Color(0xFF141416),
            size         = Size(barW, barH),
            cornerRadius = CornerRadius(radius),
        )
        // Subtle inner border so the track reads as a groove, not a flat rectangle.
        drawRoundRect(
            color        = Color(0xFF262629),
            size         = Size(barW, barH),
            cornerRadius = CornerRadius(radius),
            style        = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
        )

        if (fillW > 0f) {
            val stops = List(6) { i -> mixedRampColor(i / 5f) }

            // Soft drop shadow beneath the fill for depth.
            drawRoundRect(
                color        = Color.Black.copy(alpha = 0.35f),
                topLeft      = Offset(0f, barH * 0.18f),
                size         = Size(fillW, barH),
                cornerRadius = CornerRadius(radius),
            )

            // Outer glow, tinted to match whatever's at the leading edge of the fill.
            val edgeColor = mixedRampColor(progress)
            listOf(
                6f to 0.12f,
                3f to 0.22f,
            ).forEach { (expand, a) ->
                drawRoundRect(
                    color        = edgeColor.copy(alpha = a),
                    topLeft      = Offset(-expand / 2f, -expand / 2f),
                    size         = Size(fillW + expand, barH + expand),
                    cornerRadius = CornerRadius(radius + expand / 2f),
                )
            }

            // Main fill: the full blue -> white-mix -> red ramp, mapped across the
            // filled width so far (not the whole bar), so early progress reads as
            // deep blue and only reaches the red end once mostly complete.
            drawRoundRect(
                brush = Brush.horizontalGradient(colors = stops, endX = fillW),
                size         = Size(fillW, barH),
                cornerRadius = CornerRadius(radius),
            )

            // Bright core highlight through the vertical center of the fill, giving it
            // a rounded, glassy look rather than a flat color block.
            val coreH = barH * 0.4f
            drawRoundRect(
                brush = Brush.horizontalGradient(colors = stops, endX = fillW),
                topLeft      = Offset(0f, (barH - coreH) / 2f),
                size         = Size(fillW, coreH),
                cornerRadius = CornerRadius(coreH / 2f),
                alpha        = 0.5f,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft      = Offset(0f, barH * 0.12f),
                size         = Size(fillW, barH * 0.22f),
                cornerRadius = CornerRadius(barH * 0.11f),
            )

            // Shimmer: a broad soft pass plus a tighter bright pass riding on top of it,
            // instead of one flat streak.
            if (!isComplete) {
                val shimX = shimmerPos * fillW
                clipRect(right = fillW) {
                    val broadHalf = barH * 6f
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent,
                            ),
                            startX = shimX - broadHalf,
                            endX   = shimX + broadHalf,
                        ),
                        size         = Size(fillW, barH),
                        cornerRadius = CornerRadius(radius),
                    )
                    val tightHalf = barH * 1.5f
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.65f),
                                Color.Transparent,
                            ),
                            startX = shimX - tightHalf,
                            endX   = shimX + tightHalf,
                        ),
                        size         = Size(fillW, barH),
                        cornerRadius = CornerRadius(radius),
                    )
                }
            }
        }
    }
}

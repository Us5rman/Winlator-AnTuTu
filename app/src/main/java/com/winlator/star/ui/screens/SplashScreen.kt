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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
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
            // Increased container size to 220.dp and icon size to 200.dp
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
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
                modifier    = Modifier.fillMaxWidth().height(8.dp),
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
                    Button(
                        onClick = onProceed,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape   = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Proceed", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GlowingProgressBar(
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
            color        = Color(0xFF1E1E1E),
            size         = Size(barW, barH),
            cornerRadius = CornerRadius(radius),
        )

        if (fillW > 0f) {
            val cyanColor = Color(0xFF00E5FF)
            val blendedColor = Color(0xFF8000FF)
            val redColor = Color(0xFFFF0055)

            // Outer subtle glow layer matching the blue-to-red transition
            listOf(
                4f to 0.15f,
                2f to 0.30f,
            ).forEach { (expand, a) ->
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            cyanColor.copy(alpha = a),
                            blendedColor.copy(alpha = a),
                            redColor.copy(alpha = a)
                        ),
                        endX = fillW,
                    ),
                    topLeft      = Offset(-expand / 2f, -expand / 2f),
                    size         = Size(fillW + expand, barH + expand),
                    cornerRadius = CornerRadius(radius + expand / 2f),
                )
            }

            // Main gradient fill (Blue -> Blended Purple -> Red)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(cyanColor, blendedColor, redColor),
                    endX   = fillW,
                ),
                size         = Size(fillW, barH),
                cornerRadius = CornerRadius(radius),
            )

            // Shimmer effect while loading
            if (!isComplete) {
                val shimX    = shimmerPos * fillW
                val shimHalf = barH * 4f
                clipRect(right = fillW) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.4f),
                                Color.Transparent,
                            ),
                            startX = shimX - shimHalf,
                            endX   = shimX + shimHalf,
                        ),
                        size         = Size(fillW, barH),
                        cornerRadius = CornerRadius(radius),
                    )
                }
            }
        }
    }
}

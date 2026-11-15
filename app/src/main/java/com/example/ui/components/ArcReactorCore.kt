package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CoreGold
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorCore(
    size: Dp = 190.dp,
    isActivePulse: Boolean = false,
    isBoostMode: Boolean = false,
    useGoldAccent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "arc_reactor")
    val outerRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isBoostMode) 6000 else 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )
    val innerRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActivePulse) 4500 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )
    val pulseScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = if (isActivePulse) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryGlow = if (useGoldAccent) CoreGold else ArcCyan
    val secondaryGlow = if (useGoldAccent) ArcCyan else ArcBlue

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = this.size.minDimension
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxR = minDim / 2f

            // Ambient radial halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = if (isActivePulse) 0.30f else 0.16f),
                        secondaryGlow.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR * pulseScale
                ),
                radius = maxR,
                center = center
            )

            // Outer segmented tick ring
            rotate(outerRotation, pivot = center) {
                val tickOuter = maxR * 0.90f
                val tickInner = maxR * 0.80f
                for (i in 0 until 32) {
                    val angle = Math.toRadians((i * 11.25))
                    val start = Offset(
                        center.x + (tickInner * cos(angle)).toFloat(),
                        center.y + (tickInner * sin(angle)).toFloat()
                    )
                    val end = Offset(
                        center.x + (tickOuter * cos(angle)).toFloat(),
                        center.y + (tickOuter * sin(angle)).toFloat()
                    )
                    drawLine(
                        color = primaryGlow.copy(alpha = if (i % 4 == 0) 0.9f else 0.38f),
                        start = start,
                        end = end,
                        strokeWidth = if (i % 4 == 0) 3.5f else 2f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Counter-rotating tech arcs
            rotate(innerRotation, pivot = center) {
                val arcRadius = maxR * 0.70f
                val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
                for (startAngle in listOf(15f, 105f, 195f, 285f)) {
                    drawArc(
                        color = primaryGlow.copy(alpha = 0.85f),
                        startAngle = startAngle,
                        sweepAngle = 55f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                }
            }

            // Inner core circle
            val coreR = maxR * 0.50f * pulseScale
            drawCircle(
                color = ObsidianCard,
                radius = coreR,
                center = center
            )
            drawCircle(
                color = primaryGlow,
                radius = coreR,
                center = center,
                style = Stroke(width = 3.5f)
            )

            // Inverted Stark-style Arc Reactor Triangle
            val triRadius = coreR * 0.64f
            val p1 = Offset(center.x - triRadius * 0.86f, center.y - triRadius * 0.50f)
            val p2 = Offset(center.x + triRadius * 0.86f, center.y - triRadius * 0.50f)
            val p3 = Offset(center.x, center.y + triRadius * 0.98f)
            val triPath = Path().apply {
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }
            drawPath(
                path = triPath,
                color = ObsidianDeep
            )
            drawPath(
                path = triPath,
                color = primaryGlow,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = primaryGlow.copy(alpha = 0.75f),
                radius = coreR * 0.14f,
                center = Offset(center.x, center.y + triRadius * 0.05f)
            )
        }
    }
}

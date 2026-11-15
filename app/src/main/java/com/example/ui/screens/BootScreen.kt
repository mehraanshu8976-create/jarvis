package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.ArcReactorCore
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun BootScreen(
    onBootComplete: () -> Unit
) {
    var targetProgress by remember { mutableFloatStateOf(0.15f) }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 450),
        label = "boot_progress"
    )

    LaunchedEffect(Unit) {
        delay(250)
        targetProgress = 0.45f
        delay(350)
        targetProgress = 0.78f
        delay(400)
        targetProgress = 1.0f
        delay(450)
        onBootComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianDeep, ObsidianCard.copy(alpha = 0.55f), ObsidianDeep)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INITIALIZING J.A.R.V.I.S.",
                    style = MaterialTheme.typography.labelLarge,
                    color = ArcCyan
                )
                TextButton(
                    onClick = onBootComplete,
                    modifier = Modifier.testTag("boot_skip_button")
                ) {
                    Text("ENTER HUD", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ArcReactorCore(
                    size = 210.dp,
                    isActivePulse = true
                )
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "J.A.R.V.I.S.",
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "JUST A RATHER VERY INTELLIGENT SYSTEM",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "SYSTEM BOOTING... ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = ArcCyan
                )
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ArcCyan,
                    trackColor = ObsidianCard,
                    strokeCap = StrokeCap.Round
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BootModuleChip("CORE", Icons.Rounded.Memory, progress >= 0.2f)
                    BootModuleChip("MEMORY", Icons.Rounded.Storage, progress >= 0.4f)
                    BootModuleChip("NET", Icons.Rounded.Language, progress >= 0.6f)
                    BootModuleChip("AI VOICE", Icons.Rounded.GraphicEq, progress >= 0.8f)
                    BootModuleChip("SEC", Icons.Rounded.Security, progress >= 0.95f)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "PREPARING YOUR AI EXPERIENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun BootModuleChip(
    label: String,
    icon: ImageVector,
    online: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianCard.copy(alpha = 0.7f))
            .border(1.dp, if (online) ArcBlue.copy(alpha = 0.5f) else ObsidianBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (online) ArcCyan else TextMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary
        )
        Text(
            text = if (online) "ONLINE" else "LOAD",
            style = MaterialTheme.typography.labelSmall,
            color = if (online) EmeraldOnline else TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = if (online) ArcCyan else TextMuted.copy(alpha = 0.35f),
            modifier = Modifier.size(12.dp)
        )
    }
}

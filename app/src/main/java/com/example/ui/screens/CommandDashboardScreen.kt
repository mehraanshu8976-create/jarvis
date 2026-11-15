package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SettingsInputComponent
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SkillEntity
import com.example.data.preferences.JarvisUserSettings
import com.example.device.SystemTelemetry
import com.example.ui.components.ArcReactorCore
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CoreGold
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.NeuralPurple
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar

@Composable
fun CommandDashboardScreen(
    settings: JarvisUserSettings,
    telemetry: SystemTelemetry,
    skills: List<SkillEntity>,
    flashlightOn: Boolean,
    isSpeaking: Boolean,
    statusBanner: String?,
    onOpenVoiceOverlay: () -> Unit,
    onToggleFpsBoost: (Activity?, Boolean) -> Unit,
    onToggleFlashlight: () -> Unit,
    onQuickYouTubeSearch: (String) -> Unit,
    onNavigateToVoiceLab: () -> Unit,
    onNavigateToSkills: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showYouTubeDialog by remember { mutableStateOf(false) }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning,"
            hour < 17 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
    }

    val linkedSkillsCount = skills.count { it.isConnected && it.isEnabled }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Greeting Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_jarvis_emblem),
                            contentDescription = "JARVIS Emblem",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, ArcCyan, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = settings.commanderName,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HeaderIconBtn(
                            icon = Icons.Rounded.RecordVoiceOver,
                            contentDescription = "Voice Lab",
                            onClick = onNavigateToVoiceLab,
                            testTag = "header_voicelab_button"
                        )
                        HeaderIconBtn(
                            icon = Icons.Rounded.Extension,
                            contentDescription = "Skills & API",
                            onClick = onNavigateToSkills,
                            testTag = "header_skills_button"
                        )
                        HeaderIconBtn(
                            icon = Icons.Rounded.Tune,
                            contentDescription = "Commander Profile",
                            onClick = onNavigateToProfile,
                            testTag = "header_profile_button"
                        )
                    }
                }
            }

            // Hero Arc Reactor + System Health / AI Core Status
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SideTelemetryCard(
                        title = "SYSTEM\nHEALTH",
                        value = "${telemetry.systemHealthScore}%",
                        badge = if (telemetry.systemHealthScore >= 75) "OPTIMAL" else "CHECK",
                        badgeColor = if (telemetry.systemHealthScore >= 75) EmeraldOnline else CoreGold,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable(onClick = onOpenVoiceOverlay)
                            .testTag("hero_arc_reactor"),
                        contentAlignment = Alignment.Center
                    ) {
                        ArcReactorCore(
                            size = 152.dp,
                            isActivePulse = isSpeaking || settings.fpsBoostEnabled,
                            isBoostMode = settings.fpsBoostEnabled
                        )
                    }

                    SideTelemetryCard(
                        title = "AI CORE\nSTATUS",
                        value = if (isSpeaking) "SPEAK" else "READY",
                        badge = settings.activeAccent.displayName.substringBefore(" "),
                        badgeColor = ArcCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Status / AI Insight Pill Banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = ArcCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = statusBanner ?: telemetry.lastBoostMessage
                            ?: "All systems nominal. Tap Talk to Jarvis to command your phone in Hindi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Clean & FPS Boost Control Panel
            item {
                val boosted = settings.fpsBoostEnabled
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    ObsidianCard,
                                    if (boosted) ArcBlue.copy(alpha = 0.25f) else ObsidianCardElevated
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = if (boosted) ArcCyan else ObsidianBorder,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = if (boosted) EmeraldOnline else ArcCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CLEAN & FPS BOOST ENGINE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (boosted) EmeraldOnline else ArcCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Live ${telemetry.fps} FPS (${telemetry.frameTimeMs}ms) • Cache ${telemetry.cacheSizeKb} KB",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }

                        Button(
                            onClick = { onToggleFpsBoost(activity, !boosted) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (boosted) EmeraldOnline else ArcCyan,
                                contentColor = ObsidianDeep
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("fps_boost_button")
                        ) {
                            Icon(
                                imageVector = if (boosted) Icons.Rounded.Bolt else Icons.Rounded.CleaningServices,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (boosted) "BOOSTED" else "BOOST NOW",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // LIVE INTELLIGENCE (Real Hardware Telemetry)
            item {
                SectionCard(
                    title = "LIVE INTELLIGENCE",
                    trailingBadge = if (settings.fpsBoostEnabled) "MAX REFRESH" else "REAL-TIME",
                    trailingColor = if (settings.fpsBoostEnabled) EmeraldOnline else CoreGold
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryMiniTile(
                            label = "FRAME RATE",
                            value = "${telemetry.fps} FPS",
                            accent = ArcCyan,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMiniTile(
                            label = "MEMORY",
                            value = "${telemetry.memoryUsedPercent}%",
                            accent = NeuralPurple,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMiniTile(
                            label = "NETWORK",
                            value = telemetry.networkStatus,
                            accent = EmeraldOnline,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMiniTile(
                            label = "TEMP",
                            value = "${telemetry.batteryTempCelsius}°C",
                            accent = CoreGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // QUICK ACTIONS
            item {
                SectionCard(
                    title = "QUICK ACTIONS",
                    trailingBadge = "$linkedSkillsCount SKILLS LINKED",
                    trailingColor = ArcCyan
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionTile(
                                label = "Talk to Jarvis",
                                icon = Icons.Rounded.Mic,
                                accent = ArcCyan,
                                onClick = onOpenVoiceOverlay,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_action_talk")
                            )
                            QuickActionTile(
                                label = "YouTube Play",
                                icon = Icons.Rounded.PlayCircle,
                                accent = CoreGold,
                                onClick = { showYouTubeDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_action_youtube")
                            )
                            QuickActionTile(
                                label = if (flashlightOn) "Torch ON" else "Torch OFF",
                                icon = if (flashlightOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                                accent = if (flashlightOn) EmeraldOnline else TextSecondary,
                                onClick = onToggleFlashlight,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_action_flashlight")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionTile(
                                label = "Voice Lab",
                                icon = Icons.Rounded.RecordVoiceOver,
                                accent = NeuralPurple,
                                onClick = onNavigateToVoiceLab,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_action_voicelab")
                            )
                            QuickActionTile(
                                label = "Skills & API",
                                icon = Icons.Rounded.SettingsInputComponent,
                                accent = ArcCyan,
                                onClick = onNavigateToSkills,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_action_skills")
                            )
                            QuickActionTile(
                                label = "FPS Clean",
                                icon = Icons.Rounded.CleaningServices,
                                accent = EmeraldOnline,
                                onClick = { onToggleFpsBoost(activity, true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_action_fps")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showYouTubeDialog) {
        var songQuery by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showYouTubeDialog = false },
            containerColor = ObsidianCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("YouTube Search & Play", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text("Enter a song or video to open and search on YouTube:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = songQuery,
                        onValueChange = { songQuery = it },
                        placeholder = { Text("e.g. Kesariya song") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArcCyan,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onQuickYouTubeSearch(songQuery)
                        showYouTubeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcCyan, contentColor = ObsidianDeep)
                ) {
                    Text("Launch YouTube")
                }
            },
            dismissButton = {
                TextButton(onClick = { showYouTubeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun HeaderIconBtn(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, CircleShape)
            .testTag(testTag)
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = ArcCyan)
    }
}

@Composable
private fun SideTelemetryCard(
    title: String,
    value: String,
    badge: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = badgeColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    trailingBadge: String,
    trailingColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
            Text(
                text = trailingBadge,
                style = MaterialTheme.typography.labelSmall,
                color = trailingColor
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun TelemetryMiniTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianDeep.copy(alpha = 0.85f))
            .border(1.dp, ObsidianBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianDeep.copy(alpha = 0.8f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.SkillEntity
import com.example.data.preferences.JarvisAiModel
import com.example.data.preferences.JarvisUserSettings
import com.example.data.preferences.VoiceAccent
import com.example.ui.components.ArcReactorCore
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CoreGold
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

enum class AiCoreSubTab(val label: String) {
    MODELS("Models"),
    VOICE_LAB("Voice Lab"),
    SKILLS("Skills & API")
}

@Composable
fun AiCoreScreen(
    initialSubTab: AiCoreSubTab = AiCoreSubTab.MODELS,
    settings: JarvisUserSettings,
    skills: List<SkillEntity>,
    isPlayingSample: Boolean,
    onSelectModel: (String) -> Unit,
    onSelectBehavior: (String) -> Unit,
    onVoicePitchChange: (Float) -> Unit,
    onVoiceSpeedChange: (Float) -> Unit,
    onVoiceAccentSelect: (VoiceAccent) -> Unit,
    onAutoSpeakToggle: (Boolean) -> Unit,
    onTestJarvisVoice: () -> Unit,
    onToggleReferenceSample: () -> Unit,
    onLinkPlatform: (String, String, String, String) -> Unit,
    onUnlinkSkill: (String) -> Unit,
    onToggleSkillEnabled: (SkillEntity, Boolean) -> Unit,
    onAddCustomSkill: (String, String, String, String, String, String, String, String) -> Unit,
    onTestSkill: (SkillEntity, String) -> Unit,
    onDeleteCustomSkill: (String) -> Unit,
    onExportManifest: () -> String,
    onImportManifest: (String) -> Result<String>
) {
    var activeSubTab by remember(initialSubTab) { mutableStateOf(initialSubTab) }
    var linkingSkill by remember { mutableStateOf<SkillEntity?>(null) }
    var showAddSkillDialog by remember { mutableStateOf(false) }
    var showManifestDialog by remember { mutableStateOf(false) }

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
            item {
                Column {
                    Text(
                        text = "AI CORE",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Your AI brain, Hindi voice studio & extensible skills center",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiCoreSubTab.entries.forEach { tab ->
                            val selected = activeSubTab == tab
                            FilterChip(
                                selected = selected,
                                onClick = { activeSubTab = tab },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ArcCyan,
                                    selectedLabelColor = ObsidianDeep,
                                    containerColor = ObsidianCard,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = ObsidianBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("aicore_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            when (activeSubTab) {
                AiCoreSubTab.MODELS -> {
                    item {
                        val active = settings.activeModel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(ObsidianCard)
                                .border(1.dp, CoreGold.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArcReactorCore(
                                size = 102.dp,
                                isActivePulse = true,
                                useGoldAccent = true
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ACTIVE MODEL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CoreGold
                                )
                                Text(
                                    text = active.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ModelSpecChip("INTELLIGENCE", active.intelligence, Modifier.weight(1f))
                                    ModelSpecChip("SPEED", active.speed, Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ModelSpecChip("REASONING", active.reasoning, Modifier.weight(1f))
                                    ModelSpecChip("CONTEXT", active.contextWindow, Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "SELECT GEMINI ENGINE",
                            style = MaterialTheme.typography.labelLarge,
                            color = ArcCyan
                        )
                    }

                    items(JarvisAiModel.entries) { model ->
                        val selected = settings.selectedModelId == model.modelId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) ObsidianCardElevated else ObsidianCard)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) CoreGold else ObsidianBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectModel(model.modelId) }
                                .minimumInteractiveComponentSize()
                                .padding(14.dp)
                                .testTag("model_card_${model.modelId}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Psychology,
                                        contentDescription = null,
                                        tint = if (selected) CoreGold else ArcCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = model.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${model.intelligence} • ${model.speed} • ${model.contextWindow} context",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            if (selected) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CoreGold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, CoreGold, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ObsidianCard)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "HINDI RESPONSE STYLE",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Concise", "Balanced", "Detailed").forEach { mode ->
                                    val selected = settings.modelBehavior == mode
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectBehavior(mode) },
                                        label = { Text(mode) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ArcCyan,
                                            selectedLabelColor = ObsidianDeep,
                                            containerColor = ObsidianDeep,
                                            labelColor = TextSecondary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                AiCoreSubTab.VOICE_LAB -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(ObsidianCard)
                                .border(1.dp, ArcCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "VOICE CUSTOMIZATION LAB",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ArcCyan
                                    )
                                    Text(
                                        text = "Preferences save automatically for all future Hindi interactions",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = ArcCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Reference sample player
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ObsidianDeep)
                                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Reference Voice Profile",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Play your uploaded target voice reference",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                Button(
                                    onClick = onToggleReferenceSample,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPlayingSample) CrimsonAlert else CoreGold,
                                        contentColor = ObsidianDeep
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("play_reference_sample_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingSample) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                        contentDescription = "Play reference voice"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isPlayingSample) "Stop" else "Sample")
                                }
                            }

                            // Pitch slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Voice Pitch", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(
                                        text = "${((settings.voicePitch * 100).roundToInt() / 100f)}x",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ArcCyan
                                    )
                                }
                                Slider(
                                    value = settings.voicePitch,
                                    onValueChange = onVoicePitchChange,
                                    valueRange = 0.50f..1.50f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ArcCyan,
                                        activeTrackColor = ArcCyan
                                    ),
                                    modifier = Modifier.testTag("voice_pitch_slider")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Deep Baritone (0.5x)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("High (1.5x)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }

                            // Speaking Speed slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Speaking Speed", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(
                                        text = "${((settings.voiceSpeed * 100).roundToInt() / 100f)}x",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ArcCyan
                                    )
                                }
                                Slider(
                                    value = settings.voiceSpeed,
                                    onValueChange = onVoiceSpeedChange,
                                    valueRange = 0.60f..1.60f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ArcCyan,
                                        activeTrackColor = ArcCyan
                                    ),
                                    modifier = Modifier.testTag("voice_speed_slider")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Calm Slow (0.6x)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("Rapid (1.6x)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }

                            // Auto-speak toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Speak Replies in Hindi", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("Speak every JARVIS response aloud automatically", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                                Switch(
                                    checked = settings.autoSpeakEnabled,
                                    onCheckedChange = onAutoSpeakToggle,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ObsidianDeep,
                                        checkedTrackColor = ArcCyan
                                    )
                                )
                            }

                            Button(
                                onClick = onTestJarvisVoice,
                                colors = ButtonDefaults.buttonColors(containerColor = ArcCyan, contentColor = ObsidianDeep),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("test_voice_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test JARVIS Hindi Voice", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    item {
                        Text("SELECT VOICE ACCENT", style = MaterialTheme.typography.labelLarge, color = ArcCyan)
                    }

                    items(VoiceAccent.entries) { accent ->
                        val selected = settings.voiceAccentId == accent.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) ObsidianCardElevated else ObsidianCard)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) ArcCyan else ObsidianBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onVoiceAccentSelect(accent) }
                                .minimumInteractiveComponentSize()
                                .padding(14.dp)
                                .testTag("accent_option_${accent.id}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(accent.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("${accent.localeTag} • ${accent.description}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            if (selected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected Accent", tint = ArcCyan)
                            }
                        }
                    }
                }

                AiCoreSubTab.SKILLS -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ObsidianCard)
                                .border(1.dp, ArcCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "EXTENSIBLE SKILL & API ENGINE",
                                style = MaterialTheme.typography.labelLarge,
                                color = ArcCyan
                            )
                            Text(
                                text = "Link Spotify, Google Calendar, or Smart Home hubs, or register third-party Webhook & Deep Link skills.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showAddSkillDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ArcCyan, contentColor = ObsidianDeep),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("add_custom_skill_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("New Skill", style = MaterialTheme.typography.labelLarge)
                                }
                                OutlinedButton(
                                    onClick = { showManifestDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("skill_manifest_button")
                                ) {
                                    Icon(Icons.Rounded.Code, contentDescription = null, tint = ArcCyan)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("JSON API", color = ArcCyan, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }

                    items(skills, key = { it.id }) { skill ->
                        SkillCard(
                            skill = skill,
                            onLinkClick = { linkingSkill = skill },
                            onUnlinkClick = { onUnlinkSkill(skill.id) },
                            onToggleEnabled = { enabled -> onToggleSkillEnabled(skill, enabled) },
                            onTestClick = { onTestSkill(skill, "test") },
                            onDeleteClick = { onDeleteCustomSkill(skill.id) }
                        )
                    }
                }
            }
        }
    }

    linkingSkill?.let { target ->
        LinkPlatformDialog(
            skill = target,
            onDismiss = { linkingSkill = null },
            onSave = { label, endpoint, token ->
                onLinkPlatform(target.id, label, endpoint, token)
                linkingSkill = null
            }
        )
    }

    if (showAddSkillDialog) {
        AddCustomSkillDialog(
            onDismiss = { showAddSkillDialog = false },
            onCreate = { name, cat, desc, type, endpoint, method, token, keywords ->
                onAddCustomSkill(name, cat, desc, type, endpoint, method, token, keywords)
                showAddSkillDialog = false
            }
        )
    }

    if (showManifestDialog) {
        SkillManifestDialog(
            exportedJson = onExportManifest(),
            onDismiss = { showManifestDialog = false },
            onImportJson = onImportManifest
        )
    }
}

@Composable
private fun ModelSpecChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianDeep)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.labelMedium, color = ArcCyan)
    }
}

@Composable
private fun SkillCard(
    skill: SkillEntity,
    onLinkClick: () -> Unit,
    onUnlinkClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onTestClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianCard)
            .border(
                width = 1.dp,
                color = if (skill.isConnected) EmeraldOnline.copy(alpha = 0.45f) else ObsidianBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Rounded.Extension,
                    contentDescription = null,
                    tint = if (skill.isConnected) EmeraldOnline else ArcCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(skill.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = "${skill.category} • ${if (skill.isConnected) "Linked (${skill.accountLabel})" else "Not Linked"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (skill.isConnected) EmeraldOnline else CoreGold
                    )
                }
            }
            Switch(
                checked = skill.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianDeep,
                    checkedTrackColor = ArcCyan
                )
            )
        }

        Text(skill.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

        if (skill.lastResult.isNotBlank()) {
            Text(
                text = "Status: ${skill.lastResult}",
                style = MaterialTheme.typography.labelSmall,
                color = ArcCyan
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (skill.id != "youtube") {
                    TextButton(onClick = if (skill.isConnected) onUnlinkClick else onLinkClick) {
                        Icon(Icons.Rounded.Link, contentDescription = null, tint = ArcCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (skill.isConnected) "Disconnect" else "Link Account", color = ArcCyan)
                    }
                }
                TextButton(onClick = onTestClick) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = EmeraldOnline, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Skill", color = EmeraldOnline)
                }
            }
            if (!skill.isBuiltIn) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Custom Skill", tint = CrimsonAlert)
                }
            }
        }
    }
}

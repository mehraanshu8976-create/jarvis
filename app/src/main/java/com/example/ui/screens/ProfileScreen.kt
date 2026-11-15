package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.SkillEntity
import com.example.data.preferences.JarvisUserSettings
import com.example.device.InstalledAppShortcut
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CoreGold
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    settings: JarvisUserSettings,
    skills: List<SkillEntity>,
    installedApps: List<InstalledAppShortcut>,
    onUpdateCommanderName: (String) -> Unit,
    onOpenVoiceLab: () -> Unit,
    onOpenSkillsHub: () -> Unit,
    onLaunchApp: (String) -> Unit
) {
    val context = LocalContext.current
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
    }
    var showEditNameDialog by remember { mutableStateOf(false) }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ArcCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_jarvis_emblem),
                        contentDescription = "Commander Avatar",
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .border(2.dp, ArcCyan, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("COMMANDER PROFILE", style = MaterialTheme.typography.labelSmall, color = ArcCyan)
                        Text(settings.commanderName, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Text(
                            text = "Voice: ${settings.activeAccent.displayName} • ${settings.activeModel.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = { showEditNameDialog = true },
                        modifier = Modifier.testTag("edit_commander_name_button")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Commander Name", tint = ArcCyan)
                    }
                }
            }

            // Voice & Skills Quick Config Summary
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianDeep)
                            .clickable(onClick = onOpenVoiceLab)
                            .minimumInteractiveComponentSize()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = ArcCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Voice Parameters", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    "Pitch ${settings.voicePitch}x • Speed ${settings.voiceSpeed}x • ${settings.activeAccent.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text("Tune", style = MaterialTheme.typography.labelLarge, color = ArcCyan)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianDeep)
                            .clickable(onClick = onOpenSkillsHub)
                            .minimumInteractiveComponentSize()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = EmeraldOnline)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Connected Skills", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    "${skills.count { it.isConnected }} of ${skills.size} platforms linked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text("Manage", style = MaterialTheme.typography.labelLarge, color = EmeraldOnline)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianDeep)
                            .clickable {
                                if (!micGranted) {
                                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            .minimumInteractiveComponentSize()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, tint = if (micGranted) EmeraldOnline else CoreGold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Microphone Access", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    if (micGranted) "Granted for Hindi & English speech" else "Tap to grant voice recognition access",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        if (micGranted) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Granted", tint = EmeraldOnline)
                        } else {
                            Text("Grant", style = MaterialTheme.typography.labelLarge, color = CoreGold)
                        }
                    }
                }
            }

            // Discovered Installed Apps for Quick Device Access
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Apps, contentDescription = null, tint = ArcCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DEVICE APP ACCESS (${installedApps.size} DISCOVERED)",
                        style = MaterialTheme.typography.labelLarge,
                        color = ArcCyan
                    )
                }
            }

            items(installedApps.take(20), key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                        .clickable { onLaunchApp(app.label) }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.OpenInNew, contentDescription = "Launch ${app.label}", tint = ArcCyan, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showEditNameDialog) {
        var nameInput by remember { mutableStateOf(settings.commanderName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = ObsidianCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Commander Name", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("How should JARVIS address you?") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCommanderName(nameInput)
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcCyan, contentColor = ObsidianDeep)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.SkillEntity
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LinkPlatformDialog(
    skill: SkillEntity,
    onDismiss: () -> Unit,
    onSave: (accountLabel: String, endpointOrUri: String, authToken: String) -> Unit
) {
    var accountLabel by remember { mutableStateOf(skill.accountLabel) }
    var endpointOrUri by remember { mutableStateOf(skill.endpointOrUri) }
    var authToken by remember { mutableStateOf(skill.authToken) }

    val accountHint = when (skill.id) {
        "spotify" -> "Spotify username or email"
        "google_calendar" -> "Google Calendar email"
        "smart_home" -> "Home Assistant / Hub name"
        else -> "Account or profile label"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Link ${skill.name}", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(skill.description, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = accountLabel,
                    onValueChange = { accountLabel = it },
                    label = { Text(accountHint) },
                    singleLine = true,
                    colors = jarvisFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (skill.skillType == "WEBHOOK_API" || skill.id == "smart_home") {
                    OutlinedTextField(
                        value = endpointOrUri,
                        onValueChange = { endpointOrUri = it },
                        label = { Text("Webhook / API Endpoint URL") },
                        placeholder = { Text("https://home.example.com/api/webhook") },
                        singleLine = true,
                        colors = jarvisFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = authToken,
                        onValueChange = { authToken = it },
                        label = { Text("Bearer Token / API Key (Optional)") },
                        singleLine = true,
                        colors = jarvisFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(accountLabel, endpointOrUri, authToken) },
                colors = ButtonDefaults.buttonColors(containerColor = ArcCyan, contentColor = ObsidianDeep)
            ) {
                Text("Link Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AddCustomSkillDialog(
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        category: String,
        description: String,
        skillType: String,
        endpointOrUri: String,
        httpMethod: String,
        authToken: String,
        triggerKeywords: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Developer API") }
    var description by remember { mutableStateOf("") }
    var skillType by remember { mutableStateOf("WEBHOOK_API") }
    var endpointOrUri by remember { mutableStateOf("") }
    var httpMethod by remember { mutableStateOf("POST") }
    var authToken by remember { mutableStateOf("") }
    var triggerKeywords by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Register Developer Skill", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Connect a third-party REST Webhook or Android Deep Link skill. JARVIS automatically routes matching voice commands to it.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Skill Name") },
                    placeholder = { Text("e.g. Home Lights or News Feed") },
                    singleLine = true,
                    colors = jarvisFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("WEBHOOK_API" to "Webhook API", "DEEP_LINK" to "Deep Link").forEach { (typeId, label) ->
                        FilterChip(
                            selected = skillType == typeId,
                            onClick = { skillType = typeId },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ArcCyan,
                                selectedLabelColor = ObsidianDeep
                            )
                        )
                    }
                }
                if (skillType == "WEBHOOK_API") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("POST", "GET").forEach { m ->
                            FilterChip(
                                selected = httpMethod == m,
                                onClick = { httpMethod = m },
                                label = { Text(m) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ArcCyan,
                                    selectedLabelColor = ObsidianDeep
                                )
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = endpointOrUri,
                    onValueChange = { endpointOrUri = it },
                    label = {
                        Text(if (skillType == "WEBHOOK_API") "HTTPS Endpoint ({query} supported)" else "Deep Link URI ({query} supported)")
                    },
                    placeholder = {
                        Text(if (skillType == "WEBHOOK_API") "https://api.example.com/jarvis?q={query}" else "spotify:search:{query}")
                    },
                    singleLine = true,
                    colors = jarvisFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = triggerKeywords,
                    onValueChange = { triggerKeywords = it },
                    label = { Text("Trigger Keywords (comma-separated)") },
                    placeholder = { Text("lights, bedroom, switch") },
                    singleLine = true,
                    colors = jarvisFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Skill Description for AI Router") },
                    singleLine = true,
                    colors = jarvisFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (skillType == "WEBHOOK_API") {
                    OutlinedTextField(
                        value = authToken,
                        onValueChange = { authToken = it },
                        label = { Text("Bearer Auth Token (Optional)") },
                        singleLine = true,
                        colors = jarvisFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && endpointOrUri.isNotBlank()) {
                        onCreate(name, category, description, skillType, endpointOrUri, httpMethod, authToken, triggerKeywords)
                    }
                },
                enabled = name.isNotBlank() && endpointOrUri.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ArcCyan, contentColor = ObsidianDeep)
            ) {
                Text("Register Skill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun SkillManifestDialog(
    exportedJson: String,
    onDismiss: () -> Unit,
    onImportJson: (String) -> Result<String>
) {
    var jsonInput by remember { mutableStateOf(exportedJson) }
    var feedback by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Developer Skill Manifest API", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Paste a JSON skill object or array to import external developer skills, or inspect the active manifest below:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    minLines = 6,
                    maxLines = 10,
                    colors = jarvisFieldColors(),
                    textStyle = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth()
                )
                if (feedback != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(feedback!!, style = MaterialTheme.typography.labelMedium, color = ArcCyan)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val res = onImportJson(jsonInput)
                    feedback = res.fold(
                        onSuccess = { it },
                        onFailure = { "Invalid JSON: ${it.localizedMessage}" }
                    )
                    if (res.isSuccess) onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ArcCyan, contentColor = ObsidianDeep)
            ) {
                Text("Import Manifest")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun jarvisFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ArcCyan,
    unfocusedBorderColor = ObsidianBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = ArcCyan,
    unfocusedLabelColor = TextSecondary,
    cursorColor = ArcCyan
)

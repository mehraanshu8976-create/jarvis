package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.JarvisAiRepository
import com.example.data.local.ChatMessageEntity
import com.example.data.local.JarvisDatabase
import com.example.data.local.SkillEntity
import com.example.data.preferences.JarvisPreferencesRepository
import com.example.data.preferences.JarvisUserSettings
import com.example.data.preferences.VoiceAccent
import com.example.device.DeviceActionResult
import com.example.device.DeviceControlExecutor
import com.example.device.InstalledAppShortcut
import com.example.device.SystemTelemetry
import com.example.device.SystemTelemetryMonitor
import com.example.voice.JarvisVoiceEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed interface AiResponseState {
    data object Idle : AiResponseState
    data object Thinking : AiResponseState
    data class Success(val replyHindi: String, val actionResult: DeviceActionResult?) : AiResponseState
    data class Error(val message: String) : AiResponseState
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = JarvisPreferencesRepository(application)
    private val database = JarvisDatabase.getInstance(application)
    private val dao = database.jarvisDao()
    private val aiRepo = JarvisAiRepository()

    val telemetryMonitor = SystemTelemetryMonitor(application)
    val deviceExecutor = DeviceControlExecutor(application, telemetryMonitor)
    val voiceEngine = JarvisVoiceEngine(application)

    val settings: StateFlow<JarvisUserSettings> = prefsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JarvisUserSettings())

    val messages: StateFlow<List<ChatMessageEntity>> = dao.observeMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val skills: StateFlow<List<SkillEntity>> = dao.observeSkills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val telemetry: StateFlow<SystemTelemetry> = telemetryMonitor.telemetry
    val isSpeaking: StateFlow<Boolean> = voiceEngine.isSpeaking
    val isPlayingSample: StateFlow<Boolean> = voiceEngine.isPlayingSample

    private val _aiState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val aiState: StateFlow<AiResponseState> = _aiState.asStateFlow()

    private val _flashlightOn = MutableStateFlow(false)
    val flashlightOn: StateFlow<Boolean> = _flashlightOn.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppShortcut>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppShortcut>> = _installedApps.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        viewModelScope.launch {
            dao.insertSkillsIfAbsent(JarvisDatabase.defaultSkills)
            _installedApps.value = deviceExecutor.getInstalledAppShortcuts()
            if (dao.observeMessages().first().isEmpty()) {
                dao.insertMessage(
                    ChatMessageEntity(
                        role = "jarvis",
                        text = "नमस्ते कमांडर, जार्विस ऑनलाइन है। आप मुझसे कुछ भी पूछ सकते हैं या अपने मोबाइल को कंट्रोल करने के लिए कमांड दे सकते हैं।",
                        actionBadge = "System Online",
                        actionStatus = "All AI modules ready"
                    )
                )
            }
        }
        viewModelScope.launch {
            prefsRepo.settingsFlow.collect { s ->
                voiceEngine.applyVoiceConfig(s.voicePitch, s.voiceSpeed, s.activeAccent)
            }
        }
    }

    fun startTelemetry() {
        telemetryMonitor.startMonitoring()
    }

    fun stopTelemetry() {
        telemetryMonitor.stopMonitoring()
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun sendCommand(query: String, activity: Activity?) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _aiState.value = AiResponseState.Thinking
            dao.insertMessage(ChatMessageEntity(role = "user", text = trimmed))

            try {
                val currentSettings = settings.value
                val currentSkills = skills.value
                val historyPairs = messages.value.takeLast(6).map { it.role to it.text }
                val appLabels = _installedApps.value.map { it.label }

                val aiResult = aiRepo.askJarvis(
                    userMessage = trimmed,
                    commanderName = currentSettings.commanderName,
                    modelId = currentSettings.selectedModelId,
                    modelBehavior = currentSettings.modelBehavior,
                    recentHistory = historyPairs,
                    skills = currentSkills,
                    installedAppsPreview = appLabels
                )

                val targetSkill = if (aiResult.actionType == "EXECUTE_SKILL") {
                    currentSkills.firstOrNull { it.id.equals(aiResult.skillId, ignoreCase = true) }
                        ?: currentSkills.firstOrNull {
                            it.isEnabled && it.triggerKeywords.split(",").any { kw ->
                                kw.trim().isNotEmpty() && trimmed.contains(kw.trim(), ignoreCase = true)
                            }
                        }
                } else {
                    null
                }

                val actionResult = if (aiResult.actionType != "NONE") {
                    if (aiResult.actionType == "RUN_FPS_BOOST") {
                        prefsRepo.setFpsBoost(true)
                    } else if (aiResult.actionType == "TOGGLE_FLASHLIGHT") {
                        _flashlightOn.value = aiResult.actionParam.lowercase() !in listOf("off", "false", "0", "band")
                    }
                    deviceExecutor.executeAction(
                        activity = activity,
                        actionType = aiResult.actionType,
                        actionParam = aiResult.actionParam,
                        secondaryParam = aiResult.secondaryParam,
                        skill = targetSkill
                    )
                } else {
                    null
                }

                if (targetSkill != null && actionResult != null) {
                    dao.upsertSkill(targetSkill.copy(lastResult = actionResult.statusDetail))
                }

                dao.insertMessage(
                    ChatMessageEntity(
                        role = "jarvis",
                        text = aiResult.spokenReplyHindi,
                        actionBadge = actionResult?.badge?.takeIf { it.isNotBlank() },
                        actionStatus = actionResult?.statusDetail?.takeIf { it.isNotBlank() }
                    )
                )

                if (actionResult != null && actionResult.statusDetail.isNotBlank()) {
                    _statusBanner.value = "${actionResult.badge}: ${actionResult.statusDetail}"
                }

                _aiState.value = AiResponseState.Success(aiResult.spokenReplyHindi, actionResult)

                if (currentSettings.autoSpeakEnabled) {
                    voiceEngine.speakHindi(aiResult.spokenReplyHindi)
                }
            } catch (e: TimeoutCancellationException) {
                val msg = "Request timed out after 60 seconds. Check your network and retry."
                _aiState.value = AiResponseState.Error(msg)
                dao.insertMessage(
                    ChatMessageEntity(
                        role = "jarvis",
                        text = "क्षमा करें कमांडर, नेटवर्क धीमा होने के कारण जवाब मिलने में समय लग गया। कृपया पुनः प्रयास करें।",
                        actionBadge = "Timeout",
                        actionStatus = msg
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errText = e.localizedMessage ?: "AI Core failed to respond."
                _aiState.value = AiResponseState.Error(errText)
                dao.insertMessage(
                    ChatMessageEntity(
                        role = "jarvis",
                        text = "कमांडर, एआई कोर से संपर्क करने में समस्या आई है। कृपया कनेक्शन की जांच करें।",
                        actionBadge = "AI Core Error",
                        actionStatus = errText
                    )
                )
            }
        }
    }

    fun triggerFpsBoost(activity: Activity?, enable: Boolean) {
        viewModelScope.launch {
            prefsRepo.setFpsBoost(enable)
            val (_, msg) = telemetryMonitor.runCleanAndFpsBoost(activity, enable)
            _statusBanner.value = msg
            if (settings.value.autoSpeakEnabled) {
                val spoken = if (enable) {
                    "एफपीएस बूस्ट और कैश क्लीन पूरा हो गया है कमांडर। सिस्टम अब अधिकतम परफॉर्मेंस पर है।"
                } else {
                    "स्टैंडर्ड परफॉर्मेंस मोड सक्रिय कर दिया गया है।"
                }
                voiceEngine.speakHindi(spoken)
            }
        }
    }

    fun toggleFlashlight() {
        val next = !_flashlightOn.value
        val result = deviceExecutor.toggleFlashlight(next)
        if (result.executed) {
            _flashlightOn.value = next
        }
        _statusBanner.value = "${result.badge}: ${result.statusDetail}"
    }

    fun launchInstalledApp(appLabel: String) {
        val result = deviceExecutor.openAppByName(appLabel)
        _statusBanner.value = "${result.badge}: ${result.statusDetail}"
    }

    fun quickYouTubeSearch(query: String) {
        val result = deviceExecutor.openYouTubeSearch(query)
        _statusBanner.value = "${result.badge}: ${result.statusDetail}"
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            dao.clearMessages()
            dao.insertMessage(
                ChatMessageEntity(
                    role = "jarvis",
                    text = "चैट हिस्ट्री साफ कर दी गई है कमांडर। नया कमांड दें।",
                    actionBadge = "Memory Cleared",
                    actionStatus = "Ready"
                )
            )
        }
    }

    fun setCommanderName(name: String) {
        viewModelScope.launch { prefsRepo.setCommanderName(name) }
    }

    fun setSelectedModel(modelId: String) {
        viewModelScope.launch { prefsRepo.setSelectedModel(modelId) }
    }

    fun setModelBehavior(behavior: String) {
        viewModelScope.launch { prefsRepo.setModelBehavior(behavior) }
    }

    fun setVoicePitch(pitch: Float) {
        viewModelScope.launch { prefsRepo.setVoicePitch(pitch) }
    }

    fun setVoiceSpeed(speed: Float) {
        viewModelScope.launch { prefsRepo.setVoiceSpeed(speed) }
    }

    fun setVoiceAccent(accent: VoiceAccent, applyRecommendedTuning: Boolean = false) {
        viewModelScope.launch {
            prefsRepo.setVoiceAccent(accent.id)
            if (applyRecommendedTuning) {
                prefsRepo.setVoicePitch(accent.recommendedPitch)
                prefsRepo.setVoiceSpeed(accent.recommendedSpeed)
            }
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoSpeak(enabled) }
    }

    fun speakVoiceTest() {
        val name = settings.value.commanderName
        voiceEngine.speakHindi(
            "नमस्ते $name, मैं जार्विस हूँ। आपकी चुनी हुई आवाज, पिच और गति के साथ सभी सिस्टम सक्रिय हैं।"
        )
    }

    fun linkPlatformSkill(
        skillId: String,
        accountLabel: String,
        endpointOrUri: String,
        authToken: String
    ) {
        viewModelScope.launch {
            val existing = dao.getSkillById(skillId) ?: return@launch
            val updated = existing.copy(
                isConnected = true,
                isEnabled = true,
                accountLabel = accountLabel.trim().ifEmpty { "Linked Account" },
                endpointOrUri = endpointOrUri.trim().ifEmpty { existing.endpointOrUri },
                authToken = authToken.trim(),
                lastResult = "Linked and ready"
            )
            dao.upsertSkill(updated)
            _statusBanner.value = "${existing.name} linked successfully"
        }
    }

    fun unlinkSkill(skillId: String) {
        viewModelScope.launch {
            val existing = dao.getSkillById(skillId) ?: return@launch
            dao.upsertSkill(
                existing.copy(
                    isConnected = false,
                    accountLabel = "",
                    authToken = "",
                    lastResult = "Disconnected"
                )
            )
            _statusBanner.value = "${existing.name} disconnected"
        }
    }

    fun toggleSkillEnabled(skill: SkillEntity, enabled: Boolean) {
        viewModelScope.launch {
            dao.upsertSkill(skill.copy(isEnabled = enabled))
        }
    }

    fun addCustomSkill(
        name: String,
        category: String,
        description: String,
        skillType: String,
        endpointOrUri: String,
        httpMethod: String,
        authToken: String,
        triggerKeywords: String
    ) {
        viewModelScope.launch {
            val id = "skill_${UUID.randomUUID().toString().take(8)}"
            val skill = SkillEntity(
                id = id,
                name = name.trim(),
                category = category.trim().ifEmpty { "Developer API" },
                description = description.trim().ifEmpty { "Custom external skill integration" },
                skillType = skillType,
                isBuiltIn = false,
                isConnected = true,
                isEnabled = true,
                accountLabel = if (skillType == "DEEP_LINK") "Deep Link" else "Webhook API",
                endpointOrUri = endpointOrUri.trim(),
                httpMethod = httpMethod,
                authToken = authToken.trim(),
                triggerKeywords = triggerKeywords.trim(),
                lastResult = "Registered"
            )
            dao.upsertSkill(skill)
            _statusBanner.value = "Skill \"${skill.name}\" registered"
        }
    }

    fun importSkillManifestJson(rawJson: String): Result<String> {
        return try {
            val trimmed = rawJson.trim()
            val array = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                JSONArray().put(JSONObject(trimmed))
            }
            var count = 0
            viewModelScope.launch {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val name = obj.optString("name").trim()
                    val endpoint = obj.optString("endpointOrUri").ifBlank { obj.optString("endpoint") }.trim()
                    if (name.isNotEmpty() && endpoint.isNotEmpty()) {
                        val skill = SkillEntity(
                            id = obj.optString("id").ifBlank { "skill_${UUID.randomUUID().toString().take(8)}" },
                            name = name,
                            category = obj.optString("category", "Developer API"),
                            description = obj.optString("description", "Imported developer skill"),
                            skillType = obj.optString("skillType", "WEBHOOK_API"),
                            isBuiltIn = false,
                            isConnected = true,
                            isEnabled = true,
                            accountLabel = "Manifest Import",
                            endpointOrUri = endpoint,
                            httpMethod = obj.optString("httpMethod", "POST"),
                            authToken = obj.optString("authToken", ""),
                            triggerKeywords = obj.optString("triggerKeywords", name.lowercase()),
                            lastResult = "Imported via Skill API"
                        )
                        dao.upsertSkill(skill)
                        count++
                    }
                }
                _statusBanner.value = "Imported skill manifest successfully"
            }
            Result.success("Manifest parsed and queued")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportSkillsManifestJson(): String {
        val arr = JSONArray()
        skills.value.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("category", s.category)
                put("description", s.description)
                put("skillType", s.skillType)
                put("endpointOrUri", s.endpointOrUri)
                put("httpMethod", s.httpMethod)
                put("triggerKeywords", s.triggerKeywords)
            })
        }
        return arr.toString(2)
    }

    fun testSkill(skill: SkillEntity, sampleQuery: String) {
        viewModelScope.launch {
            val result = deviceExecutor.executeSkill(skill, sampleQuery.ifBlank { "status" })
            dao.upsertSkill(skill.copy(lastResult = result.statusDetail))
            _statusBanner.value = "${result.badge}: ${result.statusDetail}"
        }
    }

    fun deleteCustomSkill(skillId: String) {
        viewModelScope.launch {
            dao.deleteCustomSkill(skillId)
            _statusBanner.value = "Custom skill removed"
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetryMonitor.stopMonitoring()
        voiceEngine.release()
    }
}

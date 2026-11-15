package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarvisDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jarvis_preferences"
)

enum class VoiceAccent(
    val id: String,
    val displayName: String,
    val localeTag: String,
    val description: String,
    val recommendedPitch: Float,
    val recommendedSpeed: Float
) {
    HINDI_BARITONE(
        id = "hi_baritone",
        displayName = "Hindi Baritone",
        localeTag = "hi-IN",
        description = "Deep resonant Hindi cinematic voice",
        recommendedPitch = 0.78f,
        recommendedSpeed = 0.95f
    ),
    HINDI_NEURAL(
        id = "hi_standard",
        displayName = "Hindi Natural",
        localeTag = "hi-IN",
        description = "Balanced clear conversational Hindi",
        recommendedPitch = 0.96f,
        recommendedSpeed = 1.00f
    ),
    INDIAN_ENGLISH_HINDI(
        id = "en_in_hinglish",
        displayName = "Indian Hinglish",
        localeTag = "en-IN",
        description = "Sharp Indian cadence for mixed commands",
        recommendedPitch = 0.84f,
        recommendedSpeed = 0.98f
    ),
    BRITISH_COMMAND(
        id = "en_gb_jarvis",
        displayName = "British Protocol",
        localeTag = "en-GB",
        description = "Classic refined British butler inflection",
        recommendedPitch = 0.82f,
        recommendedSpeed = 0.94f
    );

    companion object {
        fun fromId(id: String?): VoiceAccent =
            entries.firstOrNull { it.id == id } ?: HINDI_BARITONE
    }
}

enum class JarvisAiModel(
    val modelId: String,
    val displayName: String,
    val intelligence: String,
    val speed: String,
    val reasoning: String,
    val contextWindow: String,
    val badge: String
) {
    GEMINI_FLASH(
        modelId = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        intelligence = "Advanced",
        speed = "Fast",
        reasoning = "High",
        contextWindow = "1M",
        badge = "DEFAULT"
    ),
    GEMINI_FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        intelligence = "Standard+",
        speed = "Ultra Fast",
        reasoning = "Balanced",
        contextWindow = "1M",
        badge = "LOW LATENCY"
    ),
    GEMINI_ARGON(
        modelId = "gemini-4-argon",
        displayName = "Gemini 4 Argon",
        intelligence = "Frontier",
        speed = "Moderate",
        reasoning = "Deep",
        contextWindow = "2M",
        badge = "PRO REASONING"
    );

    companion object {
        fun fromId(id: String?): JarvisAiModel =
            entries.firstOrNull { it.modelId == id } ?: GEMINI_FLASH
    }
}

data class JarvisUserSettings(
    val commanderName: String = "Commander",
    val selectedModelId: String = JarvisAiModel.GEMINI_FLASH.modelId,
    val modelBehavior: String = "Balanced",
    val voicePitch: Float = 0.78f,
    val voiceSpeed: Float = 0.95f,
    val voiceAccentId: String = VoiceAccent.HINDI_BARITONE.id,
    val autoSpeakEnabled: Boolean = true,
    val fpsBoostEnabled: Boolean = false
) {
    val activeAccent: VoiceAccent get() = VoiceAccent.fromId(voiceAccentId)
    val activeModel: JarvisAiModel get() = JarvisAiModel.fromId(selectedModelId)
}

class JarvisPreferencesRepository(private val context: Context) {

    private object Keys {
        val COMMANDER_NAME = stringPreferencesKey("commander_name")
        val SELECTED_MODEL = stringPreferencesKey("selected_model_id")
        val MODEL_BEHAVIOR = stringPreferencesKey("model_behavior")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_ACCENT = stringPreferencesKey("voice_accent_id")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak_enabled")
        val FPS_BOOST = booleanPreferencesKey("fps_boost_enabled")
    }

    val settingsFlow: Flow<JarvisUserSettings> = context.jarvisDataStore.data.map { prefs ->
        JarvisUserSettings(
            commanderName = prefs[Keys.COMMANDER_NAME] ?: "Commander",
            selectedModelId = prefs[Keys.SELECTED_MODEL] ?: JarvisAiModel.GEMINI_FLASH.modelId,
            modelBehavior = prefs[Keys.MODEL_BEHAVIOR] ?: "Balanced",
            voicePitch = (prefs[Keys.VOICE_PITCH] ?: 0.78f).coerceIn(0.5f, 1.5f),
            voiceSpeed = (prefs[Keys.VOICE_SPEED] ?: 0.95f).coerceIn(0.6f, 1.6f),
            voiceAccentId = prefs[Keys.VOICE_ACCENT] ?: VoiceAccent.HINDI_BARITONE.id,
            autoSpeakEnabled = prefs[Keys.AUTO_SPEAK] ?: true,
            fpsBoostEnabled = prefs[Keys.FPS_BOOST] ?: false
        )
    }

    suspend fun setCommanderName(name: String) {
        val clean = name.trim().ifEmpty { "Commander" }
        context.jarvisDataStore.edit { it[Keys.COMMANDER_NAME] = clean }
    }

    suspend fun setSelectedModel(modelId: String) {
        context.jarvisDataStore.edit { it[Keys.SELECTED_MODEL] = modelId }
    }

    suspend fun setModelBehavior(behavior: String) {
        context.jarvisDataStore.edit { it[Keys.MODEL_BEHAVIOR] = behavior }
    }

    suspend fun setVoicePitch(pitch: Float) {
        context.jarvisDataStore.edit { it[Keys.VOICE_PITCH] = pitch.coerceIn(0.5f, 1.5f) }
    }

    suspend fun setVoiceSpeed(speed: Float) {
        context.jarvisDataStore.edit { it[Keys.VOICE_SPEED] = speed.coerceIn(0.6f, 1.6f) }
    }

    suspend fun setVoiceAccent(accentId: String) {
        context.jarvisDataStore.edit { it[Keys.VOICE_ACCENT] = accentId }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun setFpsBoost(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.FPS_BOOST] = enabled }
    }
}

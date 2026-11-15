package com.example.ai

import android.util.Log
import com.example.data.local.SkillEntity
import com.example.data.preferences.JarvisAiModel
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "JarvisAiRepository"

data class JarvisStructuredResponse(
    val spokenReplyHindi: String,
    val actionType: String,
    val actionParam: String,
    val secondaryParam: String,
    val skillId: String
)

class JarvisAiRepository {

    private val jarvisResponseSchema = Schema.obj(
        properties = mapOf(
            "spokenReplyHindi" to Schema.string(
                description = "JARVIS response strictly in natural, clear Hindi (Devanagari script). Respectful, intelligent, concise."
            ),
            "actionType" to Schema.enumeration(
                values = listOf(
                    "NONE",
                    "OPEN_YOUTUBE_SEARCH",
                    "OPEN_APP",
                    "RUN_FPS_BOOST",
                    "TOGGLE_FLASHLIGHT",
                    "SET_ALARM",
                    "SET_TIMER",
                    "WEB_SEARCH",
                    "OPEN_MAPS",
                    "DIAL_NUMBER",
                    "OPEN_SETTINGS",
                    "EXECUTE_SKILL"
                ),
                description = "Device or skill action to execute on Android."
            ),
            "actionParam" to Schema.string(
                description = "Primary parameter: song/video search query for YouTube, app name for OPEN_APP, query for skill/web/maps, 'on'/'off' for flashlight, HH:MM for alarm, seconds for timer, or phone number."
            ),
            "secondaryParam" to Schema.string(
                description = "Optional extra detail such as alarm/timer label or settings panel name."
            ),
            "skillId" to Schema.string(
                description = "Exact skill id when actionType is EXECUTE_SKILL, otherwise empty string."
            )
        ),
        optionalProperties = listOf("secondaryParam", "skillId")
    )

    private val defaultModel by lazy {
        createModel(JarvisAiModel.GEMINI_FLASH.modelId)
    }

    private fun createModel(modelId: String) = Firebase.ai.generativeModel(
        modelName = modelId,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = jarvisResponseSchema
            temperature = 0.6f
        }
    )

    suspend fun askJarvis(
        userMessage: String,
        commanderName: String,
        modelId: String,
        modelBehavior: String,
        recentHistory: List<Pair<String, String>>,
        skills: List<SkillEntity>,
        installedAppsPreview: List<String>
    ): JarvisStructuredResponse {
        val model = if (modelId == JarvisAiModel.GEMINI_FLASH.modelId) {
            defaultModel
        } else {
            createModel(modelId)
        }

        val activeSkillsSummary = skills.filter { it.isEnabled }.joinToString("\n") { s ->
            "- id=${s.id}, name=${s.name}, connected=${s.isConnected}, type=${s.skillType}, keywords=[${s.triggerKeywords}], desc=${s.description}"
        }

        val behaviorGuidance = when (modelBehavior) {
            "Concise" -> "Keep replies crisp in 1 to 2 short Hindi sentences."
            "Detailed" -> "Give thorough, insightful Hindi explanations while staying natural."
            else -> "Keep replies balanced, clear, and conversational in 2 to 3 Hindi sentences."
        }

        val systemContext = """
            You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), $commanderName's personal AI assistant and Android device controller.
            CRITICAL LANGUAGE RULE:
            - ALWAYS write `spokenReplyHindi` in natural, clear Hindi (Devanagari script, e.g., "जी $commanderName, मैं यूट्यूब खोलकर यह गाना चला रहा हूँ।").
            - Even if the user writes or speaks in English, Hinglish, or Hindi, you MUST always reply in Hindi!
            - $behaviorGuidance
            
            KNOWLEDGE & CAPABILITIES:
            - Answer any general knowledge, science, coding, daily advice, math, or world question accurately in Hindi.
            - You also control the user's Android phone via `actionType`:
              1. OPEN_YOUTUBE_SEARCH: When user wants to open YouTube and search/play a song or video (put the song/video query in `actionParam`).
              2. OPEN_APP: When user asks to open an app (e.g., WhatsApp, Chrome, Camera, Settings, Maps). Installed apps sample: ${installedAppsPreview.take(25).joinToString(", ")}.
              3. RUN_FPS_BOOST: When user asks to clean cache, boost FPS, speed up phone, or optimize performance.
              4. TOGGLE_FLASHLIGHT: When user asks to turn flashlight/torch on or off (`actionParam` = "on" or "off").
              5. SET_ALARM: `actionParam` = "HH:MM" (24h), `secondaryParam` = label.
              6. SET_TIMER: `actionParam` = seconds as integer string, `secondaryParam` = label.
              7. WEB_SEARCH: Search Google or open a website URL.
              8. OPEN_MAPS: Navigate or search a place in Maps.
              9. DIAL_NUMBER: Open phone dialer for a phone number.
              10. OPEN_SETTINGS: Open Wi-Fi, Bluetooth, Volume, or System Settings (`actionParam` = panel name).
              11. EXECUTE_SKILL: Route to one of the registered skills below (set `skillId` to exact skill id and `actionParam` to the query/command):
            $activeSkillsSummary
              12. NONE: For pure conversation or general knowledge questions.
        """.trimIndent()

        val promptContents = buildList<Content> {
            add(content("user") { text(systemContext) })
            add(content("model") {
                text("""{"spokenReplyHindi":"जी $commanderName, मैं पूरी तरह तैयार हूँ। बताइए मैं आपकी क्या सहायता कर सकता हूँ?","actionType":"NONE","actionParam":"","secondaryParam":"","skillId":""}""")
            })
            recentHistory.takeLast(6).forEach { (role, text) ->
                val geminiRole = if (role == "user") "user" else "model"
                add(content(geminiRole) { text(text) })
            }
            add(content("user") { text(userMessage) })
        }

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(promptContents)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("JARVIS AI returned an empty response.")
            val json = JSONObject(jsonText)
            return JarvisStructuredResponse(
                spokenReplyHindi = json.optString("spokenReplyHindi")
                    .ifBlank { "जी $commanderName, आपका अनुरोध पूरा किया जा रहा है।" },
                actionType = json.optString("actionType", "NONE"),
                actionParam = json.optString("actionParam", ""),
                secondaryParam = json.optString("secondaryParam", ""),
                skillId = json.optString("skillId", "")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "JARVIS inference timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "JARVIS inference failed", e)
            throw e
        }
    }
}

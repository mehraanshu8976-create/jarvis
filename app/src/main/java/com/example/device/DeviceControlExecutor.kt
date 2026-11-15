package com.example.device

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.local.SkillEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class InstalledAppShortcut(
    val label: String,
    val packageName: String
)

data class DeviceActionResult(
    val executed: Boolean,
    val badge: String,
    val statusDetail: String
)

class DeviceControlExecutor(
    private val context: Context,
    private val telemetryMonitor: SystemTelemetryMonitor
) {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun getInstalledAppShortcuts(): List<InstalledAppShortcut> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }
        return resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = info.loadLabel(pm).toString().trim()
                if (label.isBlank()) null else InstalledAppShortcut(label = label, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun executeAction(
        activity: Activity?,
        actionType: String,
        actionParam: String,
        secondaryParam: String,
        skill: SkillEntity?
    ): DeviceActionResult {
        return when (actionType.uppercase()) {
            "OPEN_YOUTUBE_SEARCH" -> openYouTubeSearch(actionParam)
            "OPEN_APP" -> openAppByName(actionParam)
            "RUN_FPS_BOOST" -> {
                val (_, msg) = telemetryMonitor.runCleanAndFpsBoost(activity, enableBoost = true)
                DeviceActionResult(true, "FPS Boost & Clean", msg)
            }
            "TOGGLE_FLASHLIGHT" -> toggleFlashlight(
                turnOn = actionParam.lowercase() !in listOf("off", "false", "0", "band")
            )
            "SET_ALARM" -> setAlarm(actionParam, secondaryParam)
            "SET_TIMER" -> setTimer(actionParam, secondaryParam)
            "WEB_SEARCH" -> openWebSearch(actionParam)
            "OPEN_MAPS" -> openMaps(actionParam)
            "DIAL_NUMBER" -> dialNumber(actionParam)
            "OPEN_SETTINGS" -> openSettingsPanel(actionParam)
            "EXECUTE_SKILL" -> executeSkill(skill, actionParam)
            else -> DeviceActionResult(false, "", "")
        }
    }

    fun openYouTubeSearch(query: String): DeviceActionResult {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return openAppByName("YouTube")
        }

        // First try direct YouTube app media play-from-search or search intent
        val playSearchIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            putExtra(SearchManager.QUERY, cleanQuery)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (tryStartActivity(playSearchIntent)) {
            return DeviceActionResult(true, "YouTube Play", "Searching & playing \"$cleanQuery\"")
        }

        val ytSearchIntent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra(SearchManager.QUERY, cleanQuery)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (tryStartActivity(ytSearchIntent)) {
            return DeviceActionResult(true, "YouTube Search", "Opened YouTube for \"$cleanQuery\"")
        }

        // Fallback to standard HTTPS YouTube search URL (works in YouTube app or browser)
        val urlIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(cleanQuery)}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (tryStartActivity(urlIntent)) {
            DeviceActionResult(true, "YouTube Search", "Launched search for \"$cleanQuery\"")
        } else {
            DeviceActionResult(false, "YouTube", "No YouTube or browser handler available on this device.")
        }
    }

    fun openAppByName(rawName: String): DeviceActionResult {
        val query = rawName.trim().lowercase()
        if (query.isEmpty()) {
            return DeviceActionResult(false, "Open App", "No app name specified.")
        }

        // Handle built-in system apps reliably
        when {
            "camera" in query || "कैमरा" in query -> {
                val camIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryStartActivity(camIntent)) {
                    return DeviceActionResult(true, "Open Camera", "Camera launched")
                }
            }
            "settings" in query || "सेटिंग" in query -> {
                val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryStartActivity(settingsIntent)) {
                    return DeviceActionResult(true, "Open Settings", "System settings launched")
                }
            }
            "calendar" in query -> {
                val calIntent = Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryStartActivity(calIntent)) {
                    return DeviceActionResult(true, "Google Calendar", "Calendar opened")
                }
            }
        }

        val pm = context.packageManager
        val apps = getInstalledAppShortcuts()
        val match = apps.firstOrNull { it.label.lowercase() == query }
            ?: apps.firstOrNull { it.label.lowercase().contains(query) }
            ?: apps.firstOrNull { query.contains(it.label.lowercase()) }

        if (match != null) {
            val launchIntent = pm.getLaunchIntentForPackage(match.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null && tryStartActivity(launchIntent)) {
                return DeviceActionResult(true, "Open ${match.label}", "Launched ${match.label}")
            }
        }

        // Known package fallbacks
        val knownPkg = when {
            "youtube" in query -> "com.google.android.youtube"
            "spotify" in query -> "com.spotify.music"
            "whatsapp" in query -> "com.whatsapp"
            "chrome" in query -> "com.android.chrome"
            "maps" in query -> "com.google.android.apps.maps"
            "gmail" in query -> "com.google.android.gm"
            else -> null
        }
        if (knownPkg != null) {
            val intent = pm.getLaunchIntentForPackage(knownPkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null && tryStartActivity(intent)) {
                return DeviceActionResult(true, "Open App", "Launched $rawName")
            }
        }

        return DeviceActionResult(false, "Open App", "\"$rawName\" is not installed on this device.")
    }

    fun toggleFlashlight(turnOn: Boolean): DeviceActionResult {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return DeviceActionResult(false, "Flashlight", "Camera service unavailable")
            val cameraId = cm.cameraIdList.firstOrNull()
                ?: return DeviceActionResult(false, "Flashlight", "No flashlight hardware detected")
            cm.setTorchMode(cameraId, turnOn)
            DeviceActionResult(
                true,
                if (turnOn) "Flashlight ON" else "Flashlight OFF",
                if (turnOn) "Torch activated" else "Torch deactivated"
            )
        } catch (e: Exception) {
            DeviceActionResult(false, "Flashlight", e.localizedMessage ?: "Flashlight unavailable on this device")
        }
    }

    private fun setAlarm(timeStr: String, label: String): DeviceActionResult {
        val parts = timeStr.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 7
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            if (label.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (tryStartActivity(intent)) {
            DeviceActionResult(true, "Set Alarm", String.format("Alarm set for %02d:%02d", hour, minute))
        } else {
            DeviceActionResult(false, "Alarm", "No Clock app available")
        }
    }

    private fun setTimer(secondsStr: String, label: String): DeviceActionResult {
        val seconds = secondsStr.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(1) ?: 60
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            if (label.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (tryStartActivity(intent)) {
            DeviceActionResult(true, "Set Timer", "Timer configured for ${seconds}s")
        } else {
            DeviceActionResult(false, "Timer", "No Timer handler available")
        }
    }

    private fun openWebSearch(query: String): DeviceActionResult {
        val url = if (query.startsWith("http://") || query.startsWith("https://")) {
            query
        } else {
            "https://www.google.com/search?q=${Uri.encode(query)}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (tryStartActivity(intent)) {
            DeviceActionResult(true, "Web Search", "Opened \"$query\"")
        } else {
            DeviceActionResult(false, "Web Search", "No browser available")
        }
    }

    private fun openMaps(destination: String): DeviceActionResult {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(destination)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (tryStartActivity(intent)) {
            DeviceActionResult(true, "Maps", "Navigating to \"$destination\"")
        } else {
            openWebSearch("maps $destination")
        }
    }

    private fun dialNumber(number: String): DeviceActionResult {
        val clean = number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (clean.isEmpty()) return DeviceActionResult(false, "Phone", "Invalid phone number")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (tryStartActivity(intent)) {
            DeviceActionResult(true, "Dialer", "Ready to call $clean")
        } else {
            DeviceActionResult(false, "Dialer", "Phone dialer unavailable")
        }
    }

    private fun openSettingsPanel(panel: String): DeviceActionResult {
        val p = panel.lowercase()
        val action = when {
            "wifi" in p && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> Settings.Panel.ACTION_WIFI
            "volume" in p && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> Settings.Panel.ACTION_VOLUME
            "internet" in p && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> Settings.Panel.ACTION_INTERNET_CONNECTIVITY
            "bluetooth" in p -> Settings.ACTION_BLUETOOTH_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return if (tryStartActivity(intent)) {
            DeviceActionResult(true, "System Panel", "Opened ${panel.ifBlank { "Settings" }}")
        } else {
            DeviceActionResult(false, "System Panel", "Could not open settings panel")
        }
    }

    suspend fun executeSkill(skill: SkillEntity?, query: String): DeviceActionResult {
        if (skill == null) {
            return DeviceActionResult(false, "Skill", "Requested skill not found")
        }
        if (!skill.isEnabled) {
            return DeviceActionResult(false, skill.name, "${skill.name} is currently disabled")
        }

        return when (skill.id) {
            "youtube" -> openYouTubeSearch(query)
            "spotify" -> {
                if (!skill.isConnected) {
                    return DeviceActionResult(false, "Spotify", "Link Spotify account in AI Core -> Skills first.")
                }
                val cleanQuery = query.trim()
                val spotifyPlay = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    setPackage("com.spotify.music")
                    putExtra(SearchManager.QUERY, cleanQuery)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryStartActivity(spotifyPlay)) {
                    DeviceActionResult(true, "Spotify Skill", "Playing \"$cleanQuery\" on Spotify")
                } else {
                    val webSpotify = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://open.spotify.com/search/${Uri.encode(cleanQuery)}")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    if (tryStartActivity(webSpotify)) {
                        DeviceActionResult(true, "Spotify Skill", "Opened Spotify search for \"$cleanQuery\"")
                    } else {
                        DeviceActionResult(false, "Spotify Skill", "Spotify app or browser unavailable")
                    }
                }
            }
            "google_calendar" -> {
                if (!skill.isConnected) {
                    return DeviceActionResult(false, "Calendar", "Link Google Calendar in AI Core -> Skills first.")
                }
                val calIntent = if (query.isNotBlank()) {
                    Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, query)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                if (tryStartActivity(calIntent)) {
                    DeviceActionResult(true, "Google Calendar", "Calendar action opened")
                } else {
                    DeviceActionResult(false, "Google Calendar", "No Calendar app available")
                }
            }
            else -> {
                if (skill.skillType == "DEEP_LINK" && skill.endpointOrUri.isNotBlank()) {
                    val uriStr = skill.endpointOrUri.replace("{query}", Uri.encode(query))
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (tryStartActivity(intent)) {
                        DeviceActionResult(true, "${skill.name} Skill", "Launched deep link")
                    } else {
                        DeviceActionResult(false, "${skill.name} Skill", "No app handles URI: $uriStr")
                    }
                } else if (skill.endpointOrUri.startsWith("http://") || skill.endpointOrUri.startsWith("https://")) {
                    executeWebhookSkill(skill, query)
                } else {
                    DeviceActionResult(
                        false,
                        skill.name,
                        "Configure a valid Webhook URL or Deep Link URI in AI Core -> Skills."
                    )
                }
            }
        }
    }

    suspend fun executeWebhookSkill(skill: SkillEntity, query: String): DeviceActionResult =
        withContext(Dispatchers.IO) {
            val rawUrl = skill.endpointOrUri.trim()
            if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                return@withContext DeviceActionResult(
                    false,
                    skill.name,
                    "Invalid webhook URL. Must start with https:// or http://"
                )
            }
            try {
                val resolvedUrl = rawUrl.replace("{query}", Uri.encode(query))
                val builder = Request.Builder().url(resolvedUrl)
                if (skill.authToken.isNotBlank()) {
                    builder.addHeader("Authorization", "Bearer ${skill.authToken.trim()}")
                }
                if (skill.httpMethod.equals("GET", ignoreCase = true)) {
                    builder.get()
                } else {
                    val payload = JSONObject().apply {
                        put("skillId", skill.id)
                        put("skillName", skill.name)
                        put("query", query)
                        put("timestamp", System.currentTimeMillis())
                    }.toString()
                    builder.post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                }
                httpClient.newCall(builder.build()).execute().use { response ->
                    val code = response.code
                    val bodyPreview = response.body?.string()?.take(180).orEmpty()
                    if (response.isSuccessful) {
                        DeviceActionResult(
                            true,
                            "${skill.name} (${code})",
                            bodyPreview.ifBlank { "HTTP $code OK" }
                        )
                    } else {
                        DeviceActionResult(
                            false,
                            "${skill.name} (${code})",
                            "Webhook returned HTTP $code"
                        )
                    }
                }
            } catch (e: Exception) {
                DeviceActionResult(false, skill.name, e.localizedMessage ?: "Webhook request failed")
            }
        }

    private fun tryStartActivity(intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}

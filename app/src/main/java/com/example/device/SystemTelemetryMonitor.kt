package com.example.device

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.roundToInt

data class SystemTelemetry(
    val fps: Int = 60,
    val frameTimeMs: Float = 16.6f,
    val memoryUsedPercent: Int = 0,
    val memoryAvailableMb: Long = 0L,
    val memoryTotalMb: Long = 0L,
    val batteryPercent: Int = 100,
    val batteryTempCelsius: Float = 0f,
    val isCharging: Boolean = false,
    val networkStatus: String = "Offline",
    val cacheSizeKb: Long = 0L,
    val systemHealthScore: Int = 92,
    val lastBoostFreedKb: Long = 0L,
    val lastBoostMessage: String? = null
)

class SystemTelemetryMonitor(private val context: Context) {

    private val _telemetry = MutableStateFlow(SystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private var lastFrameTimeNs = 0L
    private var frameCount = 0
    private var isMonitoringFps = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isMonitoringFps) return
            if (lastFrameTimeNs == 0L) {
                lastFrameTimeNs = frameTimeNanos
            } else {
                frameCount++
                val elapsedNs = frameTimeNanos - lastFrameTimeNs
                if (elapsedNs >= 500_000_000L) {
                    val seconds = elapsedNs / 1_000_000_000.0
                    val measuredFps = (frameCount / seconds).roundToInt().coerceIn(1, 240)
                    val avgFrameMs = (elapsedNs / 1_000_000.0 / frameCount).toFloat()
                    _telemetry.value = _telemetry.value.copy(
                        fps = measuredFps,
                        frameTimeMs = (avgFrameMs * 10f).roundToInt() / 10f
                    )
                    lastFrameTimeNs = frameTimeNanos
                    frameCount = 0
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun startMonitoring() {
        refreshSystemMetrics()
        if (!isMonitoringFps) {
            isMonitoringFps = true
            lastFrameTimeNs = 0L
            frameCount = 0
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    fun stopMonitoring() {
        isMonitoringFps = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun refreshSystemMetrics() {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availMb = memInfo.availMem / (1024L * 1024L)
        val usedPercent = (((totalMb - availMb).toDouble() / totalMb.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt() else 100
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300) ?: 300
        val tempCelsius = tempTenths / 10f
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val networkStatus = readNetworkStatus()
        val cacheKb = (folderSizeBytes(context.cacheDir) + folderSizeBytes(context.codeCacheDir)) / 1024L

        val healthScore = calculateHealthScore(
            memoryUsedPercent = usedPercent,
            batteryPercent = batteryPct,
            tempCelsius = tempCelsius,
            fps = _telemetry.value.fps
        )

        _telemetry.value = _telemetry.value.copy(
            memoryUsedPercent = usedPercent,
            memoryAvailableMb = availMb,
            memoryTotalMb = totalMb,
            batteryPercent = batteryPct,
            batteryTempCelsius = tempCelsius,
            isCharging = isCharging,
            networkStatus = networkStatus,
            cacheSizeKb = cacheKb,
            systemHealthScore = healthScore
        )
    }

    fun runCleanAndFpsBoost(activity: Activity?, enableBoost: Boolean): Pair<Long, String> {
        val beforeKb = (folderSizeBytes(context.cacheDir) + folderSizeBytes(context.codeCacheDir)) / 1024L
        deleteDirContents(context.cacheDir)
        System.gc()

        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val window = activity.window
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.display
                } else {
                    @Suppress("DEPRECATION")
                    activity.windowManager.defaultDisplay
                }
                if (enableBoost && display != null) {
                    val highestMode = display.supportedModes.maxByOrNull { it.refreshRate }
                    if (highestMode != null) {
                        val params = window.attributes
                        params.preferredDisplayModeId = highestMode.modeId
                        window.attributes = params
                    }
                } else {
                    val params = window.attributes
                    params.preferredDisplayModeId = 0
                    window.attributes = params
                }
            } catch (_: Exception) {
                // Display mode hint is best-effort on devices without high-refresh support.
            }
        }

        val afterKb = (folderSizeBytes(context.cacheDir) + folderSizeBytes(context.codeCacheDir)) / 1024L
        val freedKb = (beforeKb - afterKb).coerceAtLeast(0L)
        refreshSystemMetrics()

        val message = if (enableBoost) {
            "FPS Boost active • Cache purged (${freedKb} KB freed)"
        } else {
            "Standard mode restored • ${freedKb} KB cleared"
        }
        _telemetry.value = _telemetry.value.copy(
            lastBoostFreedKb = freedKb,
            lastBoostMessage = message
        )
        return freedKb to message
    }

    private fun readNetworkStatus(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Offline"
        val network = cm.activeNetwork ?: return "Offline"
        val caps = cm.getNetworkCapabilities(network) ?: return "Offline"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "5G / Mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
    }

    private fun calculateHealthScore(
        memoryUsedPercent: Int,
        batteryPercent: Int,
        tempCelsius: Float,
        fps: Int
    ): Int {
        var score = 100
        if (memoryUsedPercent > 80) score -= 18 else if (memoryUsedPercent > 65) score -= 8
        if (batteryPercent < 20) score -= 12
        if (tempCelsius > 42f) score -= 12
        if (fps < 45) score -= 10
        return score.coerceIn(35, 100)
    }

    private fun folderSizeBytes(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var sum = 0L
        dir.listFiles()?.forEach { file ->
            sum += if (file.isDirectory) folderSizeBytes(file) else file.length()
        }
        return sum
    }

    private fun deleteDirContents(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                deleteDirContents(child)
                child.delete()
            } else {
                child.delete()
            }
        }
    }
}

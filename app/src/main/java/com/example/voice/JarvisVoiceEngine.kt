package com.example.voice

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.R
import com.example.data.preferences.VoiceAccent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "JarvisVoiceEngine"

class JarvisVoiceEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var mediaPlayer: MediaPlayer? = null

    private var currentPitch = 0.78f
    private var currentSpeed = 0.95f
    private var currentAccent = VoiceAccent.HINDI_BARITONE

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isPlayingSample = MutableStateFlow(false)
    val isPlayingSample: StateFlow<Boolean> = _isPlayingSample.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                applyVoiceConfig(currentPitch, currentSpeed, currentAccent)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            } else {
                Log.e(TAG, "TextToSpeech init failed with status=$status")
            }
        }
    }

    fun applyVoiceConfig(pitch: Float, speed: Float, accent: VoiceAccent) {
        currentPitch = pitch.coerceIn(0.5f, 1.5f)
        currentSpeed = speed.coerceIn(0.6f, 1.6f)
        currentAccent = accent

        val engine = tts ?: return
        if (!isReady) return

        val targetLocale = Locale.forLanguageTag(accent.localeTag)
        val result = engine.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale("hi", "IN"))
        }

        // Prefer a deep/male voice variant if available on the device
        try {
            val matchingVoices = engine.voices?.filter {
                it.locale.language.equals(targetLocale.language, ignoreCase = true)
            }.orEmpty()
            val preferred = matchingVoices.firstOrNull {
                val lower = it.name.lowercase()
                "male" in lower || "network" in lower || "hi-in-x-hie" in lower || "hid" in lower
            } ?: matchingVoices.firstOrNull()
            if (preferred != null) {
                engine.voice = preferred
            }
        } catch (_: Exception) {
            // Fallback to default voice for locale
        }

        engine.setPitch(currentPitch)
        engine.setSpeechRate(currentSpeed)
    }

    fun speakHindi(text: String) {
        val engine = tts ?: return
        if (!isReady || text.isBlank()) return
        stopReferenceSample()

        val cleaned = text
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .trim()
        applyVoiceConfig(currentPitch, currentSpeed, currentAccent)
        _isSpeaking.value = true
        engine.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "jarvis_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
        stopReferenceSample()
    }

    fun toggleReferenceVoiceSample() {
        if (_isPlayingSample.value) {
            stopReferenceSample()
            return
        }
        stopSpeaking()
        try {
            val mp = MediaPlayer.create(context, R.raw.jarvis_voice_sample)
            if (mp != null) {
                mediaPlayer = mp
                _isPlayingSample.value = true
                mp.setOnCompletionListener {
                    _isPlayingSample.value = false
                    it.release()
                    if (mediaPlayer === it) mediaPlayer = null
                }
                mp.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play reference voice sample", e)
            _isPlayingSample.value = false
        }
    }

    private fun stopReferenceSample() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {
        }
        mediaPlayer = null
        _isPlayingSample.value = false
    }

    fun release() {
        stopReferenceSample()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

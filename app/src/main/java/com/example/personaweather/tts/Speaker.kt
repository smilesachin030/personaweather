package com.example.personaweather.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Speaker(
    context: Context,
    languageCode: String,
    private val onLanguageUnavailable: () -> Unit,
) : AutoCloseable {
    private val locale = when (languageCode) {
        "ta" -> Locale("ta", "IN")
        "hi" -> Locale("hi", "IN")
        else -> Locale("en", "IN")
    }
    private var initialized = false
    private var available = false
    private var closed = false
    private var pendingText: String? = null
    private var pendingInitStatus: Int? = null
    private var engineReference: TextToSpeech? = null

    init {
        val instance = TextToSpeech(context.applicationContext, ::onInitialized)
        synchronized(this) {
            if (closed) {
                instance.shutdown()
            } else {
                engineReference = instance
                pendingInitStatus?.let { status ->
                    pendingInitStatus = null
                    completeInitialization(status)
                }
            }
        }
    }

    @Synchronized
    private fun onInitialized(status: Int) {
        if (closed) return
        if (engineReference == null) {
            pendingInitStatus = status
            return
        }
        completeInitialization(status)
    }

    private fun completeInitialization(status: Int) {
        val instance = engineReference
        if (status != TextToSpeech.SUCCESS || instance == null) {
            initialized = true
            available = false
            onLanguageUnavailable()
            return
        }
        val availability = instance.isLanguageAvailable(locale)
        val languageResult = if (availability >= TextToSpeech.LANG_AVAILABLE) instance.setLanguage(locale) else TextToSpeech.LANG_NOT_SUPPORTED
        initialized = true
        available = availability >= TextToSpeech.LANG_AVAILABLE && languageResult >= TextToSpeech.LANG_AVAILABLE
        if (available) {
            pendingText?.let(::speakNow)
            pendingText = null
        } else {
            onLanguageUnavailable()
        }
    }

    @Synchronized
    fun speak(text: String): Boolean {
        if (!initialized) {
            pendingText = text
            return true
        }
        if (!available) return false
        speakNow(text)
        return true
    }

    private fun speakNow(text: String) {
        engineReference?.speak(text, TextToSpeech.QUEUE_FLUSH, null, SPEECH_UTTERANCE_ID)
    }

    @Synchronized
    override fun close() {
        closed = true
        pendingText = null
        engineReference?.stop()
        engineReference?.shutdown()
        engineReference = null
    }

    companion object {
        private const val SPEECH_UTTERANCE_ID = "persona-weather-summary"
    }
}

package com.kirillex.multispeakliteral.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
            }
        }
    }

    fun speak(text: String, langCode: String) {
        if (isInitialized) {
            tts?.language = Locale(langCode)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

package com.literaltranslator

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

    /**
     * Озвучивает текст на указанном языке (например, "en" или "pt")
     */
    fun speak(text: String, langCode: String) {
        if (!isInitialized || text.isBlank()) return

        // Подбираем локаль по коду языка
        val locale = Locale(langCode)
        val result = tts?.setLanguage(locale)

        if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
            // Озвучиваем текст (QUEUE_FLUSH прерывает предыдущую речь, если она шла)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    /**
     * Обязательно освобождаем ресурсы при уничтожении экрана
     */
    fun shutdown() {
        tts?.shutdown()
    }
}

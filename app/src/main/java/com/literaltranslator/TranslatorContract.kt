package com.literaltranslator

// Новое перечисление для вкладок
enum class TranslatorTab {
    TRANSLATION, DEFINITIONS, SYNONYMS
}

// Обновленный TranslatorState
data class TranslatorState(
    val inputText: String = "",
    val translatedText: String = "",
    val definitions: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(), // Добавили синонимы для полноты картины
    val sourceLanguageName: String = "English",
    val targetLanguageName: String = "Portuguese",
    val sourceLanguageCode: String = "en",
    val targetLanguageCode: String = "pt",
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // ТЕКУЩАЯ АКТИВНАЯ ВКЛАДКА (по умолчанию открыта Translation)
    val activeTab: TranslatorTab = TranslatorTab.TRANSLATION 
)

// Обновленный интерфейс интентов
sealed interface TranslatorIntent {
    data class ChangeInputText(val text: String) : TranslatorIntent
    data object ExecuteTranslation : TranslatorIntent
    data object SwapLanguages : TranslatorIntent
    data object ClearText : TranslatorIntent
    // Внутри sealed interface TranslatorIntent добавим:
    data object SpeakSourceText : TranslatorIntent
    data object SpeakTargetText : TranslatorIntent

    // ИНТЕНТ ДЛЯ КЛИКА ПО ВКЛАДКЕ
    data class SelectTab(val tab: TranslatorTab) : TranslatorIntent

    data object CopyTranslation : TranslatorIntent

    // Side Effects: one-time events (Toast, navigation, ...)
    sealed interface TranslatorEffect {
        data class ShowToast(val message: String) : TranslatorEffect
        data class ShowToastRes(val resId: Int, val args: List<Any> = emptyList()) : TranslatorEffect
        data object TriggerHapticFeedback : TranslatorEffect
        // Внутри sealed interface TranslatorEffect добавим:
        data class PlaySpeech(val text: String, val langCode: String) : TranslatorEffect
        data class CopyToClipboard(val text: String) : TranslatorEffect
    }
}

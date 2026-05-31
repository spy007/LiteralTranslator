package com.literaltranslator

data class LanguageModel(
    val code: String,
    val name: String
)

// Обновленный TranslatorState
data class TranslatorState(
    val inputText: String = "",
    val translatedText: String = "",
    val sourceLanguageName: String = "English",
    val targetLanguageName: String = "Portuguese",
    val sourceLangCode: String = "en", // Дефолт, который перезапишется при старте
    val targetLangCode: String = "es", // Испанский по умолчанию для перевода
    val isLoading: Boolean = false,
    val error: String? = null,
    
    val isSelectingSourceLang: Boolean = false,
    val isSelectingTargetLang: Boolean = false,
    val availableLanguages: List<LanguageModel> = emptyList(),
    val selectedWordRange: IntRange? = null,
    val selectedWord: String = "",
    val selectedWordTranslation: String = "",
)

// Обновленный интерфейс интентов
sealed interface TranslatorIntent {
    data object SetSourceLanguage: TranslatorIntent
    data class ChangeInputText(val text: String) : TranslatorIntent
    data object ExecuteTranslation : TranslatorIntent
    data object SwapLanguages : TranslatorIntent
    data object ClearText : TranslatorIntent
    // Внутри sealed interface TranslatorIntent добавим:
    data object SpeakSourceText : TranslatorIntent
    data object SpeakTargetText : TranslatorIntent

    data object CopyTranslation : TranslatorIntent

    object ClickSourceLanguage : TranslatorIntent

    object ClickTargetLanguage : TranslatorIntent

    data class SelectSourceLanguage(val language: LanguageModel) : TranslatorIntent

    data class SelectTargetLanguage(val language: LanguageModel) : TranslatorIntent

    object DismissLanguageSelection : TranslatorIntent

    object SwitchLanguages : TranslatorIntent

    data class SelectWord(val range: IntRange?) : TranslatorIntent
}


// Side Effects: one-time events (Toast, navigation, ...)
sealed interface TranslatorEffect {
    data class ShowToast(val message: String) : TranslatorEffect
    data class ShowToastRes(val resId: Int, val args: List<Any> = emptyList()) : TranslatorEffect
    data object TriggerHapticFeedback : TranslatorEffect
    // Внутри sealed interface TranslatorEffect добавим:
    data class PlaySpeech(val text: String, val langCode: String) : TranslatorEffect
    data class CopyToClipboard(val text: String) : TranslatorEffect
}

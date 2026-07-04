package com.literaltranslator.presentation.translator

import com.literaltranslator.domain.model.LanguageModel

// Updated TranslatorState
data class TranslatorState(
    val inputText: String = "",
    val translatedText: String = "",
    val sourceLanguageName: String = "English",
    val targetLanguageName: String = "Portuguese",
    val sourceLangCode: String = "en", // Default, which will be overwritten at startup
    val targetLangCode: String = "es", // Spanish by default for translation
    val isLoading: Boolean = false,
    val error: String? = null,
    
    val isSelectingSourceLang: Boolean = false,
    val isSelectingTargetLang: Boolean = false,
    val availableLanguages: List<LanguageModel> = emptyList(),
    val selectedWordRange: IntRange? = null,
    val selectedWord: String = "",
    val selectedWordTranslation: String = "",
    val isSelectedWordFavorite: Boolean = false,
    val isTranslationFavorite: Boolean = false
)

// Updated intents interface
sealed interface TranslatorIntent {
    data object SetSourceLanguage: TranslatorIntent
    data class ChangeInputText(val text: String) : TranslatorIntent
    data object ExecuteTranslation : TranslatorIntent
    data object SwapLanguages : TranslatorIntent
    data object ClearText : TranslatorIntent
    
    data object SpeakSourceText : TranslatorIntent
    data object SpeakTargetText : TranslatorIntent
    data object SpeakSelectedWordTranslation : TranslatorIntent

    data object CopyTranslation : TranslatorIntent

    object ClickSourceLanguage : TranslatorIntent

    object ClickTargetLanguage : TranslatorIntent

    data class SelectSourceLanguage(val language: LanguageModel) : TranslatorIntent

    data class SelectTargetLanguage(val language: LanguageModel) : TranslatorIntent

    object DismissLanguageSelection : TranslatorIntent

    object SwitchLanguages : TranslatorIntent

    data class SelectWord(val range: IntRange?) : TranslatorIntent
    data object ToggleFavoriteSelectedWord : TranslatorIntent
    data object ToggleFavoriteTranslation : TranslatorIntent
}


// Side Effects: one-time events (Toast, navigation, ...)
sealed interface TranslatorEffect {
    data class ShowToast(val message: String) : TranslatorEffect
    data class ShowToastRes(val resId: Int, val args: List<Any> = emptyList()) : TranslatorEffect
    data object TriggerHapticFeedback : TranslatorEffect
    
    data class PlaySpeech(val text: String, val langCode: String) : TranslatorEffect
    data class CopyToClipboard(val text: String) : TranslatorEffect
}

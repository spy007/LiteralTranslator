package com.literaltranslator.presentation.translator

import com.literaltranslator.domain.model.LanguageModel
import com.literaltranslator.domain.model.FavoriteModel

data class TranslationResultState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val targetLangCode: String = "es",
    val targetLanguageName: String = "Spanish",
    val translatedText: String = "",
    val isTranslationFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TranslationTabState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tabName: String = "Tab",
    val inputText: String = "",
    val sourceLangCode: String = "en",
    val sourceLanguageName: String = "English",
    val results: List<TranslationResultState> = listOf(TranslationResultState()),
    val selectedWordRange: IntRange? = null,
    val selectedWord: String = "",
    val selectedWordTranslation: String = "",
    val isSelectedWordFavorite: Boolean = false
)

// Updated TranslatorState for multi-window tabs and multi-translations
data class TranslatorState(
    val tabs: List<TranslationTabState> = listOf(TranslationTabState(tabName = "Tab 1")),
    val selectedTabId: String = tabs.firstOrNull()?.id ?: "",
    val availableLanguages: List<LanguageModel> = emptyList(),
    val isSelectingSourceLang: Boolean = false,
    val isSelectingTargetLang: Boolean = false,
    val selectingTargetId: String? = null, // To know which translation target row's language is being changed
    val favorites: List<FavoriteModel> = emptyList(),
    val isShowingFavorites: Boolean = false,
    val isRecording: Boolean = false
) {
    val currentTab: TranslationTabState?
        get() = tabs.find { it.id == selectedTabId }
}

// Updated intents interface
sealed interface TranslatorIntent {
    data object AddInputTab : TranslatorIntent
    data class RemoveInputTab(val id: String) : TranslatorIntent
    data class SelectInputTab(val id: String) : TranslatorIntent
    
    data class AddTranslationTarget(val tabId: String) : TranslatorIntent
    data class RemoveTranslationTarget(val tabId: String, val targetId: String) : TranslatorIntent

    data class ChangeInputText(val text: String) : TranslatorIntent
    data object ExecuteTranslation : TranslatorIntent
    data class SwapLanguages(val targetId: String) : TranslatorIntent
    data object ClearText : TranslatorIntent
    
    data object SpeakSourceText : TranslatorIntent
    data class SpeakTargetText(val targetId: String) : TranslatorIntent
    data object SpeakSelectedWordTranslation : TranslatorIntent

    data class CopyTranslation(val targetId: String) : TranslatorIntent
    data class ShareTranslation(val targetId: String) : TranslatorIntent

    object ClickSourceLanguage : TranslatorIntent
    data class ClickTargetLanguage(val targetId: String) : TranslatorIntent

    data class SelectSourceLanguage(val language: LanguageModel) : TranslatorIntent
    data class SelectTargetLanguage(val language: LanguageModel) : TranslatorIntent

    object DismissLanguageSelection : TranslatorIntent

    data class SelectWord(val range: IntRange?) : TranslatorIntent
    data object ToggleFavoriteSelectedWord : TranslatorIntent
    data class ToggleFavoriteTranslation(val targetId: String) : TranslatorIntent

    data object OpenFavorites : TranslatorIntent
    data object CloseFavorites : TranslatorIntent
    data class DeleteFavorite(val id: Int) : TranslatorIntent
    data class SelectFavorite(val favorite: FavoriteModel) : TranslatorIntent

    data object StartVoiceInput : TranslatorIntent
}

// Side Effects: one-time events (Toast, navigation, ...)
sealed interface TranslatorEffect {
    data class ShowToast(val message: String) : TranslatorEffect
    data class ShowToastRes(val resId: Int, val args: List<Any> = emptyList()) : TranslatorEffect
    data object TriggerHapticFeedback : TranslatorEffect
    
    data class PlaySpeech(val text: String, val langCode: String) : TranslatorEffect
    data class CopyToClipboard(val text: String) : TranslatorEffect
    data class ShareText(val text: String) : TranslatorEffect
    data class OpenVoiceRecognition(val langCode: String) : TranslatorEffect
}

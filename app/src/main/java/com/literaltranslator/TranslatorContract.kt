package com.literaltranslator

// Новое перечисление для вкладок
enum class TranslatorTab {
    TRANSLATION, DEFINITIONS, SYNONYMS
}

data class LanguageModel(
    val code: String,
    val name: String
)

// Обновленный TranslatorState
data class TranslatorState(
    val inputText: String = "",
    val translatedText: String = "",
    val definitions: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(), // Добавили синонимы для полноты картины
    val sourceLanguageName: String = "English",
    val targetLanguageName: String = "Portuguese",
    val sourceLangCode: String = "en", // Дефолт, который перезапишется при старте
    val targetLangCode: String = "es", // Испанский по умолчанию для перевода
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // ТЕКУЩАЯ АКТИВНАЯ ВКЛАДКА (по умолчанию открыта Translation)
    val activeTab: TranslatorTab = TranslatorTab.TRANSLATION,

    // Флаги для открытия окон выбора
    val isSelectingSourceLang: Boolean = false,
    val isSelectingTargetLang: Boolean = false,
    val availableLanguages: List<LanguageModel> = emptyList(),
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

    // ИНТЕНТ ДЛЯ КЛИКА ПО ВКЛАДКЕ
    data class SelectTab(val tab: TranslatorTab) : TranslatorIntent

    data object CopyTranslation : TranslatorIntent

    object ClickSourceLanguage : TranslatorIntent

    object ClickTargetLanguage : TranslatorIntent

    data class SelectSourceLanguage(val language: LanguageModel) : TranslatorIntent

    data class SelectTargetLanguage(val language: LanguageModel) : TranslatorIntent

    object DismissLanguageSelection : TranslatorIntent

    object SwitchLanguages : TranslatorIntent
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

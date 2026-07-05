package com.literaltranslator.presentation.translator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.literaltranslator.domain.repository.TranslationRepository
import com.literaltranslator.utils.getLanguageName
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import com.literaltranslator.R

@OptIn(FlowPreview::class) // Required for debounce operator
class TranslatorViewModel(
    private val repository: TranslationRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow(TranslatorState())
    val viewState: StateFlow<TranslatorState> = _viewState.asStateFlow()

    private val _effect = MutableSharedFlow<TranslatorEffect>()
    val effect: SharedFlow<TranslatorEffect> = _effect.asSharedFlow()

    // Special stream to track user input changes
    private val _inputTextFlow = MutableStateFlow("")

    init {
        // Start reactive listening for text input
        _inputTextFlow
            .debounce(600) // Wait 600 ms after the user finishes typing
            .filter { text -> text.isNotBlank() } // Ignore empty strings
            .distinctUntilChanged() // Do not re-translate if the text hasn't changed
            .onEach {
                triggerTranslation() // Trigger translation automatically!
            }
            .launchIn(viewModelScope)

        // Initialize languages when creating ViewModel
        initializeDefaultLanguages()
        // Immediately load supported languages from ML Kit in the background
        loadAvailableLanguages()

        // Observe favorites from database
        repository.getAllFavorites()
            .onEach { favs ->
                _viewState.update { it.copy(favorites = favs) }
            }
            .launchIn(viewModelScope)
    }

    private fun initializeDefaultLanguages() {
        // Get the system language of the phone (e.g., "ru", "en", "pt")
        val systemLangCode = Locale.getDefault().language

        _viewState.update { currentState ->
            val sourceName = getLanguageName(systemLangCode)
            val targetLangCode = if (systemLangCode == "es") "en" else "es"
            val targetName = getLanguageName(targetLangCode)

            currentState.copy(
                sourceLangCode = systemLangCode,
                sourceLanguageName = sourceName,
                targetLangCode = targetLangCode,
                targetLanguageName = targetName
            )
        }
    }

    private fun loadAvailableLanguages() {
        viewModelScope.launch {
            repository.getAvailableLanguages().onSuccess { languages ->
                _viewState.update { it.copy(availableLanguages = languages) }
            }
        }
    }

    fun handleIntent(intent: TranslatorIntent) {
        when (intent) {
            is TranslatorIntent.SetSourceLanguage -> {
                val sourceLanguageName = Locale(Locale.getDefault().language).getDisplayName(Locale.getDefault())
                _viewState.update {
                    it.copy(
                        sourceLanguageName = sourceLanguageName,
                        targetLanguageName = sourceLanguageName,
                        isSelectingSourceLang = false
                    )
                }
            }

            TranslatorIntent.ExecuteTranslation -> {
                triggerTranslation()
            }

            TranslatorIntent.SwapLanguages -> {
                _viewState.update { currentState ->
                    currentState.copy(
                        sourceLanguageName = currentState.targetLanguageName,
                        targetLanguageName = currentState.sourceLanguageName,
                        sourceLangCode = currentState.targetLangCode,
                        targetLangCode = currentState.sourceLangCode,
                        inputText = currentState.translatedText,
                        translatedText = currentState.inputText,
                        selectedWordRange = null
                    )
                }
                // Update the input text flow to match the new source text
                _inputTextFlow.value = _viewState.value.inputText
                // Trigger translation immediately with the new language configuration
                triggerTranslation()

                viewModelScope.launch {
                    _effect.emit(TranslatorEffect.TriggerHapticFeedback)
                }
            }

            TranslatorIntent.ClearText -> {
                _inputTextFlow.value = ""
                _viewState.update {
                    it.copy(
                        inputText = "",
                        translatedText = "",
                        selectedWordRange = null,
                        isTranslationFavorite = false
                    )
                }
            }

            TranslatorIntent.SpeakSourceText -> {
                val text = _viewState.value.inputText
                val lang = _viewState.value.sourceLangCode
                if (text.isNotBlank()) {
                    viewModelScope.launch { _effect.emit(TranslatorEffect.PlaySpeech(text, lang)) }
                }
            }

            TranslatorIntent.SpeakTargetText -> {
                val text = _viewState.value.translatedText
                val lang = _viewState.value.targetLangCode
                if (text.isNotBlank()) {
                    viewModelScope.launch { _effect.emit(TranslatorEffect.PlaySpeech(text, lang)) }
                }
            }

            TranslatorIntent.SpeakSelectedWordTranslation -> {
                val text = _viewState.value.selectedWordTranslation
                val lang = _viewState.value.targetLangCode
                if (text.isNotBlank()) {
                    viewModelScope.launch { _effect.emit(TranslatorEffect.PlaySpeech(text, lang)) }
                }
            }

            TranslatorIntent.CopyTranslation -> {
                val text = _viewState.value.translatedText
                if (text.isNotBlank()) {
                    viewModelScope.launch {
                        _effect.emit(TranslatorEffect.CopyToClipboard(text))
                        _effect.emit(TranslatorEffect.ShowToastRes(R.string.copied_to_clipboard))
                    }
                }
            }

            TranslatorIntent.ShareTranslation -> {
                val text = _viewState.value.translatedText
                if (text.isNotBlank()) {
                    viewModelScope.launch {
                        _effect.emit(TranslatorEffect.ShareText(text))
                    }
                }
            }

            is TranslatorIntent.ClickSourceLanguage -> {
                _viewState.update { it.copy(isSelectingSourceLang = true) }
            }

            is TranslatorIntent.ClickTargetLanguage -> {
                _viewState.update { it.copy(isSelectingTargetLang = true) }
            }

            is TranslatorIntent.DismissLanguageSelection -> {
                _viewState.update {
                    it.copy(
                        isSelectingSourceLang = false,
                        isSelectingTargetLang = false
                    )
                }
            }

            is TranslatorIntent.SelectSourceLanguage -> {
                _viewState.update {
                    it.copy(
                        sourceLanguageName = intent.language.name,
                        sourceLangCode = intent.language.code,
                        isSelectingSourceLang = false
                    )
                }
                triggerTranslation()
            }

            is TranslatorIntent.SelectTargetLanguage -> {
                _viewState.update {
                    it.copy(
                        targetLanguageName = intent.language.name,
                        targetLangCode = intent.language.code,
                        isSelectingTargetLang = false
                    )
                }
                triggerTranslation()
            }

            is TranslatorIntent.SwitchLanguages -> {
                _viewState.update {
                    it.copy(
                        sourceLanguageName = it.targetLanguageName,
                        targetLanguageName = it.sourceLanguageName,
                        sourceLangCode = it.targetLangCode,
                        targetLangCode = it.sourceLangCode,
                        selectedWordRange = null
                    )
                }
                triggerTranslation()
            }

            is TranslatorIntent.SelectWord -> {
                val range = intent.range
                if (range != null && range.first in _viewState.value.inputText.indices) {
                    val word = _viewState.value.inputText.substring(range)
                    _viewState.update {
                        it.copy(
                            selectedWordRange = range,
                            selectedWord = word,
                            selectedWordTranslation = "", // Clear previous translation
                            isSelectedWordFavorite = false
                        )
                    }
                    triggerWordTranslation(word)
                } else {
                    _viewState.update { 
                        it.copy(
                            selectedWordRange = null, 
                            selectedWordTranslation = "",
                            isSelectedWordFavorite = false
                        ) 
                    }
                }
            }

            TranslatorIntent.ToggleFavoriteSelectedWord -> {
                val state = _viewState.value
                if (state.selectedWord.isNotBlank() && state.selectedWordTranslation.isNotBlank()) {
                    viewModelScope.launch {
                        repository.toggleFavorite(
                            word = state.selectedWord,
                            translation = state.selectedWordTranslation,
                            sourceLang = state.sourceLangCode,
                            targetLang = state.targetLangCode
                        )
                        val isFav = repository.isFavorite(
                            state.selectedWord,
                            state.sourceLangCode,
                            state.targetLangCode
                        )
                        _viewState.update { it.copy(isSelectedWordFavorite = isFav) }
                    }
                }
            }

            TranslatorIntent.ToggleFavoriteTranslation -> {
                val state = _viewState.value
                if (state.inputText.isNotBlank() && state.translatedText.isNotBlank()) {
                    viewModelScope.launch {
                        repository.toggleFavorite(
                            word = state.inputText,
                            translation = state.translatedText,
                            sourceLang = state.sourceLangCode,
                            targetLang = state.targetLangCode
                        )
                        val isFav = repository.isFavorite(
                            state.inputText,
                            state.sourceLangCode,
                            state.targetLangCode
                        )
                        _viewState.update { it.copy(isTranslationFavorite = isFav) }
                    }
                }
            }

            is TranslatorIntent.ChangeInputText -> {
                // Update the text in UI immediately (so characters appear without lag)
                _viewState.update { it.copy(inputText = intent.text, selectedWordRange = null) }
                // And send the text to the auto-translation stream
                _inputTextFlow.value = intent.text
            }

            TranslatorIntent.OpenFavorites -> {
                _viewState.update { it.copy(isShowingFavorites = true) }
            }

            TranslatorIntent.CloseFavorites -> {
                _viewState.update { it.copy(isShowingFavorites = false) }
            }

            is TranslatorIntent.DeleteFavorite -> {
                viewModelScope.launch {
                    repository.deleteFavoriteById(intent.id)
                }
            }

            is TranslatorIntent.SelectFavorite -> {
                _viewState.update {
                    it.copy(
                        inputText = intent.favorite.word,
                        translatedText = intent.favorite.translation,
                        sourceLangCode = intent.favorite.sourceLangCode,
                        targetLangCode = intent.favorite.targetLangCode,
                        sourceLanguageName = getLanguageName(intent.favorite.sourceLangCode),
                        targetLanguageName = getLanguageName(intent.favorite.targetLangCode),
                        isShowingFavorites = false,
                        isTranslationFavorite = true
                    )
                }
                _inputTextFlow.value = intent.favorite.word
            }

            TranslatorIntent.StartVoiceInput -> {
                viewModelScope.launch {
                    _effect.emit(TranslatorEffect.OpenVoiceRecognition(_viewState.value.sourceLangCode))
                }
            }
        }
    }

    private fun triggerWordTranslation(word: String) {
        if (word.isBlank()) return
        val currentState = _viewState.value

        viewModelScope.launch {
            val result = repository.translateText(
                word,
                currentState.sourceLangCode,
                currentState.targetLangCode
            )

            result.onSuccess { translation ->
                val isFav = repository.isFavorite(
                    word,
                    currentState.sourceLangCode,
                    currentState.targetLangCode
                )
                _viewState.update { 
                    it.copy(
                        selectedWordTranslation = translation,
                        isSelectedWordFavorite = isFav
                    ) 
                }
            }
        }
    }

    private fun triggerTranslation() {
        val currentState = _viewState.value
        val textToTranslate = currentState.inputText

        if (textToTranslate.isBlank()) return

        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true, error = null, isTranslationFavorite = false) }

            val result = repository.translateText(
                textToTranslate,
                currentState.sourceLangCode,
                currentState.targetLangCode
            )

            result.onSuccess { translationResult ->
                val isFav = repository.isFavorite(
                    textToTranslate,
                    currentState.sourceLangCode,
                    currentState.targetLangCode
                )
                _viewState.update {
                    it.copy(
                        translatedText = translationResult,
                        isLoading = false,
                        isTranslationFavorite = isFav
                    )
                }
            }.onFailure { exception ->
                _viewState.update { it.copy(isLoading = false, error = exception.localizedMessage) }
                _effect.emit(
                    TranslatorEffect.ShowToastRes(
                        R.string.error_prefix,
                        listOf(exception.localizedMessage ?: "Unknown")
                    )
                )
            }
        }
    }
}

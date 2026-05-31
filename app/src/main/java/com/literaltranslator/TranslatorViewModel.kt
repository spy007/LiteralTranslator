package com.literaltranslator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.literaltranslator.utils.getLanguageName
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(FlowPreview::class) // Требуется для использования оператора debounce
class TranslatorViewModel(
    private val repository: TranslationRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow(TranslatorState())
    val viewState: StateFlow<TranslatorState> = _viewState.asStateFlow()

    private val _effect = MutableSharedFlow<TranslatorEffect>()
    val effect: SharedFlow<TranslatorEffect> = _effect.asSharedFlow()

    // Специальный поток для отслеживания изменений ввода текста пользователем
    private val _inputTextFlow = MutableStateFlow("")

    init {
        // Запускаем реактивное прослушивание ввода текста
        _inputTextFlow
            .debounce(600) // Ждем 600 мс после окончания ввода текста пользователем
            .filter { text -> text.isNotBlank() } // Игнорируем пустые строки
            .distinctUntilChanged() // Не переводим повторно, если текст не изменился
            .onEach {
                triggerTranslation() // Запускаем перевод автоматически!
            }
            .launchIn(viewModelScope)

        // Инициализируем языки при создании ViewModel
        initializeDefaultLanguages()
        // Сразу фоном загружаем поддерживаемые языки из ML Kit
        loadAvailableLanguages()
    }

    private fun initializeDefaultLanguages() {
        // Получаем системный язык телефона (например, "ru", "en", "pt")
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
            is TranslatorIntent.ChangeInputText -> {
                // И мгновенно обновляем текст в UI (чтобы буквы появлялись без задержек)
                _viewState.update { it.copy(inputText = intent.text, selectedWordRange = null) }
                // И отправляем текст в поток автоперевода
                _inputTextFlow.value = intent.text
            }

            TranslatorIntent.SetSourceLanguage -> {
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
                        inputText = currentState.translatedText,
                        translatedText = currentState.inputText,
                        selectedWordRange = null
                    ).also { triggerTranslation() }
                }
                viewModelScope.launch {
                    _effect.emit(TranslatorEffect.TriggerHapticFeedback)
                }
            }

            TranslatorIntent.ClearText -> {
                _inputTextFlow.value = ""
                _viewState.update {
                    it.copy(inputText = "", translatedText = "", definitions = emptyList(), selectedWordRange = null)
                }
            }

            is TranslatorIntent.SelectTab -> {
                _viewState.update { it.copy(activeTab = intent.tab) }
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

            TranslatorIntent.CopyTranslation -> {
                val text = _viewState.value.translatedText
                if (text.isNotBlank()) {
                    viewModelScope.launch {
                        _effect.emit(TranslatorEffect.CopyToClipboard(text))
                        _effect.emit(TranslatorEffect.ShowToastRes(R.string.copied_to_clipboard))
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
                            selectedWord = word
                        )
                    }
                    triggerWordTranslation(word)
                } else {
                    _viewState.update { it.copy(selectedWordRange = null) }
                }
            }
        }
    }

    private fun triggerWordTranslation(word: String) {
        if (word.isBlank()) return
        val currentState = _viewState.value

        viewModelScope.launch {
            val result = repository.translateText(
                text = word,
                sourceLangCode = currentState.sourceLangCode,
                targetLangCode = currentState.targetLangCode
            )

            result.onSuccess { translation ->
                _viewState.update { it.copy(selectedWordTranslation = translation) }
            }
        }
    }

    private fun triggerTranslation() {
        val currentState = _viewState.value
        val textToTranslate = currentState.inputText

        if (textToTranslate.isBlank()) return

        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true, error = null) }

            val result = repository.translateText(
                text = textToTranslate,
                sourceLangCode = currentState.sourceLangCode,
                targetLangCode = currentState.targetLangCode
            )

            result.onSuccess { translationResult ->
                _viewState.update {
                    it.copy(
                        translatedText = translationResult,
                        definitions = listOf(
                            "a feeling of expectation and desire for a certain thing to happen.",
                            "a feeling of trust."
                        ),
                        isLoading = false
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
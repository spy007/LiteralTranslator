package com.literaltranslator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class) // Требуется для использования оператора debounce
class TranslatorViewModel(
    private val repository: TranslationRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow(TranslatorState())
    val viewState: StateFlow<TranslatorState> = _viewState.asStateFlow()

    private val _effect = MutableSharedFlow<TranslatorIntent.TranslatorEffect>()
    val effect: SharedFlow<TranslatorIntent.TranslatorEffect> = _effect.asSharedFlow()

    // Специальный поток для отслеживания изменений ввода текста пользователем
    private val _inputTextFlow = MutableStateFlow("")

    init {
        // Запускаем реактивное прослушивание ввода текста
        _inputTextFlow
            .debounce(600) // Ждем 600 мс после окончания ввода текста пользователем
            .filter { text -> text.isNotBlank() } // Игнорируем пустые строки
            .distinctUntilChanged() // Не переводим повторно, если текст не изменился
            .onEach {
                performRealTranslation() // Запускаем перевод автоматически!
            }
            .launchIn(viewModelScope)
    }

    fun handleIntent(intent: TranslatorIntent) {
        when (intent) {
            is TranslatorIntent.ChangeInputText -> {
                // И мгновенно обновляем текст в UI (чтобы буквы появлялись без задержек)
                _viewState.update { it.copy(inputText = intent.text) }
                // И отправляем текст в поток автоперевода
                _inputTextFlow.value = intent.text
            }
            TranslatorIntent.ExecuteTranslation -> {
                performRealTranslation()
            }
            TranslatorIntent.SwapLanguages -> {
                _viewState.update { currentState ->
                    val updatedState = currentState.copy(
                        sourceLanguageName = currentState.targetLanguageName,
                        targetLanguageName = currentState.sourceLanguageName,
                        sourceLanguageCode = currentState.targetLanguageCode,
                        targetLanguageCode = currentState.sourceLanguageCode,
                        inputText = currentState.translatedText,
                        translatedText = currentState.inputText
                    )
                    // Обновляем поток ввода новым текстом
                    _inputTextFlow.value = updatedState.inputText
                    updatedState
                }
                viewModelScope.launch {
                    _effect.emit(TranslatorIntent.TranslatorEffect.TriggerHapticFeedback)
                }
            }
            TranslatorIntent.ClearText -> {
                _inputTextFlow.value = ""
                _viewState.update {
                    it.copy(inputText = "", translatedText = "", definitions = emptyList())
                }
            }
            is TranslatorIntent.SelectTab -> {
                _viewState.update { it.copy(activeTab = intent.tab) }
            }
            TranslatorIntent.SpeakSourceText -> {
                val text = _viewState.value.inputText
                val lang = _viewState.value.sourceLanguageCode
                if (text.isNotBlank()) {
                    viewModelScope.launch { _effect.emit(TranslatorIntent.TranslatorEffect.PlaySpeech(text, lang)) }
                }
            }
            TranslatorIntent.SpeakTargetText -> {
                val text = _viewState.value.translatedText
                val lang = _viewState.value.targetLanguageCode
                if (text.isNotBlank()) {
                    viewModelScope.launch { _effect.emit(TranslatorIntent.TranslatorEffect.PlaySpeech(text, lang)) }
                }
            }
        }
    }

    private fun performRealTranslation() {
        val currentState = _viewState.value
        val textToTranslate = currentState.inputText

        if (textToTranslate.isBlank()) return

        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true, error = null) }

            val result = repository.translateText(
                text = textToTranslate,
                sourceLangCode = currentState.sourceLanguageCode,
                targetLangCode = currentState.targetLanguageCode
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
                _effect.emit(TranslatorIntent.TranslatorEffect.ShowToast("Ошибка: ${exception.localizedMessage}"))
            }
        }
    }

    // Модифицируем метод успешного перевода, чтобы он сразу генерировал и синонимы для теста
    // (В реальном приложении эти данные берутся из Dictionary API)
    private fun mockSuccessData(translationResult: String) {
        _viewState.update {
            it.copy(
                translatedText = translationResult,
                definitions = listOf(
                    "a feeling of expectation and desire for a certain thing to happen.",
                    "a feeling of trust."
                ),
                synonyms = listOf("aspiration", "desire", "wish", "expectation", "faith"),
                isLoading = false
            )
        }
    }
}
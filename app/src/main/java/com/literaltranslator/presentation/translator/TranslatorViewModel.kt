package com.literaltranslator.presentation.translator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.literaltranslator.domain.repository.TranslationRepository
import com.literaltranslator.domain.model.LanguageModel
import com.literaltranslator.domain.model.FavoriteModel
import com.literaltranslator.utils.getLanguageName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import com.literaltranslator.R

class TranslatorViewModel(
    private val repository: TranslationRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow(TranslatorState())
    val viewState: StateFlow<TranslatorState> = _viewState.asStateFlow()

    private val _effect = MutableSharedFlow<TranslatorEffect>()
    val effect: SharedFlow<TranslatorEffect> = _effect.asSharedFlow()

    private val translationJobs = mutableMapOf<String, Job>()
    private var tabCounter = 1

    init {
        initializeDefaultLanguages()
        loadAvailableLanguages()

        // Observe favorites from database
        repository.getAllFavorites()
            .onEach { favs ->
                _viewState.update { it.copy(favorites = favs) }
                // Refresh favorite statuses for current tabs/targets
                refreshFavoritesStatus()
            }
            .launchIn(viewModelScope)
    }

    private fun initializeDefaultLanguages() {
        val systemLangCode = Locale.getDefault().language
        val sourceName = getLanguageName(systemLangCode)
        val targetLangCode = if (systemLangCode == "es") "en" else "es"
        val targetName = getLanguageName(targetLangCode)

        _viewState.update { currentState ->
            val initialTab = TranslationTabState(
                tabName = systemLangCode.uppercase(Locale.ROOT),
                sourceLangCode = systemLangCode,
                sourceLanguageName = sourceName,
                results = listOf(
                    TranslationResultState(
                        targetLangCode = targetLangCode,
                        targetLanguageName = targetName
                    )
                )
            )
            tabCounter = 1
            currentState.copy(
                tabs = listOf(initialTab),
                selectedTabId = initialTab.id
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
            TranslatorIntent.AddInputTab -> {
                val systemLangCode = Locale.getDefault().language
                val sourceName = getLanguageName(systemLangCode)
                val targetLangCode = if (systemLangCode == "es") "en" else "es"
                val targetName = getLanguageName(targetLangCode)

                _viewState.update { currentState ->
                    val newTab = TranslationTabState(
                        tabName = systemLangCode.uppercase(Locale.ROOT),
                        sourceLangCode = systemLangCode,
                        sourceLanguageName = sourceName,
                        results = listOf(
                            TranslationResultState(
                                targetLangCode = targetLangCode,
                                targetLanguageName = targetName
                            )
                        )
                    )
                    val updatedTabs = currentState.tabs + newTab
                    currentState.copy(tabs = updatedTabs, selectedTabId = newTab.id)
                }
            }

            is TranslatorIntent.RemoveInputTab -> {
                _viewState.update { currentState ->
                    val updatedTabs = currentState.tabs.filter { it.id != intent.id }
                    val finalTabs = if (updatedTabs.isEmpty()) {
                        val systemLangCode = Locale.getDefault().language
                        listOf(
                            TranslationTabState(
                                tabName = systemLangCode.uppercase(Locale.ROOT),
                                sourceLangCode = systemLangCode,
                                sourceLanguageName = getLanguageName(systemLangCode),
                                results = listOf(
                                    TranslationResultState(
                                        targetLangCode = if (systemLangCode == "es") "en" else "es",
                                        targetLanguageName = getLanguageName(if (systemLangCode == "es") "en" else "es")
                                    )
                                )
                            )
                        )
                    } else updatedTabs

                    val newSelectedId = if (currentState.selectedTabId == intent.id) {
                        finalTabs.first().id
                    } else {
                        currentState.selectedTabId
                    }

                    currentState.copy(tabs = finalTabs, selectedTabId = newSelectedId)
                }
                translationJobs[intent.id]?.cancel()
                translationJobs.remove(intent.id)
            }

            is TranslatorIntent.SelectInputTab -> {
                _viewState.update { it.copy(selectedTabId = intent.id) }
            }

            is TranslatorIntent.AddTranslationTarget -> {
                _viewState.update { currentState ->
                    val updatedTabs = currentState.tabs.map { tab ->
                        if (tab.id == intent.tabId) {
                            // Find a language that isn't already used as a target, or default to English/Spanish
                            val existingCodes = tab.results.map { it.targetLangCode }
                            val nextLang = currentState.availableLanguages.firstOrNull { it.code !in existingCodes && it.code != tab.sourceLangCode }
                                ?: LanguageModel("fr", "French")

                            val newResult = TranslationResultState(
                                targetLangCode = nextLang.code,
                                targetLanguageName = nextLang.name
                            )
                            tab.copy(results = tab.results + newResult)
                        } else tab
                    }
                    currentState.copy(tabs = updatedTabs)
                }
                triggerTranslation(intent.tabId)
            }

            is TranslatorIntent.RemoveTranslationTarget -> {
                _viewState.update { currentState ->
                    val updatedTabs = currentState.tabs.map { tab ->
                        if (tab.id == intent.tabId) {
                            val updatedResults = tab.results.filter { it.id != intent.targetId }
                            val finalResults = if (updatedResults.isEmpty()) {
                                listOf(TranslationResultState())
                            } else updatedResults
                            tab.copy(results = finalResults)
                        } else tab
                    }
                    currentState.copy(tabs = updatedTabs)
                }
            }

            is TranslatorIntent.ChangeInputText -> {
                val currentTabId = _viewState.value.selectedTabId
                _viewState.update { currentState ->
                    val updatedTabs = currentState.tabs.map { tab ->
                        if (tab.id == currentTabId) {
                            tab.copy(inputText = intent.text, selectedWordRange = null)
                        } else tab
                    }
                    currentState.copy(tabs = updatedTabs)
                }
                launchAutoTranslation(currentTabId)
            }

            TranslatorIntent.ExecuteTranslation -> {
                triggerTranslation(_viewState.value.selectedTabId)
            }

            is TranslatorIntent.SwapLanguages -> {
                val currentTabId = _viewState.value.selectedTabId
                _viewState.update { currentState ->
                    val updatedTabs = currentState.tabs.map { tab ->
                        if (tab.id == currentTabId) {
                            val target = tab.results.find { it.id == intent.targetId }
                            if (target != null) {
                                val newSourceCode = target.targetLangCode
                                val newSourceName = target.targetLanguageName
                                val newTargetCode = tab.sourceLangCode
                                val newTargetName = tab.sourceLanguageName
                                val newInputText = target.translatedText

                                val updatedResults = tab.results.map { res ->
                                    if (res.id == intent.targetId) {
                                        res.copy(
                                            targetLangCode = newTargetCode,
                                            targetLanguageName = newTargetName,
                                            translatedText = tab.inputText
                                        )
                                    } else res
                                }

                                tab.copy(
                                    sourceLangCode = newSourceCode,
                                    sourceLanguageName = newSourceName,
                                    inputText = newInputText,
                                    results = updatedResults,
                                    selectedWordRange = null
                                )
                            } else tab
                        } else tab
                    }
                    currentState.copy(tabs = updatedTabs)
                }

                triggerTranslation(currentTabId)
                viewModelScope.launch {
                    _effect.emit(TranslatorEffect.TriggerHapticFeedback)
                }
            }

            TranslatorIntent.ClearText -> {
                val currentTabId = _viewState.value.selectedTabId
                _viewState.update { currentState ->
                    val updatedTabs = currentState.tabs.map { tab ->
                        if (tab.id == currentTabId) {
                            tab.copy(
                                inputText = "",
                                selectedWordRange = null,
                                results = tab.results.map { it.copy(translatedText = "", isTranslationFavorite = false) }
                            )
                        } else tab
                    }
                    currentState.copy(tabs = updatedTabs)
                }
                translationJobs[currentTabId]?.cancel()
            }

            TranslatorIntent.SpeakSourceText -> {
                val tab = _viewState.value.currentTab
                if (tab != null && tab.inputText.isNotBlank()) {
                    viewModelScope.launch {
                        _effect.emit(TranslatorEffect.PlaySpeech(tab.inputText, tab.sourceLangCode))
                    }
                }
            }

            is TranslatorIntent.SpeakTargetText -> {
                val tab = _viewState.value.currentTab
                val target = tab?.results?.find { it.id == intent.targetId }
                if (target != null && target.translatedText.isNotBlank()) {
                    viewModelScope.launch {
                        _effect.emit(TranslatorEffect.PlaySpeech(target.translatedText, target.targetLangCode))
                    }
                }
            }

            TranslatorIntent.SpeakSelectedWordTranslation -> {
                val tab = _viewState.value.currentTab
                if (tab != null && tab.selectedWordTranslation.isNotBlank()) {
                    viewModelScope.launch {
                        _effect.emit(TranslatorEffect.PlaySpeech(tab.selectedWordTranslation, tab.results.firstOrNull()?.targetLangCode ?: "en"))
                    }
                }
            }

            is TranslatorIntent.CopyTranslation -> {
                val tab = _viewState.value.currentTab
                val target = tab?.results?.find { it.id == intent.targetId }
                if (target != null && target.translatedText.isNotBlank()) {
                    viewModelScope.launch {
                        _effect.emit(TranslatorEffect.CopyToClipboard(target.translatedText))
                        _effect.emit(TranslatorEffect.ShowToastRes(R.string.copied_to_clipboard))
                    }
                }
            }

            is TranslatorIntent.ShareTranslation -> {
                val tab = _viewState.value.currentTab
                val target = tab?.results?.find { it.id == intent.targetId }
                if (target != null && target.translatedText.isNotBlank()) {
                    viewModelScope.launch {
                        _effect.emit(TranslatorEffect.ShareText(target.translatedText))
                    }
                }
            }

            TranslatorIntent.ClickSourceLanguage -> {
                _viewState.update { it.copy(isSelectingSourceLang = true, isSelectingTargetLang = false, selectingTargetId = null) }
            }

            is TranslatorIntent.ClickTargetLanguage -> {
                _viewState.update { it.copy(isSelectingTargetLang = true, isSelectingSourceLang = false, selectingTargetId = intent.targetId) }
            }

            TranslatorIntent.DismissLanguageSelection -> {
                _viewState.update {
                    it.copy(
                        isSelectingSourceLang = false,
                        isSelectingTargetLang = false,
                        selectingTargetId = null
                    )
                }
            }

            is TranslatorIntent.SelectSourceLanguage -> {
                val currentTabId = _viewState.value.selectedTabId
                _viewState.update { currentState ->
                    val updatedTabs = currentState.tabs.map { tab ->
                        if (tab.id == currentTabId) {
                            tab.copy(
                                tabName = intent.language.code.uppercase(Locale.ROOT),
                                sourceLanguageName = intent.language.name,
                                sourceLangCode = intent.language.code
                            )
                        } else tab
                    }
                    currentState.copy(tabs = updatedTabs, isSelectingSourceLang = false)
                }
                triggerTranslation(currentTabId)
            }

            is TranslatorIntent.SelectTargetLanguage -> {
                val currentTabId = _viewState.value.selectedTabId
                val targetId = _viewState.value.selectingTargetId
                _viewState.update { currentState ->
                    val updatedTabs = currentState.tabs.map { tab ->
                        if (tab.id == currentTabId) {
                            val updatedResults = tab.results.map { res ->
                                if (res.id == targetId) {
                                    res.copy(
                                        targetLanguageName = intent.language.name,
                                        targetLangCode = intent.language.code
                                    )
                                } else res
                              }
                            tab.copy(results = updatedResults)
                        } else tab
                    }
                    currentState.copy(tabs = updatedTabs, isSelectingTargetLang = false, selectingTargetId = null)
                }
                triggerTranslation(currentTabId)
            }

            is TranslatorIntent.SelectWord -> {
                val currentTabId = _viewState.value.selectedTabId
                val range = intent.range
                _viewState.update { currentState ->
                    val updatedTabs = currentState.tabs.map { tab ->
                        if (tab.id == currentTabId) {
                            if (range != null && range.first in tab.inputText.indices) {
                                val word = tab.inputText.substring(range)
                                tab.copy(
                                    selectedWordRange = range,
                                    selectedWord = word,
                                    selectedWordTranslation = "",
                                    isSelectedWordFavorite = false
                                )
                            } else {
                                tab.copy(
                                    selectedWordRange = null,
                                    selectedWord = "",
                                    selectedWordTranslation = "",
                                    isSelectedWordFavorite = false
                                )
                            }
                        } else tab
                    }
                    currentState.copy(tabs = updatedTabs)
                }
                
                val updatedTab = _viewState.value.tabs.find { it.id == currentTabId }
                if (updatedTab != null && updatedTab.selectedWord.isNotBlank()) {
                    triggerWordTranslation(currentTabId, updatedTab.selectedWord)
                }
            }

            TranslatorIntent.ToggleFavoriteSelectedWord -> {
                val currentTabId = _viewState.value.selectedTabId
                val tab = _viewState.value.tabs.find { it.id == currentTabId } ?: return
                if (tab.selectedWord.isNotBlank() && tab.selectedWordTranslation.isNotBlank()) {
                    viewModelScope.launch {
                        val firstTarget = tab.results.firstOrNull() ?: return@launch
                        repository.toggleFavorite(
                            word = tab.selectedWord,
                            translation = tab.selectedWordTranslation,
                            sourceLang = tab.sourceLangCode,
                            targetLang = firstTarget.targetLangCode
                        )
                        val isFav = repository.isFavorite(
                            tab.selectedWord,
                            tab.sourceLangCode,
                            firstTarget.targetLangCode
                        )
                        _viewState.update { currentState ->
                            currentState.copy(tabs = currentState.tabs.map { 
                                if (it.id == currentTabId) it.copy(isSelectedWordFavorite = isFav) else it
                            })
                        }
                    }
                }
            }

            is TranslatorIntent.ToggleFavoriteTranslation -> {
                val currentTabId = _viewState.value.selectedTabId
                val tab = _viewState.value.tabs.find { it.id == currentTabId } ?: return
                val target = tab.results.find { it.id == intent.targetId } ?: return
                if (tab.inputText.isNotBlank() && target.translatedText.isNotBlank()) {
                    viewModelScope.launch {
                        repository.toggleFavorite(
                            word = tab.inputText,
                            translation = target.translatedText,
                            sourceLang = tab.sourceLangCode,
                            targetLang = target.targetLangCode
                        )
                        val isFav = repository.isFavorite(
                            tab.inputText,
                            tab.sourceLangCode,
                            target.targetLangCode
                        )
                        _viewState.update { currentState ->
                            currentState.copy(tabs = currentState.tabs.map { t ->
                                if (t.id == currentTabId) {
                                    t.copy(results = t.results.map { r ->
                                        if (r.id == intent.targetId) r.copy(isTranslationFavorite = isFav) else r
                                    })
                                } else t
                            })
                        }
                    }
                }
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
                _viewState.update { currentState ->
                    val newTab = TranslationTabState(
                        tabName = intent.favorite.sourceLangCode.uppercase(Locale.ROOT),
                        inputText = intent.favorite.word,
                        sourceLangCode = intent.favorite.sourceLangCode,
                        sourceLanguageName = getLanguageName(intent.favorite.sourceLangCode),
                        results = listOf(
                            TranslationResultState(
                                targetLangCode = intent.favorite.targetLangCode,
                                targetLanguageName = getLanguageName(intent.favorite.targetLangCode),
                                translatedText = intent.favorite.translation,
                                isTranslationFavorite = true
                            )
                        )
                    )
                    currentState.copy(
                        tabs = currentState.tabs + newTab,
                        selectedTabId = newTab.id,
                        isShowingFavorites = false
                    )
                }
            }

            TranslatorIntent.StartVoiceInput -> {
                val tab = _viewState.value.currentTab
                if (tab != null) {
                    viewModelScope.launch {
                        _effect.emit(TranslatorEffect.OpenVoiceRecognition(tab.sourceLangCode))
                    }
                }
            }
        }
    }

    private fun launchAutoTranslation(tabId: String) {
        translationJobs[tabId]?.cancel()
        translationJobs[tabId] = viewModelScope.launch {
            delay(600)
            triggerTranslation(tabId)
        }
    }

    private fun triggerWordTranslation(tabId: String, word: String) {
        if (word.isBlank()) return
        val tab = _viewState.value.tabs.find { it.id == tabId } ?: return
        val target = tab.results.firstOrNull() ?: return

        viewModelScope.launch {
            val result = repository.translateText(
                word,
                tab.sourceLangCode,
                target.targetLangCode
            )

            result.onSuccess { translation ->
                val isFav = repository.isFavorite(
                    word,
                    tab.sourceLangCode,
                    target.targetLangCode
                )
                _viewState.update { currentState ->
                    currentState.copy(tabs = currentState.tabs.map { t ->
                        if (t.id == tabId) t.copy(selectedWordTranslation = translation, isSelectedWordFavorite = isFav) else t
                    })
                }
            }
        }
    }

    private fun triggerTranslation(tabId: String) {
        val tab = _viewState.value.tabs.find { it.id == tabId } ?: return
        if (tab.inputText.isBlank()) {
            _viewState.update { currentState ->
                currentState.copy(tabs = currentState.tabs.map { t ->
                    if (t.id == tabId) {
                        t.copy(results = t.results.map { r -> r.copy(translatedText = "", isTranslationFavorite = false) })
                    } else t
                })
            }
            return
        }

        tab.results.forEach { target ->
            viewModelScope.launch {
                _viewState.update { currentState ->
                    currentState.copy(tabs = currentState.tabs.map { t ->
                        if (t.id == tabId) {
                            t.copy(results = t.results.map { r ->
                                if (r.id == target.id) r.copy(isLoading = true, error = null) else r
                            })
                        } else t
                    })
                }

                val result = repository.translateText(
                    tab.inputText,
                    tab.sourceLangCode,
                    target.targetLangCode
                )

                result.onSuccess { translationResult ->
                    val isFav = repository.isFavorite(
                        tab.inputText,
                        tab.sourceLangCode,
                        target.targetLangCode
                    )
                    _viewState.update { currentState ->
                        currentState.copy(tabs = currentState.tabs.map { t ->
                            if (t.id == tabId) {
                                t.copy(results = t.results.map { r ->
                                    if (r.id == target.id) r.copy(translatedText = translationResult, isLoading = false, isTranslationFavorite = isFav) else r
                                })
                            } else t
                        })
                    }
                }.onFailure { exception ->
                    _viewState.update { currentState ->
                        currentState.copy(tabs = currentState.tabs.map { t ->
                            if (t.id == tabId) {
                                t.copy(results = t.results.map { r ->
                                    if (r.id == target.id) r.copy(isLoading = false, error = exception.localizedMessage) else r
                                })
                            } else t
                        })
                    }
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

    private fun refreshFavoritesStatus() {
        viewModelScope.launch {
            _viewState.update { currentState ->
                val updatedTabs = currentState.tabs.map { tab ->
                    val updatedResults = tab.results.map { res ->
                        val isFav = repository.isFavorite(tab.inputText, tab.sourceLangCode, res.targetLangCode)
                        res.copy(isTranslationFavorite = isFav)
                    }
                    val isWordFav = if (tab.selectedWord.isNotBlank() && tab.results.isNotEmpty()) {
                        repository.isFavorite(tab.selectedWord, tab.sourceLangCode, tab.results.first().targetLangCode)
                    } else false
                    tab.copy(results = updatedResults, isSelectedWordFavorite = isWordFav)
                }
                currentState.copy(tabs = updatedTabs)
            }
        }
    }
}

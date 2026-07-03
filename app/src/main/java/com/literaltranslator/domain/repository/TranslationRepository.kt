package com.literaltranslator.domain.repository

import com.literaltranslator.domain.model.LanguageModel

interface TranslationRepository {
    /**
     * Translates text
     * Auto-downloading of language packages if there is no on a device
     */
    suspend fun translateText(
        text: String,
        sourceLangCode: String,
        targetLangCode: String
    ): Result<String>

    suspend fun getAvailableLanguages(): Result<List<LanguageModel>>

    suspend fun toggleFavorite(word: String, translation: String, sourceLang: String, targetLang: String)
    suspend fun isFavorite(word: String, sourceLang: String, targetLang: String): Boolean
}

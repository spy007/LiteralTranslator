package com.kirillex.multispeakliteral.domain.repository

import com.kirillex.multispeakliteral.domain.model.LanguageModel
import com.kirillex.multispeakliteral.domain.model.FavoriteModel
import kotlinx.coroutines.flow.Flow

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

    suspend fun getAvailableLanguages(): Result<List<com.kirillex.multispeakliteral.domain.model.LanguageModel>>

    suspend fun toggleFavorite(word: String, translation: String, sourceLang: String, targetLang: String)
    suspend fun isFavorite(word: String, sourceLang: String, targetLang: String): Boolean
    fun getAllFavorites(): Flow<List<com.kirillex.multispeakliteral.domain.model.FavoriteModel>>
    suspend fun deleteFavoriteById(id: Int)
}

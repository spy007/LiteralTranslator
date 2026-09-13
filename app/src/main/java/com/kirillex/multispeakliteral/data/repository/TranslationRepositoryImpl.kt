package com.kirillex.multispeakliteral.data.repository

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translator
import com.kirillex.multispeakliteral.data.local.entity.FavoriteWord
import com.kirillex.multispeakliteral.domain.model.FavoriteModel
import com.kirillex.multispeakliteral.domain.model.LanguageModel
import com.kirillex.multispeakliteral.utils.getLanguageName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class TranslationRepositoryImpl(
    private val favoriteDao: com.kirillex.multispeakliteral.data.local.dao.FavoriteDao? = null
) : com.kirillex.multispeakliteral.domain.repository.TranslationRepository {

    private var currentTranslator: Translator? = null
    private var currentSourceLang: String? = null
    private var currentTargetLang: String? = null

    override suspend fun translateText(
        text: String,
        sourceLangCode: String,
        targetLangCode: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val client = getTranslator(sourceLangCode, targetLangCode)

            val translatedResult = client.translate(text).await()

            Result.success(translatedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getTranslator(sourceLang: String, targetLang: String): Translator {
        if (currentTranslator != null &&
            currentSourceLang == sourceLang &&
            currentTargetLang == targetLang
        ) {
            return currentTranslator!!
        }

        // Close previous translator if it exists
        currentTranslator?.close()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()

        val client = Translation.getClient(options)
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        client.downloadModelIfNeeded(conditions).await()

        currentTranslator = client
        currentSourceLang = sourceLang
        currentTargetLang = targetLang

        return client
    }

    override suspend fun getAvailableLanguages(): Result<List<com.kirillex.multispeakliteral.domain.model.LanguageModel>> = withContext(Dispatchers.IO) {
        try {
            val languagesList = TranslateLanguage.getAllLanguages().toList().map { code ->
                LanguageModel(
                    code,
                    getLanguageName(code)
                )
            }
            Result.success(languagesList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(
        word: String,
        translation: String,
        sourceLang: String,
        targetLang: String
    ) {
        favoriteDao?.let { dao ->
            if (dao.isFavorite(word, sourceLang, targetLang)) {
                dao.deleteFavoriteByDetails(word, sourceLang, targetLang)
            } else {
                dao.insertFavorite(
                    FavoriteWord(
                        word = word,
                        translation = translation,
                        sourceLang = sourceLang,
                        targetLang = targetLang
                    )
                )
            }
        }
    }

    override suspend fun isFavorite(
        word: String,
        sourceLang: String,
        targetLang: String
    ): Boolean {
        return favoriteDao?.isFavorite(word, sourceLang, targetLang) ?: false
    }

    override fun getAllFavorites(): Flow<List<FavoriteModel>> {
        return favoriteDao?.getAllFavorites()?.map { list ->
            list.map {
                FavoriteModel(
                    id = it.id,
                    word = it.word,
                    translation = it.translation,
                    sourceLangCode = it.sourceLang,
                    targetLangCode = it.targetLang,
                    timestamp = it.timestamp
                )
            }
        } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    override suspend fun deleteFavoriteById(id: Int) {
        favoriteDao?.deleteFavoriteById(id)
    }
}

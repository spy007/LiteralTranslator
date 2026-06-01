package com.literaltranslator.data.repository

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.literaltranslator.domain.repository.TranslationRepository
import com.literaltranslator.domain.model.LanguageModel
import com.literaltranslator.utils.getLanguageName
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class TranslationRepositoryImpl : TranslationRepository {

    override suspend fun translateText(
        text: String,
        sourceLangCode: String,
        targetLangCode: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLangCode)
                .setTargetLanguage(targetLangCode)
                .build()

            val client = Translation.getClient(options)

            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            client.downloadModelIfNeeded(conditions).await()

            val translatedResult = client.translate(text).await()

            client.close()

            Result.success(translatedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailableLanguages(): Result<List<LanguageModel>> = withContext(Dispatchers.IO) {
        try {
            val languagesList = TranslateLanguage.getAllLanguages().toList().map { code ->
                LanguageModel(code, getLanguageName(code))
            }
            Result.success(languagesList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// TranslationRepositoryImpl.kt

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.literaltranslator.TranslationRepository
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
            // 1. Конфигурируем опции переводчика на основе кодов языков (например, "en", "pt")
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLangCode)
                .setTargetLanguage(targetLangCode)
                .build()

            val client = Translation.getClient(options)

            // 2. Задаем условия для скачивания языкового пакета (требуем только Wi-Fi для экономии трафика)
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            // 3. Скачиваем пакеты, если они не были скачаны ранее
            client.downloadModelIfNeeded(conditions).await()

            // 4. Выполняем перевод
            val translatedResult = client.translate(text).await()
            
            // Обязательно закрываем клиент для предотвращения утечек памяти
            client.close()

            Result.success(translatedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

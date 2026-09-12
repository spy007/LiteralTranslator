package com.literaltranslator.presentation.translator

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.literaltranslator.ui.theme.BackgroundGray

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun TranslatorScreenPreview2() {
    val previewTab = TranslationTabState(
        id = "preview_tab",
        tabName = "EN",
        inputText = "Hello World",
        sourceLanguageName = "English",
        results = listOf(
            TranslationResultState(
                id = "res1",
                targetLanguageName = "Portuguese",
                translatedText = "Olá Mundo"
            )
        )
    )
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundGray
    ) {
        TranslatorScreenContent(
            state = TranslatorState(
                tabs = listOf(previewTab),
                selectedTabId = "preview_tab"
            ),
            onIntent = {}
        )
    }
}

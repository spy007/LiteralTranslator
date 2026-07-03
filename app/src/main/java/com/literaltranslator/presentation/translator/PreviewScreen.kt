package com.literaltranslator.presentation.translator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.literaltranslator.ui.theme.TestAppTheme

//@Preview(showBackground = true)
//@Composable
//fun TranslatorScreenPreview1() {
//    TestAppTheme {
//        TranslatorScreen()
//    }
//}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun TranslatorScreenPreview2() {
    TestAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column {
                TranslatorScreenContent(
                    state = TranslatorState(
                        inputText = "Hello World",
                        translatedText = "Olá Mundo",
                        sourceLanguageName = "English",
                        targetLanguageName = "Portuguese"
                    ),
                    onIntent = {}
                )
            }
        }
    }
}
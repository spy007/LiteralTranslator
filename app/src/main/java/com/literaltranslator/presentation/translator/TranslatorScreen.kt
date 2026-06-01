package com.literaltranslator.presentation.translator

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.literaltranslator.data.repository.TranslationRepositoryImpl
import com.literaltranslator.presentation.translator.components.BottomLanguageSelector
import com.literaltranslator.presentation.translator.components.TopNavigationBar
import com.literaltranslator.presentation.translator.components.TranslatorInputSection
import com.literaltranslator.presentation.translator.components.TranslatorOutputSection
import androidx.compose.material3.Surface
import com.literaltranslator.ui.theme.BackgroundGray
import com.literaltranslator.ui.theme.BrandBlue
import com.literaltranslator.ui.theme.TestAppTheme
import com.literaltranslator.ui.theme.TopWaveDesignShape
import com.literaltranslator.utils.TextToSpeechManager

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun TranslatorScreenContentPreview() {
    TestAppTheme {
        Surface {
            TranslatorScreenContent(
                state = TranslatorState(
                    inputText = "Hello, how are you?",
                    translatedText = "Hola, ¿cómo estás?",
                    sourceLanguageName = "English",
                    targetLanguageName = "Spanish"
                ),
                onIntent = {}
            )
        }
    }
}

@Composable
fun TranslatorScreen(modifier: Modifier = Modifier) {
    val factory = viewModelFactory {
        initializer {
            TranslatorViewModel(repository = TranslationRepositoryImpl())
        }
    }
    val viewModel: TranslatorViewModel = viewModel(factory = factory)
    val state by viewModel.viewState.collectAsState()

    val context = LocalContext.current
    val ttsManager = remember { TextToSpeechManager(context) }
    val clipboardManager = LocalClipboardManager.current

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TranslatorEffect.ShowToast -> {
                    Toast.makeText(
                        context,
                        effect.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is TranslatorEffect.ShowToastRes -> {
                    val message = context.getString(effect.resId, *effect.args.toTypedArray())
                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is TranslatorEffect.PlaySpeech -> {
                    ttsManager.speak(effect.text, effect.langCode)
                }

                is TranslatorEffect.CopyToClipboard -> {
                    clipboardManager.setText(AnnotatedString(effect.text))
                }

                TranslatorEffect.TriggerHapticFeedback -> {
                    // Vibration call code
                }
            }
        }
    }

    TranslatorScreenContent(
        state = state,
        modifier = modifier,
        onIntent = (viewModel::handleIntent)
    )
}

@Composable
fun TranslatorScreenContent(
    state: TranslatorState,
    onIntent: (TranslatorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(BackgroundGray, shape = TopWaveDesignShape)
            ) {
                Box(modifier = Modifier.padding(top = 60.dp)) {
                    TranslatorInputSection(state, onIntent)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
            ) {
                TranslatorOutputSection(state, onIntent)
            }
            BottomLanguageSelector(
                state = state,
                onIntent = onIntent
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .height(50.dp)
                    .background(BrandBlue, shape = TopWaveDesignShape)
            ) {
                TopNavigationBar()
            }
            Box(
                modifier = Modifier
                    .weight(0.2f)
                    .height(50.dp)
            )
        }
    }
}

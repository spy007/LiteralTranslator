package com.literaltranslator.presentation.translator

import android.annotation.SuppressLint
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.literaltranslator.R
import com.literaltranslator.data.local.AppDatabase
import com.literaltranslator.data.repository.TranslationRepositoryImpl
import com.literaltranslator.presentation.translator.components.*
import com.literaltranslator.ui.theme.TestAppTheme
import com.literaltranslator.utils.TextToSpeechManager

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun FuturisticTranslatorScreen(modifier: Modifier = Modifier) {
    val appContext = LocalContext.current.applicationContext
    val factory = viewModelFactory {
        initializer {
            val database = AppDatabase.getDatabase(appContext)
            TranslatorViewModel(
                repository = TranslationRepositoryImpl(
                    favoriteDao = database.favoriteDao()
                )
            )
        }
    }
    val viewModel: TranslatorViewModel = viewModel(factory = factory)

    val context = LocalContext.current
    val isInspectionMode = LocalInspectionMode.current
    val ttsManager = remember {
        if (isInspectionMode) null else TextToSpeechManager(context)
    }
    val clipboardManager = LocalClipboardManager.current

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            spokenText?.let {
                viewModel.handleIntent(TranslatorIntent.ChangeInputText(it))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { ttsManager?.shutdown() }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TranslatorEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is TranslatorEffect.ShowToastRes -> {
                    val message = context.getString(effect.resId, effect.args.toTypedArray())
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                is TranslatorEffect.PlaySpeech -> {
                    ttsManager?.speak(effect.text, effect.langCode)
                }
                is TranslatorEffect.CopyToClipboard -> {
                    clipboardManager.setText(AnnotatedString(effect.text))
                }
                is TranslatorEffect.ShareText -> {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
                TranslatorEffect.TriggerHapticFeedback -> { /* Vibration code */ }
                is TranslatorEffect.OpenVoiceRecognition -> {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, effect.langCode)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_prompt))
                    }
                    try {
                        voiceLauncher.launch(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Voice recognition not supported", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val state = viewModel.viewState.collectAsState().value
    FuturisticTranslatorScreenContent(
        state = state,
        modifier = modifier,
        onIntent = (viewModel::handleIntent)
    )

    if (state.isShowingFavorites) {
        FavoritesSheet(
            favorites = state.favorites,
            onIntent = viewModel::handleIntent
        )
    }
}

@Composable
fun FuturisticTranslatorScreenContent(
    state: TranslatorState,
    onIntent: (TranslatorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FBFF)) // Very light blue-ish background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            FuturisticHeader(
                onAddClick = { onIntent(TranslatorIntent.OpenFavorites) }
            )
            
            Spacer(modifier = Modifier.height(2.dp)) // Minimal gap
            
            FuturisticInputSection(
                state = state,
                onValueChange = { onIntent(TranslatorIntent.ChangeInputText(it)) }
            )
            
            Spacer(modifier = Modifier.height(2.dp)) // Minimal gap between field and oval
            
            FuturisticLanguageSelector(
                state = state,
                onClick = { /* Could open language selection */ }
            )

            Spacer(modifier = Modifier.height(2.dp)) // Minimal gap between oval and output
            
            FuturisticOutputSection(
                state = state,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun FuturisticTranslatorScreenPreview() {
    TestAppTheme {
        FuturisticTranslatorScreenContent(
            state = TranslatorState(
                inputText = "",
                sourceLanguageName = "English",
                targetLanguageName = "Spanish"
            ),
            onIntent = {}
        )
    }
}

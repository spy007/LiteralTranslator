package com.literaltranslator.presentation.translator

import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
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
import com.literaltranslator.presentation.translator.components.BottomLanguageSelector
import com.literaltranslator.presentation.translator.components.FavoritesSheet
import com.literaltranslator.presentation.translator.components.TopNavigationBar
import com.literaltranslator.presentation.translator.components.TranslatorInputSection
import com.literaltranslator.presentation.translator.components.TranslatorOutputSection
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.literaltranslator.ui.theme.BackgroundGray
import com.literaltranslator.ui.theme.BrandBlue
import com.literaltranslator.ui.theme.TestAppTheme
import com.literaltranslator.ui.theme.TopWaveDesignShape
import com.literaltranslator.utils.TextToSpeechManager

@Composable
fun TranslatorScreen(modifier: Modifier = Modifier) {
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
    val state by viewModel.viewState.collectAsState()

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

                TranslatorEffect.TriggerHapticFeedback -> {
                    // Vibration call code
                }

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

    TranslatorScreenContent(
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
                    .weight(0.45f)
                    .background(BackgroundGray, shape = TopWaveDesignShape)
            ) {
                Box(modifier = Modifier.padding(top = 40.dp)) {
                    TranslatorInputSection(state, onIntent)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
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
                    .height(40.dp)
                    .background(BrandBlue, shape = TopWaveDesignShape)
            ) {
                TopNavigationBar(
                    onFavoritesClick = { onIntent(TranslatorIntent.OpenFavorites) }
                )
            }
            Box(
                modifier = Modifier
                    .weight(0.2f)
                    .height(50.dp)
            )
        }
    }
}

// Preview is working in PreviewScreen.kt
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun TranslatorScreenPreview() {
    TestAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column {
                Text(text = "Preview Active", modifier = Modifier.padding(8.dp), color = Color.Red)
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

@Preview(showBackground = true)
@Composable
fun TranslatorScreenPreview1() {
    TestAppTheme {
        TranslatorScreen()
    }
}

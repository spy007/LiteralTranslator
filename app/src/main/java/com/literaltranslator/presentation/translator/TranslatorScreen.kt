package com.literaltranslator.presentation.translator

import android.annotation.SuppressLint
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.literaltranslator.R
import com.literaltranslator.data.local.AppDatabase
import com.literaltranslator.data.repository.TranslationRepositoryImpl
import com.literaltranslator.presentation.translator.components.FavoritesSheet
import com.literaltranslator.presentation.translator.components.LanguageSelectionSheet
import com.literaltranslator.presentation.translator.components.TopNavigationBar
import com.literaltranslator.ui.components.shimmerLoading
import com.literaltranslator.ui.theme.*
import com.literaltranslator.utils.TextToSpeechManager
import kotlin.math.roundToInt

@SuppressLint("LocalContextGetResourceValueCall")
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
    val clipboard = LocalClipboard.current

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
                    val message = context.getString(effect.resId, *effect.args.toTypedArray())
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                is TranslatorEffect.PlaySpeech -> {
                    ttsManager?.speak(effect.text, effect.langCode)
                }
                is TranslatorEffect.CopyToClipboard -> {
                    val clipData = ClipData.newPlainText("Translation", effect.text)
                    clipboard.setClipEntry(clipData.toClipEntry())
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
                    // Feedback handled if needed
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranslatorScreenContent(
    state: TranslatorState,
    onIntent: (TranslatorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTab = state.currentTab
    var resultId by remember { mutableStateOf<String?>(null) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .defaultMinSize(minWidth = 360.dp, minHeight = 640.dp)
            .background(BackgroundGray)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom modern Top bar header with wave design shape integration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandBlue)
            ) {
                TopNavigationBar(
                    onFavoritesClick = { onIntent(TranslatorIntent.OpenFavorites) }
                )
            }

            // Row with windows tabs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(state.tabs, key = { it.id }) { tab ->
                            val isSelected = tab.id == state.selectedTabId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) BrandBlue.copy(alpha = 0.15f) else BackgroundGray,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, BrandBlue) else null,
                                modifier = Modifier.combinedClickable(
                                    onClick = { onIntent(TranslatorIntent.SelectInputTab(tab.id)) },
                                    onLongClick = { onIntent(TranslatorIntent.RemoveInputTab(tab.id)) }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tab.tabName,
                                        fontFamily = mainFont,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BrandBlue else TextPrimary
                                    )
                                    if (state.tabs.size > 1) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close tab",
                                            tint = if (isSelected) BrandBlue.copy(alpha = 0.6f) else TextLightGray,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .clickable { onIntent(TranslatorIntent.RemoveInputTab(tab.id)) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { onIntent(TranslatorIntent.AddInputTab) },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = BrandBlue, contentColor = Color.White),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add window tab", modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (currentTab != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Selected tab input section card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SuggestionChip(
                                        onClick = { onIntent(TranslatorIntent.ClickSourceLanguage) },
                                        label = { Text(text = currentTab.sourceLanguageName, fontFamily = mainFont, fontWeight = FontWeight.Bold) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            labelColor = BrandBlue,
                                            containerColor = BrandBlue.copy(alpha = 0.08f)
                                        ),
                                        border = SuggestionChipDefaults.suggestionChipBorder(borderColor = Color.Transparent, enabled = true),
                                        icon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandBlue) }
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (currentTab.inputText.isNotEmpty()) {
                                            IconButton(onClick = { onIntent(TranslatorIntent.SpeakSourceText) }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_sound),
                                                    contentDescription = "Speak source text",
                                                    tint = BrandBlue
                                                )
                                            }
                                        }
                                        IconButton(onClick = { onIntent(TranslatorIntent.StartVoiceInput) }) {
                                            Icon(painter = painterResource(id = R.drawable.ic_mic), contentDescription = "Voice input", tint = if (state.isRecording) Color.Red else TextPrimary)
                                        }
                                        if (currentTab.inputText.isNotEmpty()) {
                                            IconButton(onClick = { onIntent(TranslatorIntent.ClearText) }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextLightGray)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                val annotatedInput = buildAnnotatedString {
                                    append(currentTab.inputText)
                                    currentTab.selectedWordRange?.let { range ->
                                        if (range.first in currentTab.inputText.indices && range.last < currentTab.inputText.length) {
                                            addStyle(SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold), range.first, range.last + 1)
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 120.dp)
                                        .pointerInput(currentTab.inputText) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    awaitFirstDown(pass = PointerEventPass.Initial)
                                                    val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                                    if (up != null) {
                                                        textLayoutResult?.let { layout ->
                                                            val position = layout.getOffsetForPosition(up.position)
                                                            if (currentTab.inputText.isNotEmpty() && position in currentTab.inputText.indices && !currentTab.inputText[position].isWhitespace()) {
                                                                var start = position
                                                                while (start > 0 && !currentTab.inputText[start - 1].isWhitespace()) start--
                                                                var end = position
                                                                while (end < currentTab.inputText.length && !currentTab.inputText[end].isWhitespace()) end++
                                                                onIntent(TranslatorIntent.SelectWord(start until end))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    BasicTextField(
                                        value = currentTab.inputText,
                                        onValueChange = { onIntent(TranslatorIntent.ChangeInputText(it)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        onTextLayout = { textLayoutResult = it },
                                        visualTransformation = { TransformedText(annotatedInput, OffsetMapping.Identity) },
                                        textStyle = TextStyle(color = TextPrimary, fontFamily = mainFont, fontSize = 18.sp, lineHeight = 26.sp),
                                        decorationBox = { innerTextField ->
                                            if (currentTab.inputText.isEmpty()) {
                                                Text(stringResource(R.string.input_hint), fontFamily = mainFont, color = TextLightGray, fontSize = 18.sp)
                                            }
                                            innerTextField()
                                        }
                                    )

                                    // Popup for single word selection translation
                                    currentTab.selectedWordRange?.let { range ->
                                        textLayoutResult?.let { layout ->
                                            if (range.first in currentTab.inputText.indices) {
                                                val firstBounds = layout.getBoundingBox(range.first)
                                                val lastBounds = layout.getBoundingBox(range.last)
                                                val centerX = (firstBounds.left + lastBounds.right) / 2
                                                val density = LocalDensity.current
                                                Popup(
                                                    alignment = Alignment.TopStart,
                                                    offset = IntOffset(
                                                        x = (centerX - with(density) { 40.dp.toPx() }).roundToInt().coerceAtLeast(0),
                                                        y = (firstBounds.top - with(density) { 44.dp.toPx() }).roundToInt()
                                                    ),
                                                    onDismissRequest = { onIntent(TranslatorIntent.SelectWord(null)) }
                                                ) {
                                                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White, shadowElevation = 6.dp) {
                                                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            if (currentTab.selectedWordTranslation.isBlank()) {
                                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = BrandBlue)
                                                            } else {
                                                                IconButton(onClick = { onIntent(TranslatorIntent.SpeakSelectedWordTranslation) }, modifier = Modifier.size(24.dp)) {
                                                                    Icon(painter = painterResource(id = R.drawable.ic_sound), contentDescription = null, modifier = Modifier.size(14.dp))
                                                                }
                                                                Text(text = currentTab.selectedWordTranslation, color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = mainFont)
                                                                IconButton(onClick = { onIntent(TranslatorIntent.ToggleFavoriteSelectedWord) }, modifier = Modifier.size(24.dp)) {
                                                                    Icon(painter = painterResource(id = if (currentTab.isSelectedWordFavorite) R.drawable.ic_star else R.drawable.ic_star_outlined), contentDescription = null, modifier = Modifier.size(14.dp), tint = if (currentTab.isSelectedWordFavorite) Color(0xFFFFB300) else TextLightGray)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Section Title header for target translations
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Translations",
                                fontFamily = mainFont,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { onIntent(TranslatorIntent.SwapLanguages(resultId ?: return@IconButton)) }, modifier = Modifier.size(32.dp)) {
                                Icon(painter = painterResource(id = R.drawable.ic_swap), contentDescription = "Swap with source", modifier = Modifier.size(16.dp))
                            }

                            Button(
                                onClick = { onIntent(TranslatorIntent.AddTranslationTarget(currentTab.id)) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandBlue),
                                shape = RoundedCornerShape(10.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Add Language", fontFamily = mainFont, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Render multiple output translation cards windows
                    items(currentTab.results, key = { it.id }) { result ->
                        resultId = result.id
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SuggestionChip(
                                            onClick = { onIntent(TranslatorIntent.ClickTargetLanguage(result.id)) },
                                            label = { Text(text = result.targetLanguageName, fontFamily = mainFont, fontWeight = FontWeight.Bold) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = TextPrimary, containerColor = BackgroundGray),
                                            border = SuggestionChipDefaults.suggestionChipBorder(borderColor = Color.Transparent, enabled = true),
                                            icon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary) }
                                        )
                                    }
                                    if (result.translatedText.isNotEmpty())
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { onIntent(TranslatorIntent.SpeakTargetText(result.id)) }) {
                                                Icon(painter = painterResource(id = R.drawable.ic_sound), contentDescription = "Speak translation", tint = TextPrimary.copy(alpha = 0.7f))
                                            }
                                            IconButton(onClick = { onIntent(TranslatorIntent.ToggleFavoriteTranslation(result.id)) }) {
                                                Icon(
                                                    painter = painterResource(id = if (result.isTranslationFavorite) R.drawable.ic_star else R.drawable.ic_star_outlined),
                                                    contentDescription = "Favorite",
                                                    tint = if (result.isTranslationFavorite) Color(0xFFFFB300) else TextPrimary.copy(alpha = 0.5f)
                                                )
                                            }
                                            IconButton(onClick = { onIntent(TranslatorIntent.CopyTranslation(result.id)) }) {
                                                Icon(painter = painterResource(id = R.drawable.ic_copy), contentDescription = "Copy", tint = TextPrimary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { onIntent(TranslatorIntent.ShareTranslation(result.id)) }) {
                                                Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                            }
                                            if (currentTab.results.size > 1) {
                                                IconButton(onClick = { onIntent(TranslatorIntent.RemoveTranslationTarget(currentTab.id, result.id)) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (result.isLoading) {
                                    Box(modifier = Modifier.width(140.dp).height(24.dp).shimmerLoading(isLoading = true, shape = RoundedCornerShape(6.dp)))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(16.dp).shimmerLoading(isLoading = true, shape = RoundedCornerShape(4.dp)))
                                } else {
                                    val textColor = if (result.translatedText.isEmpty()) TextPrimary.copy(alpha = 0.4f) else TextPrimary
                                    Text(
                                        text = result.translatedText.ifBlank { stringResource(R.string.translation_hint) },
                                        color = textColor,
                                        fontFamily = mainFont,
                                        fontSize = 18.sp,
                                        lineHeight = 26.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.isSelectingSourceLang && currentTab != null) {
        LanguageSelectionSheet(
            title = stringResource(R.string.translate_from),
            languages = state.availableLanguages,
            selectedLanguageCode = currentTab.sourceLangCode,
            onLanguageSelected = { onIntent(TranslatorIntent.SelectSourceLanguage(it)) },
            onDismiss = { onIntent(TranslatorIntent.DismissLanguageSelection) }
        )
    }

    if (state.isSelectingTargetLang) {
        val targetId = state.selectingTargetId
        val currentTarget = currentTab?.results?.find { it.id == targetId }
        if (currentTarget != null) {
            LanguageSelectionSheet(
                title = stringResource(R.string.translate_to),
                languages = state.availableLanguages,
                selectedLanguageCode = currentTarget.targetLangCode,
                onLanguageSelected = { onIntent(TranslatorIntent.SelectTargetLanguage(it)) },
                onDismiss = { onIntent(TranslatorIntent.DismissLanguageSelection) }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun TranslatorScreenPreview() {
    val previewTab = TranslationTabState(
        id = "preview_tab",
        tabName = "EN",
        inputText = "Hello World",
        results = listOf(
            TranslationResultState(id = "res1", targetLanguageName = "Spanish", translatedText = "Hola Mundo"),
            TranslationResultState(id = "res2", targetLanguageName = "French", translatedText = "Bonjour le monde")
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

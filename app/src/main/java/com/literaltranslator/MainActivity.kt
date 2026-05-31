package com.literaltranslator

import TranslationRepositoryImpl
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.literaltranslator.ui.theme.TestAppTheme
import com.literaltranslator.ui.theme.mainFont
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt

// Precise color palette from the original design
val BrandBlue = Color(0xFF2962FF)
val BackgroundGray = Color(0xFFF4F6F9)   // Light background for the application
val TextPrimary = Color(0xFF3F414E)      // Deep slate blue from the design image
val TextDark = Color(0xFF0D1B2A)
val TextLightGray = Color(0xFF90A4AE)
val TagGreen = Color(0xFF4CAF50)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding: PaddingValues ->
                    TranslatorScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranslatorScreenPreview() {
    // Pass static text data; rendering will take a fraction of a second
    TranslatorScreenContent(
        state = TranslatorState(
            inputText = "Hope you like this approach",
            translatedText = "Esperança",
            sourceLanguageName = "English",
            targetLanguageName = "Portuguese"
        ),
        onIntent = {} // Empty lambda; clicks in preview won't trigger logic
    )
}

// Refined shapes for CustomTranslatorDesign matching the 50dp corner radius
val TopWaveDesignShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    // Use a fixed radius matching the 'corner = 50' used in the bottom bar
    // Note: We use 50f here as a density-independent-like value for the shape
    val r = 150f

    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h - r)
    
    // Right arc (concave) - matches RoundedCornerShape(50)
    // Using a circular-like arc approximation with cubicTo
    // kappa = 4/3 * (sqrt(2) - 1) / 1 ≈ 0.55228
    val k = 0.55228f
    cubicTo(
        w, h - r * (1 - k),
        w - r * (1 - k), h,
        w - r, h
    )
    
    // Bottom line
    lineTo(r, h)
    
    // Left arc (convex) - matching the other side
    cubicTo(
        r * (1 - k), h,
        0f, h + r * (1 - k),
        0f, h + r
    )
    close()
}

@Preview(showBackground = true)
@Composable
fun CustomTranslatorDesignPreview() {
    TestAppTheme { CustomTranslatorDesign() }
}

@Composable
fun CustomTranslatorDesign() {
}

@Composable
fun TranslatorScreen(modifier: Modifier = Modifier) {
    // 1. Create ViewModel ONLY here. Preview will not enter here.
    val factory = viewModelFactory {
        initializer {
            TranslatorViewModel(repository = TranslationRepositoryImpl())
        }
    }
    val viewModel: TranslatorViewModel = viewModel(factory = factory)
    val state by viewModel.viewState.collectAsState()

    // Listen for effects
    val context = androidx.compose.ui.platform.LocalContext.current
    val ttsManager = remember { TextToSpeechManager(context) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TranslatorEffect.ShowToast -> {
                    android.widget.Toast.makeText(
                        context,
                        effect.message,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

                is TranslatorEffect.ShowToastRes -> {
                    val message = context.getString(effect.resId, *effect.args.toTypedArray())
                    android.widget.Toast.makeText(
                        context,
                        message,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

                is TranslatorEffect.PlaySpeech -> {
                    ttsManager.speak(effect.text, effect.langCode)
                }

                is TranslatorEffect.CopyToClipboard -> {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(effect.text))
                }

                TranslatorEffect.TriggerHapticFeedback -> {
                    // Vibration call code
                }
                // FIXED: Duplicate branch for TriggerHapticFeedback removed
            }
        }
    }

    // Pass all clean data to the Stateless screen
    TranslatorScreenContent(
        state = state,
        modifier = modifier,
        onIntent = (viewModel::handleIntent)
    )
}


@Composable
fun TopNavigationBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {}) {
            Icon(
                painter = painterResource(id = R.drawable.menu),
                contentDescription = stringResource(R.string.menu_desc),
                tint = Color.White
            )
        }
    }
}

@Composable
fun TextToTranslateSection(
    state: TranslatorState,
    onIntent: (TranslatorIntent) -> Unit
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Helper to find word boundaries
    fun findWordRange(text: String, index: Int): IntRange? {
        if (text.isEmpty() || index < 0 || index >= text.length || text[index].isWhitespace()) return null
        var start = index
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        var end = index
        while (end < text.length && !text[end].isWhitespace()) end++
        return start until end
    }

    val annotatedInput = buildAnnotatedString {
        append(state.inputText)
        state.selectedWordRange?.let { range ->
            if (range.first in state.inputText.indices && range.last < state.inputText.length) {
                addStyle(
                    style = SpanStyle(
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    ),
                    start = range.first,
                    end = range.last + 1
                )
            }
        }
    }

    val wordTransformation = VisualTransformation { _ ->
        TransformedText(
            text = annotatedInput,
            offsetMapping = OffsetMapping.Identity
        )
    }

    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 50.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onIntent(TranslatorIntent.SpeakSourceText) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_sound),
                                contentDescription = stringResource(R.string.source_speech_desc),
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.sourceLanguageName,
                            color = TextPrimary,
                            fontFamily = mainFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = { onIntent(TranslatorIntent.ClearText) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_desc),
                            tint = TextLightGray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Text input field
                BasicTextField(
                    value = state.inputText,
                    onValueChange = { newText ->
                        onIntent(
                            TranslatorIntent.ChangeInputText(
                                newText
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(state.inputText) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.type == PointerEventType.Release) {
                                        val change = event.changes.first()
                                        textLayoutResult?.let { layout ->
                                            val position = layout.getOffsetForPosition(change.position)
                                            val range = findWordRange(state.inputText, position)
                                            onIntent(TranslatorIntent.SelectWord(range))
                                        }
                                    }
                                }
                            }
                        },
                    onTextLayout = { textLayoutResult = it },
                    visualTransformation = wordTransformation,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontFamily = mainFont,
                        fontSize = 24.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.drawBehind {
                                state.selectedWordRange?.let { range ->
                                    textLayoutResult?.let { layout ->
                                        if (range.first in 0 until state.inputText.length &&
                                            range.last < state.inputText.length
                                        ) {
                                            val boundingBox = layout.getBoundingBox(range.first)
                                            var minX = boundingBox.left
                                            var minY = boundingBox.top
                                            var maxX = boundingBox.right
                                            var maxY = boundingBox.bottom

                                            for (i in range.first + 1..range.last) {
                                                val b = layout.getBoundingBox(i)
                                                minX = minOf(minX, b.left)
                                                minY = minOf(minY, b.top)
                                                maxX = maxOf(maxX, b.right)
                                                maxY = maxOf(maxY, b.bottom)
                                            }

                                            drawRoundRect(
                                                color = Color.White,
                                                topLeft = androidx.compose.ui.geometry.Offset(
                                                    minX - 4.dp.toPx(),
                                                    minY - 2.dp.toPx()
                                                ),
                                                size = androidx.compose.ui.geometry.Size(
                                                    maxX - minX + 8.dp.toPx(),
                                                    maxY - minY + 4.dp.toPx()
                                                ),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                                    8.dp.toPx(),
                                                    8.dp.toPx()
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            if (state.inputText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.input_hint),
                                    fontFamily = mainFont,
                                    color = TextPrimary.copy(alpha = 0.5f),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            innerTextField()

                            // CONTEXTUAL BUBBLE (POPUP)
                            state.selectedWordRange?.let { range ->
                                textLayoutResult?.let { layout ->
                                    if (range.first in state.inputText.indices && state.selectedWordTranslation.isNotBlank()) {
                                        val rect = layout.getBoundingBox(range.first)
                                        val density = LocalDensity.current
                                        val yOffsetPx = with(density) { -44.dp.toPx() }
                                        
                                        // Position popup above the word
                                        Popup(
                                            alignment = Alignment.TopStart,
                                            offset = IntOffset(
                                                x = rect.left.roundToInt(),
                                                y = (rect.top + yOffsetPx).roundToInt()
                                            ),
                                            onDismissRequest = { onIntent(TranslatorIntent.SelectWord(null)) }
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.White,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            ) {
                                                Text(
                                                    text = state.selectedWordTranslation,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    color = BrandBlue,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = mainFont
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TranslationSection(
    state: TranslatorState,
    onIntent: (TranslatorIntent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-20).dp) // Slight overlap for seamless wave connection
            .padding(24.dp)
    ) {
        Column {
            Spacer(modifier = Modifier.height(24.dp)) // Compensate for top curve

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onIntent(TranslatorIntent.SpeakTargetText) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sound),
                            contentDescription = stringResource(R.string.target_speech_desc)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.targetLanguageName,
                        color = TextPrimary,
                        fontFamily = mainFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = stringResource(R.string.favorites_desc),
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = { onIntent(TranslatorIntent.CopyTranslation) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = stringResource(R.string.copy_desc),
                            tint = TextPrimary.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_desc),
                        tint = TextPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // DISPLAY LOGIC: Shimmer loading or Text
            if (state.isLoading) {
                // Shimmer effect during translation or package download
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(36.dp)
                        .shimmerLoading(isLoading = true, shape = RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .shimmerLoading(isLoading = true, shape = RoundedCornerShape(4.dp))
                )

// Inside WINDOW 2 (Portuguese), content block ( isLoading == false ):
            } else {
                // 1. Tab switcher with dynamic highlights and clicks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // TRANSLATION Tab
                    val isTranslationActive = state.activeTab == TranslatorTab.TRANSLATION
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // Remove gray box on click
                        ) { onIntent(TranslatorIntent.SelectTab(TranslatorTab.TRANSLATION)) }
                    ) {
                        Text(
                            text = stringResource(R.string.tab_translation),
                            color = if (isTranslationActive) BrandBlue else TextPrimary.copy(alpha = 0.6f),
                            fontFamily = mainFont,
                            fontSize = 14.sp,
                            fontWeight = if (isTranslationActive) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 50.dp, height = 2.dp)
                                .background(if (isTranslationActive) BrandBlue else Color.Transparent)
                        )
                    }

                    // DEFINITIONS Tab
                    val isDefinitionsActive = state.activeTab == TranslatorTab.DEFINITIONS
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onIntent(TranslatorIntent.SelectTab(TranslatorTab.DEFINITIONS)) }
                    ) {
                        Text(
                            text = stringResource(R.string.tab_definitions),
                            color = if (isDefinitionsActive) BrandBlue else TextPrimary.copy(alpha = 0.6f),
                            fontFamily = mainFont,
                            fontSize = 14.sp,
                            fontWeight = if (isDefinitionsActive) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 50.dp, height = 2.dp)
                                .background(if (isDefinitionsActive) BrandBlue else Color.Transparent)
                        )
                    }

                    // SYNONYMS Tab
                    val isSynonymsActive = state.activeTab == TranslatorTab.SYNONYMS
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onIntent(TranslatorIntent.SelectTab(TranslatorTab.SYNONYMS)) }
                    ) {
                        Text(
                            text = stringResource(R.string.tab_synonyms),
                            color = if (isSynonymsActive) BrandBlue else TextPrimary.copy(alpha = 0.6f),
                            fontFamily = mainFont,
                            fontSize = 14.sp,
                            fontWeight = if (isSynonymsActive) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 50.dp, height = 2.dp)
                                .background(if (isSynonymsActive) BrandBlue else Color.Transparent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. DYNAMIC CONTENT BASED ON SELECTED TAB
                when (state.activeTab) {
                    TranslatorTab.TRANSLATION -> {
                        Text(
                            text = state.translatedText.ifBlank { stringResource(R.string.translation_hint) },
                            color = TextPrimary.copy(0.5f),
                            fontFamily = mainFont,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TranslatorTab.DEFINITIONS -> {
                        if (state.definitions.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_definitions),
                                color = TextPrimary.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                        } else {
                            // Green "noun" tag from original design
                            Box(
                                modifier = Modifier
                                    .background(
                                        TagGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.tag_noun),
                                    color = TagGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Render definitions list
                            state.definitions.forEach { definition ->
                                DefinitionItem(text = definition, example = "")
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    TranslatorTab.SYNONYMS -> {
                        if (state.synonyms.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_synonyms),
                                color = TextPrimary.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                        } else {
                            // Render synonyms as a comma-separated list
                            Text(
                                text = state.synonyms.joinToString(separator = ", "),
                                color = TextPrimary,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
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
                    .weight(0.6f)
                    .background(BackgroundGray, shape = TopWaveDesignShape)
            ) {
                Box(modifier = Modifier.padding(top = 30.dp)) {
                    TextToTranslateSection(state, onIntent)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
            ) { TranslationSection(state, onIntent) }
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                BottomLanguageSelector(
                    state = state,
                    onIntent = onIntent
                )
            }
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
            ) { TopNavigationBar() }
            Box(
                modifier = Modifier
                    .weight(0.2f)
                    .height(50.dp)
            )
        }
    }
}

@Composable
fun DefinitionItem(text: String, example: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp, end = 8.dp)
                .size(6.dp)
                .background(TagGreen, shape = CircleShape)
        )
        Column {
            Text(
                text = text,
                color = TextPrimary,
                fontFamily = mainFont,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = example,
                color = TextPrimary.copy(alpha = 0.6f),
                fontFamily = mainFont,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun BottomLanguageSelector(
    state: TranslatorState,
    onIntent: (TranslatorIntent) -> Unit
) {
    val corner = 50
    val panelShape = RoundedCornerShape(corner, corner, 0, 0)

    // Custom smooth S-curve section divider
    val customWaveShape = GenericShape { size, _ ->
        val width = size.width
        val height = size.height
        val midX = width / 2f
        val deltaX = 40f

        moveTo(midX - deltaX, 0f)
        cubicTo(
            midX + deltaX, 0f,
            midX - deltaX, height,
            midX + deltaX, height
        )
        lineTo(width, height)
        lineTo(width, 0f)
        close()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(8.dp, shape = panelShape)
            .background(Color.White, shape = panelShape)
    ) {
        // Blue background for the right side
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandBlue, shape = customWaveShape)
        )

        // Language text
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onIntent(TranslatorIntent.ClickSourceLanguage) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.sourceLanguageName,
                    color = TextDark,
                    fontFamily = mainFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(48.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onIntent(TranslatorIntent.ClickTargetLanguage) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.targetLanguageName,
                    color = Color.White,
                    fontFamily = mainFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center swap button
        Box(
            modifier = Modifier
                .padding(end = 10.dp)
                .align(Alignment.Center)
                .size(40.dp)
                .shadow(4.dp, shape = CircleShape)
                .background(Color.White, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { onIntent(TranslatorIntent.SwapLanguages) },
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_swap),
                    contentDescription = stringResource(R.string.swap_languages_desc),
                    tint = Color.Unspecified, // Keep colors from XML resource
                )
            }
        }
    }

    if (state.isSelectingSourceLang) {
        LanguageSelectionSheet(
            languages = state.availableLanguages,
            onLanguageSelected = { onIntent(TranslatorIntent.SelectSourceLanguage(it)) },
            onDismiss = { onIntent(TranslatorIntent.DismissLanguageSelection) }
        )
    }

    if (state.isSelectingTargetLang) {
        LanguageSelectionSheet(
            languages = state.availableLanguages,
            onLanguageSelected = { onIntent(TranslatorIntent.SelectTargetLanguage(it)) },
            onDismiss = { onIntent(TranslatorIntent.DismissLanguageSelection) }
        )
    }
}

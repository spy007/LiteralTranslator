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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.literaltranslator.ui.theme.TestAppTheme
import androidx.compose.foundation.text.BasicTextField

// Precise color palette from the original design
val BrandBlue = Color(0xFF2962FF)
val BackgroundGray = Color(0xFFF4F6F9)   // Light background for the application
val TextDark = Color(0xFF0D1B2A)
val TextLightGray = Color(0xFF90A4AE)
val TagGreen = Color(0xFF4CAF50)

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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

// Refined shapes for CustomTranslatorDesign with "S-curve" transitions
// Top wave (Blue): Horizontal line with arcs at both ends
val TopWaveDesignShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val curveH = w * 0.15f // Height of the curve transition

    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h - curveH)
    // Right arc (concave)
    cubicTo(
        w, h - curveH * 0.3f,
        w * 0.85f, h,
        w * 0.7f, h
    )
    // Horizontal line
    lineTo(w * 0.3f, h)
    // Left arc (convex)
    cubicTo(
        w * 0.15f, h,
        0f, h + curveH * 0.5f,
        0f, h + curveH
    )
    close()
}

@Preview(showBackground = true)
@Composable
fun CustomTranslatorDesignPreview() {
    TestAppTheme { CustomTranslatorDesign() } }

@Composable
fun CustomTranslatorDesign() {}

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

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TranslatorIntent.TranslatorEffect.ShowToast -> {
                    android.widget.Toast.makeText(
                        context,
                        effect.message,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

                is TranslatorIntent.TranslatorEffect.PlaySpeech -> {
                    ttsManager.speak(effect.text, effect.langCode)
                }

                TranslatorIntent.TranslatorEffect.TriggerHapticFeedback -> {
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
                contentDescription = "Menu",
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
                                contentDescription = "Source speech",
                                tint = TextDark
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.sourceLanguageName,
                            color = TextLightGray,
                            fontSize = 14.sp
                        )
                    }
                    IconButton(
                        onClick = { onIntent(TranslatorIntent.ClearText) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextLightGray
                        )
                    }
                }

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
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextDark,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    decorationBox = { innerTextField ->
                        if (state.inputText.isEmpty()) {
                            Text(
                                text = "Type something...",
                                color = TextLightGray,
                                fontSize = 18.sp
                            )
                        }
                        innerTextField()
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
                            contentDescription = "Target speech",
                            tint = BrandBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.targetLanguageName,
                        color = TextDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Favorites",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    // Use system icon as placeholder for copy
                    Icon(
                        painter = painterResource(id = R.drawable.ic_copy),
                        contentDescription = "Copy",
                        tint = TextLightGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = TextLightGray,
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
                            text = "Translation",
                            color = if (isTranslationActive) BrandBlue else TextLightGray,
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
                            text = "Definitions",
                            color = if (isDefinitionsActive) BrandBlue else TextLightGray,
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
                            text = "Synonyms",
                            color = if (isSynonymsActive) BrandBlue else TextLightGray,
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

                Spacer(modifier = Modifier.height(24.dp))

                // 2. DYNAMIC CONTENT BASED ON SELECTED TAB
                when (state.activeTab) {
                    TranslatorTab.TRANSLATION -> {
                        Text(
                            text = state.translatedText.ifBlank { "Translation will appear here..." },
                            color = if (state.translatedText.isBlank()) TextLightGray else TextDark,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TranslatorTab.DEFINITIONS -> {
                        if (state.definitions.isEmpty()) {
                            Text(
                                text = "No definitions available.",
                                color = TextLightGray,
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
                                    text = "noun",
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
                                text = "No synonyms found.",
                                color = TextLightGray,
                                fontSize = 16.sp
                            )
                        } else {
                            // Render synonyms as a comma-separated list
                            Text(
                                text = state.synonyms.joinToString(separator = ", "),
                                color = TextDark,
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
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                BottomLanguageSelector {}
            }
        }
        Row(modifier = Modifier
            .fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .height(60.dp)
                    .background(BrandBlue, shape = TopWaveDesignShape)
            ) { TopNavigationBar() }
            Box(
                modifier = Modifier
                    .weight(0.2f)
                    .height(60.dp)
            )
        }
    }

 /*
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // 1. BLUE PANEL WITH STEEP WAVE (Bottom-most layer)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp) // Fixed height for deep drop
                .background(BrandBlue, shape = HeaderWaveShape)
        )

        // 2. ALL CONTENT (Layered on top of the blue background, no overlaps)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // WINDOW 1: Text Input (English)
            // Shape exactly matches original design: rounded only at the bottom!
            val inputCardShape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = 32.dp,
                bottomEnd = 32.dp
            )

            // Next is WINDOW 2 (Portuguese, white background)...


            // WINDOW 2: Translation (Portuguese, white background)


            Spacer(modifier = Modifier.height(8.dp))

            // Custom bottom language selector
            BottomLanguageSelector { onIntent(TranslatorIntent.SwapLanguages) }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

  */
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
                color = TextDark,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = example, color = TextLightGray, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun BottomLanguageSelector(onSwapLanguagesClick: () -> Unit) {
    val panelShape = RoundedCornerShape(32.dp)

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
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "English",
                    color = TextDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(48.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "Portuguese",
                    color = Color.White,
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
                onClick = onSwapLanguagesClick,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_swap),
                    contentDescription = "Swap languages",
                    tint = Color.Unspecified, // Keep colors from XML resource
                )
            }
        }
    }
}

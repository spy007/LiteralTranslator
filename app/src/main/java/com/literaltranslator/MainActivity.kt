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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

// Точная цветовая палитра из оригинального дизайна
val BrandBlue = Color(0xFF2962FF)
val BackgroundGray = Color(0xFFF4F6F9)   // Светлый фон приложения
val CardInputGray = Color(0xFFECEFF1)    // Светло-серое окно для английского текста
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
    // Передаем просто готовые текстовые данные, рендеринг займет долю секунды
    TranslatorScreenContent(
        state = TranslatorState(
            inputText = "Hope you like this approach",
            translatedText = "Esperança",
            sourceLanguageName = "English",
            targetLanguageName = "Portuguese"
        ),
        onIntent = {} // Пустая лямбда, клики в превью логику дергать не будут
    )
}

// 1. Кастомная форма верхнего синего фона (узкая плавная волна)
val HeaderWaveShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height * 0.65f)
    cubicTo(
        size.width * 0.7f, size.height * 0.65f,
        size.width * 0.3f, size.height,
        0f, size.height
    )
    close()
}

// 2. Волнообразная форма для Окна ввода (Английский) - волна уходит вверх налево внизу
val InputWaveShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = 60f // Мягкое закругление углов

    moveTo(r, 0f)
    lineTo(w - r, 0f)
    quadraticBezierTo(w, 0f, w, r)
    lineTo(w, h - r)
    quadraticBezierTo(w, h, w - r, h)
    lineTo(w * 0.4f, h)
    cubicTo(w * 0.25f, h, w * 0.15f, h * 0.85f, 0f, h * 0.82f)
    lineTo(0f, r)
    quadraticBezierTo(0f, 0f, r, 0f)
    close()
}

// 3. Волнообразная форма для Окна перевода (Португальский) - стыкуется с верхней волной
val TranslateWaveShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = 60f

    moveTo(0f, 0f)
    cubicTo(w * 0.15f, 0f, w * 0.25f, h * 0.12f, w * 0.4f, h * 0.12f)
    lineTo(w - r, h * 0.12f)
    quadraticBezierTo(w, h * 0.12f, w, h * 0.12f + r)
    lineTo(w, h - r)
    quadraticBezierTo(w, h, w - r, h)
    lineTo(r, h)
    quadraticBezierTo(0f, h, 0f, h - r)
    close()
}

@Composable
fun TranslatorScreen(modifier: Modifier = Modifier) {
    // 1. Создаем ViewModel ТОЛЬКО здесь. Preview сюда заходить не будет.
    val factory = viewModelFactory {
        initializer {
            TranslatorViewModel(repository = TranslationRepositoryImpl())
        }
    }
    val viewModel: TranslatorViewModel = viewModel(factory = factory)
    val state by viewModel.viewState.collectAsState()

    // Слушаем эффекты
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
                    // Код вызова вибрации устройства
                }
                // ИСПРАВЛЕНО: Удален дубликат ветки TriggerHapticFeedback
            }
        }
    }

    // Передаем все чистые данные в Stateless-экран
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
            .background(BackgroundGray)
    ) {
        // Верхний декоративный фон
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(BrandBlue, shape = HeaderWaveShape)
        )

        // Скроллируемый контент экрана
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Верхний бар навигации
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    // Используем стандартную иконку списка (подходит под меню)
                    Icon(
                        painter = painterResource(id = R.drawable.menu),
                        contentDescription = "Меню",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Text translate",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "T",
                        color = BrandBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ОКНО 1: Ввод текста (Английский)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardInputGray, shape = InputWaveShape)
                    .padding(24.dp)
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
                                    contentDescription = "Озвучить исходный текст",
                                    tint = TextDark
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // Динамическое имя исходного языка
                            Text(
                                text = state.sourceLanguageName,
                                color = TextLightGray,
                                fontSize = 14.sp
                            )
                        }
                        // Кнопка очистки текста
                        IconButton(
                            onClick = { onIntent(TranslatorIntent.ClearText) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Очистить",
                                tint = TextLightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Поле ввода текста (вместо статического Row со словом Hope)
                    TextField(
                        value = state.inputText,
                        onValueChange = { newText ->
                            // Отправляем интент во ViewModel, автоперевод сработает сам через 600мс!
                            onIntent(TranslatorIntent.ChangeInputText(newText))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        placeholder = { Text("Type something...", color = TextLightGray) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = TextDark,
                            fontSize = 18.sp,
                            lineHeight = 26.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }

            // ОКНО 2: Перевод (Португальский, белый фон)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-20).dp) // Легкий нахлест для бесшовного соединения волн
                    .background(Color.White, shape = TranslateWaveShape)
                    .padding(24.dp)
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp)) // Компенсируем верхний изгиб волны

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
                                    contentDescription = "Озвучить перевод",
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
                                contentDescription = "Избранное",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            // Используем системную иконку списков как временную замену копированию
                            Icon(
                                painter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = "Копировать",
                                tint = TextLightGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Еще",
                                tint = TextLightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    // ЛОГИКА ОТОБРАЖЕНИЯ: Загрузка (Shimmer) или Текст
                    if (state.isLoading) {
                        // Эффект мерцания пока качается пакет или идет перевод
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

// Внутри ОКНА 2 (Португальский), блок отображения контента ( isLoading == false ):
                    } else {
                        // 1. Переключатель вкладок с динамической подсветкой и кликами
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Вкладка TRANSLATION
                            val isTranslationActive = state.activeTab == TranslatorTab.TRANSLATION
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null // Убираем серый квадрат при клике, чтобы было красиво
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

                            // Вкладка DEFINITIONS
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

                            // Вкладка SYNONYMS
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

                        // 2. ДИНАМИЧЕСКИЙ КОНТЕНТ В ЗАВИСИМОСТИ ОТ ВЫБРАННОЙ ВКЛАДКИ
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
                                    // Зеленый тег Noun как в оригинальном дизайне
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

                                    // Выводим список определений
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
                                    // Отображаем синонимы красивым списком через запятую или плиткой
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

            Spacer(modifier = Modifier.height(8.dp))

            // Наш настроенный нижний переключатель языков
            BottomLanguageSelector { onIntent(TranslatorIntent.SwapLanguages) }

            Spacer(modifier = Modifier.height(24.dp))
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

    // Кастомная плавная S-образная волна раздела по центру
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
        // Синий фон для правой части
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandBlue, shape = customWaveShape)
        )

        // Текст языков
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

        // Центральная кнопка
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
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
                    contentDescription = "Сменить языки",
                    tint = Color.Unspecified, // Сохраняем цвета стрелок из XML
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

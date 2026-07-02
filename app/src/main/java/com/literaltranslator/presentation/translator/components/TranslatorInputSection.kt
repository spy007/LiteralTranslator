package com.literaltranslator.presentation.translator.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.literaltranslator.R
import com.literaltranslator.presentation.translator.TranslatorIntent
import com.literaltranslator.presentation.translator.TranslatorState
import com.literaltranslator.ui.theme.BrandBlue
import com.literaltranslator.ui.theme.TextLightGray
import com.literaltranslator.ui.theme.TextPrimary
import com.literaltranslator.ui.theme.mainFont
import kotlin.math.roundToInt

@Composable
fun TranslatorInputSection(
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp)
    ) {
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
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = stringResource(R.string.clear_desc),
                    tint = TextLightGray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(state.inputText) {
                    detectTapGestures { offset ->
                        textLayoutResult?.let { layout ->
                            val position = layout.getOffsetForPosition(offset)
                            val range = findWordRange(state.inputText, position)
                            onIntent(TranslatorIntent.SelectWord(range))
                        }
                    }
                }
        ) {
            BasicTextField(
                value = state.inputText,
                onValueChange = { onIntent(TranslatorIntent.ChangeInputText(it)) },
                modifier = Modifier.fillMaxWidth(),
                onTextLayout = { textLayoutResult = it },
                visualTransformation = wordTransformation,
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontFamily = mainFont,
                    fontSize = 24.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Normal
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
                    }
                }
            )

            // CONTEXTUAL BUBBLE (POPUP)
            state.selectedWordRange?.let { range ->
                textLayoutResult?.let { layout ->
                    if (range.first in state.inputText.indices && state.selectedWordTranslation.isNotBlank()) {
                        val rect = layout.getBoundingBox(range.first)
                        val density = LocalDensity.current
                        val yOffsetPx = with(density) { -44.dp.toPx() }
                        
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
}

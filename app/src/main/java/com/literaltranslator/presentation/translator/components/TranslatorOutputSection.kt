package com.literaltranslator.presentation.translator.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.literaltranslator.R
import com.literaltranslator.presentation.translator.TranslatorIntent
import com.literaltranslator.presentation.translator.TranslatorState
import com.literaltranslator.ui.theme.TextPrimary
import com.literaltranslator.ui.theme.mainFont
import com.literaltranslator.ui.components.shimmerLoading

@Composable
fun TranslatorOutputSection(
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
            Spacer(modifier = Modifier.height(8.dp)) // Compensate for top curve

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
                    // TODO: Create functional to open favourite words
                    Icon(
                        painter = painterResource(id = R.drawable.ic_star_rounded),
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
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
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
            } else {
                val textColor = if (state.translatedText.isEmpty()) TextPrimary.copy(alpha = 0.5f) else TextPrimary
                Text(
                    text = state.translatedText.ifBlank { stringResource(R.string.translation_hint) },
                    color = textColor,
                    fontFamily = mainFont,
                    fontSize = 24.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

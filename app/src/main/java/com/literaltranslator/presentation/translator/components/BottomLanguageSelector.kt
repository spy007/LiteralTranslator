package com.literaltranslator.presentation.translator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.literaltranslator.R
import com.literaltranslator.presentation.translator.TranslatorIntent
import com.literaltranslator.presentation.translator.TranslatorState
import com.literaltranslator.ui.theme.BrandBlue
import com.literaltranslator.ui.theme.TextPrimary
import com.literaltranslator.ui.theme.mainFont

@Composable
fun BottomLanguageSelector(
    state: TranslatorState,
    onIntent: (TranslatorIntent) -> Unit
) {
    val corner = 50
    val panelShape = RoundedCornerShape(topStart = corner.dp, topEnd = corner.dp)

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
            .height(56.dp)
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
            // TODO: Make shadow for bottom button 'translate to' (try to make shadow how it is made for circle button)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onIntent(TranslatorIntent.ClickSourceLanguage) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.sourceLanguageName,
                    color = TextPrimary,
                    fontFamily = mainFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
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
                    fontWeight = FontWeight.Medium
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

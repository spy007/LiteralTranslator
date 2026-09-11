package com.literaltranslator.presentation.translator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.literaltranslator.presentation.translator.TranslatorState

@Composable
fun FuturisticOutputSection(
    state: TranslatorState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0xFF0066FF).copy(alpha = 0.2f)
            )
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(16.dp)
            .height(180.dp)
    ) {
        if (state.translatedText.isEmpty()) {
            Text(
                text = "Translation will appear here",
                color = Color.Gray.copy(alpha = 0.6f),
                fontSize = 18.sp
            )
        } else {
            Text(
                text = state.translatedText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    color = Color(0xFF1A1C1E)
                )
            )
        }
    }
}

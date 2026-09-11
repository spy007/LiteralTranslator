package com.literaltranslator.presentation.smarthub.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.literaltranslator.ui.theme.TestAppTheme

@Composable
fun TranslationInput(
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF0066FF).copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        if (text.isEmpty()) {
            Text(
                text = "Type here to translate...",
                fontSize = 26.sp,
                color = Color.LightGray
            )
        }
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                fontSize = 26.sp,
                color = Color(0xFF1A1C1E)
            ),
            cursorBrush = SolidColor(Color(0xFF0066FF))
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TranslationInputPreview() {
    TestAppTheme {
        TranslationInput(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
    }
}

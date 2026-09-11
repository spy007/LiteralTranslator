package com.literaltranslator.presentation.smarthub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.literaltranslator.ui.theme.HubBlue
import com.literaltranslator.ui.theme.TestAppTheme
import com.literaltranslator.ui.theme.michromaFont

@Composable
fun LanguageSelection(
    modifier: Modifier = Modifier
) {
    // Greater Oval (Outer Box) - goal is to limit the shadow
    Box(
        modifier = modifier
            .fillMaxWidth()// Increased to accommodate darker shadow
            .wrapContentHeight()
            .clip(RoundedCornerShape(100.dp)), 
        contentAlignment = Alignment.Center
    ) {
        // Main Oval containing the content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(60.dp),
                    spotColor = HubBlue,
                    ambientColor = HubBlue
                )
                .background(Color.White, RoundedCornerShape(60.dp))
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFD000FF),
                            HubBlue
                        )
                    ),
                    shape = RoundedCornerShape(60.dp)
                )
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageItem(name = "English")
                LanguageItem(name = "Spanish")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE5E7EB)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                Text(
                    text = "Language selection".toUpperCase(Locale.current),
                    fontFamily = michromaFont,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(name: String) {
    Text(
        text = name,
        fontFamily = michromaFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1A1C1E)
    )
}

@Preview(showBackground = true)
@Composable
fun LanguageSelectionPreview() {
    TestAppTheme {
        LanguageSelection()
    }
}

package com.literaltranslator.presentation.smarthub

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.literaltranslator.presentation.smarthub.components.HubBottomControl
import com.literaltranslator.presentation.smarthub.components.SmartHubHeader
import com.literaltranslator.presentation.smarthub.components.TranslationInput
import com.literaltranslator.ui.theme.BackgroundGray
import com.literaltranslator.ui.theme.TestAppTheme

@Composable
fun SmartHubScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Custom background shape
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                // Start curve at 70% of the height
                lineTo(w, h * 0.66f)

                // Curve in to the middle at the bottom
                cubicTo(
                    w * 0.8f, h * 0.7f,
                    w * 0.6f, h * 0.7f,
                    w * 0.55f, h * 0.9f
                )

                lineTo(w * 0.55f, h * 0.9f)
                lineTo(w * 0.5f, h * 0.9f)
                lineTo(w * 0.45f, h * 0.89f)

                // Curve back out
                cubicTo(
                    w * 0.42f, h * 0.7f,
                    w * 0.3f, h * 0.7f,
                    0f, h * 0.67f
                )
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        BackgroundGray.copy(alpha = 0.5f)
                    ),
                    startY = 0f,
                    endY = h * 0.85f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmartHubHeader()
            
            // Input field starts below header and finishes at top of the oval
            TranslationInput(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 120.dp)
            )
            
            // Moved slightly higher by using a larger bottom spacer/padding if needed, 
            // but weight(1f) above it already handles the dynamic positioning.
            // Adding some padding to keep it from sticking to the mic button too much.
//            LanguageSelection(
//                modifier = Modifier.padding(bottom = 12.dp)
//            )

            HubBottomControl(
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun SmartHubScreenPreview() {
    TestAppTheme {
        SmartHubScreen()
    }
}

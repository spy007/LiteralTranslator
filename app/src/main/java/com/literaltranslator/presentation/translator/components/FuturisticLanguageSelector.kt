package com.literaltranslator.presentation.translator.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.literaltranslator.presentation.translator.TranslatorState

@Composable
fun FuturisticLanguageSelector(
    state: TranslatorState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp), // Height reduced from 180dp to 90dp (2x smaller)
        contentAlignment = Alignment.Center
    ) {
        // The elliptical background with gradient border
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            val strokeWidth = 2.dp.toPx() // Slightly thinner stroke for smaller size
            val brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF0066FF),
                    Color(0xFFD000FF),
                    Color(0xFF0066FF)
                )
            )
            
            drawOval(
                brush = brush,
                style = Stroke(width = strokeWidth)
            )
            
            // Top dot
            drawCircle(
                color = Color(0xFF0066FF),
                radius = 4.dp.toPx(),
                center = center.copy(y = 0f)
            )
            // Right dot
            drawCircle(
                color = Color(0xFFD000FF),
                radius = 4.dp.toPx(),
                center = center.copy(x = size.width)
            )
            // Bottom dot (larger and white)
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = center.copy(y = size.height)
            )
            // Bottom dot border/glow
            drawCircle(
                color = Color(0xFF0066FF).copy(alpha = 0.3f),
                radius = 12.dp.toPx(),
                center = center.copy(y = size.height),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            modifier = Modifier.padding(top = 4.dp), // Minimal adjustment for alignment
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.sourceLanguageName,
                    fontSize = 18.sp, // Slightly smaller text for smaller oval
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = " \u2192 ",
                    fontSize = 18.sp,
                    color = Color.Black
                )
                Text(
                    text = state.targetLanguageName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp)) // Minimal gap
            
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF0F4F8)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LANGUAGE SELECTION",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

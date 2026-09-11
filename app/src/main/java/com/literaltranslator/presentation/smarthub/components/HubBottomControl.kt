package com.literaltranslator.presentation.smarthub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.literaltranslator.R
import com.literaltranslator.ui.theme.HubBlue
import com.literaltranslator.ui.theme.TestAppTheme

@Composable
fun HubBottomControl(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(130.dp)
            .shadow(
                elevation = 30.dp,
                shape = CircleShape,
                spotColor = Color(0xFF99C7FF),
                ambientColor = Color(0xFF99C7FF)
            )
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        HubBlue.copy(alpha = 0.6f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.5f),
                    ambientColor = Color.Black.copy(alpha = 0.5f)
                )
                .clip(CircleShape)
                .background(
                    Color.White
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        color = Color.White,
                        shape = CircleShape
                    ),
                onClick = {},
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mic_gradient),
                    contentDescription = "Microphone",
                    modifier = Modifier.size(60.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HubBottomControlPreview() {
    TestAppTheme {
        HubBottomControl(
            modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        )
    }
}

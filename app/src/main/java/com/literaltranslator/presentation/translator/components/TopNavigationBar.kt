package com.literaltranslator.presentation.translator.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.literaltranslator.R
import com.literaltranslator.ui.theme.TestAppTheme
import com.literaltranslator.ui.theme.titleBarFont

@Composable
fun TopNavigationBar(
    onFavoritesClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        IconButton(
            onClick = {},
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_menu),
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            color = Color.White,
            // Changed to titleBarFont as requested in TODO for a more stylish look
            fontFamily = titleBarFont,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        IconButton(
            onClick = onFavoritesClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_star),
                contentDescription = "Favorites",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview
@Composable
fun TopNavigationBarPreview() {
    TestAppTheme {
        Surface(color = MaterialTheme.colorScheme.primary) {
            TopNavigationBar(onFavoritesClick = {})
        }
    }
}
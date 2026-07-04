package com.literaltranslator.presentation.translator.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.literaltranslator.R
import com.literaltranslator.ui.theme.mainFont

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
                contentDescription = stringResource(R.string.menu_desc),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            color = Color.White,
            // TODO: change font to my stylish
            fontFamily = mainFont,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
        IconButton(
            onClick = onFavoritesClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_star),
                contentDescription = stringResource(R.string.favorites_desc),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// FIXME: Preview not showing in Design tab
@Preview
@Composable
fun TopNavigationBarPreview() {
    TopNavigationBar(onFavoritesClick = {})
}
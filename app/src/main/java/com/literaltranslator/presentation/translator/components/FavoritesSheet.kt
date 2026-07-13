package com.literaltranslator.presentation.translator.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.literaltranslator.R
import com.literaltranslator.domain.model.FavoriteModel
import com.literaltranslator.presentation.translator.TranslatorIntent
import com.literaltranslator.ui.theme.BrandBlue
import com.literaltranslator.ui.theme.TextPrimary
import com.literaltranslator.ui.theme.mainFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesSheet(
    favorites: List<FavoriteModel>,
    onIntent: (TranslatorIntent) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { onIntent(TranslatorIntent.CloseFavorites) },
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BrandBlue.copy(alpha = 0.5f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.favorites_title),
                fontFamily = mainFont,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.favorites_empty),
                        fontFamily = mainFont,
                        color = TextPrimary.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(favorites) { favorite ->
                        FavoriteItem(
                            favorite = favorite,
                            onClick = { onIntent(TranslatorIntent.SelectFavorite(favorite)) },
                            onDelete = { onIntent(TranslatorIntent.DeleteFavorite(favorite.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteItem(
    favorite: FavoriteModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Color(0xFFF5F5F5),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = favorite.word,
                        fontFamily = mainFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${favorite.sourceLangCode} \u2192 ${favorite.targetLangCode})",
                        fontSize = 12.sp,
                        color = TextPrimary.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = favorite.translation,
                    fontFamily = mainFont,
                    fontSize = 14.sp,
                    color = BrandBlue,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

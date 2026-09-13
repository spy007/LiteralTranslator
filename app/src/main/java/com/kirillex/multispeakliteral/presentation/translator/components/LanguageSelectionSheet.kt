package com.kirillex.multispeakliteral.presentation.translator.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirillex.multispeakliteral.R
import com.kirillex.multispeakliteral.domain.model.LanguageModel
import com.kirillex.multispeakliteral.ui.theme.BrandBlue
import com.kirillex.multispeakliteral.ui.theme.TextPrimary
import com.kirillex.multispeakliteral.ui.theme.mainFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionSheet(
    title: String,
    languages: List<LanguageModel>,
    selectedLanguageCode: String,
    onLanguageSelected: (LanguageModel) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    val filteredLanguages = remember(searchQuery, languages) {
        languages
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
            .sortedBy { it.name }
    }

    // Scroll to selected language on first open if not searching
    LaunchedEffect(languages) {
        if (searchQuery.isEmpty()) {
            val selectedIndex = filteredLanguages.indexOfFirst { it.code == selectedLanguageCode }
            if (selectedIndex >= 0) {
                listState.scrollToItem(selectedIndex)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BrandBlue.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = title,
                fontFamily = mainFont,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = { 
                    Text(
                        stringResource(R.string.search_language_hint), 
                        fontFamily = mainFont,
                        color = TextPrimary.copy(alpha = 0.5f)
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextPrimary.copy(alpha = 0.5f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = TextPrimary.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = TextPrimary.copy(alpha = 0.1f),
                    cursorColor = BrandBlue
                ),
                singleLine = true
            )

            if (filteredLanguages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_languages_found),
                        fontFamily = mainFont,
                        color = TextPrimary.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filteredLanguages, key = { it.code }) { language ->
                        LanguageItem(
                            language = language,
                            isSelected = language.code == selectedLanguageCode,
                            onClick = { onLanguageSelected(language) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageItem(
    language: LanguageModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BrandBlue.copy(alpha = 0.1f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.name,
                fontFamily = mainFont,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) BrandBlue else TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_star), // Reusing star for selection check
                    contentDescription = "Selected",
                    tint = BrandBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

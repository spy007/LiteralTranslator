package com.literaltranslator.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.literaltranslator.presentation.translator.TranslatorScreen
import com.literaltranslator.ui.theme.TestAppTheme

// FIXME: not showing word translation when selecting word to translate (reason: .pointerInput() is not triggering )
// FIXME: fix showing translation of word in bubble
// TODO: change font at top bar for Literal Translation text and for input and out sections
// TODO: create @Preview for every Compose component
// TODO: create UI and logic tests for each layer of architecture
// TODO: close keyboard when press on free space in input section
// TODO: remove icon vertical three dots in output section (because it has no sense to show menu with select all)
// TODO: Create functional to open favourite words
// TODO: menu icon make better
// TODO: Make shadow for bottom button 'translate to'

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding: PaddingValues ->
                    TranslatorScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

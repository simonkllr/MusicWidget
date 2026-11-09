package com.example.musicplayerwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.musicplayerwidget.ui.screens.PlayerScreen
import com.example.musicplayerwidget.ui.theme.MusicPlayerWidgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerWidgetTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlayerScreen()
                }
            }
        }
    }
}

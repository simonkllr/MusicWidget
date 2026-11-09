package com.example.musicplayerwidget.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayerwidget.viewmodel.PlayerViewModel

/**
 * Composable-Funktion zur Darstellung der Haupt-Player-Benutzeroberfläche.
 *
 * @param viewModel Das [PlayerViewModel], welches Zustand und Aktionen bereitstellt.
 */
@Composable
fun PlayerScreen(viewModel: PlayerViewModel = viewModel()) {
    val currentSong = viewModel.currentSong
    val isPlaying by viewModel.isPlaying
    val currentPosition by viewModel.currentPosition

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Album Cover (Zentrale Ressource aus Song)
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(Color.Gray, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = currentSong.coverResId),
                contentDescription = "Album Cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Song Info
        Text(
            text = currentSong.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = currentSong.artist,
            fontSize = 16.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Progress Bar
        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { viewModel.seekTo(it.toInt()) },
            valueRange = 0f..currentSong.duration.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${currentPosition}s", color = Color.LightGray)
            Text("${currentSong.duration}s", color = Color.LightGray)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previousSong() }) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            FloatingActionButton(
                onClick = { viewModel.playPause() },
                containerColor = Color.Red
            ) {
                Icon(
                    imageVector = if (isPlaying)
                        Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { viewModel.nextSong() }) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

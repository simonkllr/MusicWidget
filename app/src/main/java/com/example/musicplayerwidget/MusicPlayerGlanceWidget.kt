package com.example.musicplayerwidget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import com.example.musicplayerwidget.data.model.Song
import com.example.musicplayerwidget.data.repository.MusicPlayerStateRepository
import com.example.musicplayerwidget.data.repository.SongRepository

/**
 * Definiert die Preferences-Keys für den Glance State.
 */
object MusicKeys {
    val SONG_INDEX = intPreferencesKey("song_index")
    val IS_PLAYING = booleanPreferencesKey("is_playing")
}

/**
 * Das Home-Screen-Widget basierend auf Jetpack Compose Glance.
 */
class MusicPlayerGlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val currentIndex = prefs[MusicKeys.SONG_INDEX] ?: 0
            val isPlaying = prefs[MusicKeys.IS_PLAYING] ?: false

            val songs = SongRepository.getAllSongs()
            val song = songs.getOrElse(currentIndex) { songs[0] }

            MusicWidgetContent(song = song, isPlaying = isPlaying)
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            val sampleSong = SongRepository.getAllSongs()[0]
            MusicWidgetContent(song = sampleSong, isPlaying = true)
        }
    }
}

/**
 * Reusable Glance Composable für das Widget-Layout.
 */
@Composable
fun MusicWidgetContent(song: Song, isPlaying: Boolean) {
    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(100.dp)
                .background(androidx.compose.ui.graphics.Color(0xFF222222))
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(song.coverResId),
                    contentDescription = "Album Art",
                    modifier = GlanceModifier.size(64.dp)
                )
                Spacer(modifier = GlanceModifier.width(16.dp))
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = song.title,
                        style = TextStyle(
                            color = ColorProvider(android.R.color.white),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = song.artist,
                        style = TextStyle(
                            color = ColorProvider(android.R.color.white),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Row(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(android.R.drawable.ic_media_previous),
                            contentDescription = "Previous",
                            modifier = GlanceModifier
                                .size(28.dp)
                                .clickable(actionRunCallback<PrevActionCallback>())
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        Image(
                            provider = ImageProvider(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play),
                            contentDescription = "Play/Pause",
                            modifier = GlanceModifier
                                .size(28.dp)
                                .clickable(actionRunCallback<PlayPauseActionCallback>())
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        Image(
                            provider = ImageProvider(android.R.drawable.ic_media_next),
                            contentDescription = "Next",
                            modifier = GlanceModifier
                                .size(28.dp)
                                .clickable(actionRunCallback<NextActionCallback>())
                        )
                    }
                }
            }
        }
    }
}

/**
 * Action-Callback für den "Nächster Song"-Button im Widget.
 */
class NextActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val songs = SongRepository.getAllSongs()
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(MusicPlayerGlanceWidget::class.java)

        var nextIndex = 0
        glanceIds.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                val current = prefs[MusicKeys.SONG_INDEX] ?: 0
                nextIndex = (current + 1) % songs.size
                prefs.toMutablePreferences().apply {
                    this[MusicKeys.SONG_INDEX] = nextIndex
                }
            }
            MusicPlayerGlanceWidget().update(context, id)
        }
        MusicPlayerStateRepository.setSongIndex(context, nextIndex)
    }
}

/**
 * Action-Callback für den "Vorheriger Song"-Button im Widget.
 */
class PrevActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val songs = SongRepository.getAllSongs()
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(MusicPlayerGlanceWidget::class.java)

        var prevIndex = 0
        glanceIds.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                val current = prefs[MusicKeys.SONG_INDEX] ?: 0
                prevIndex = if (current > 0) current - 1 else songs.size - 1
                prefs.toMutablePreferences().apply {
                    this[MusicKeys.SONG_INDEX] = prevIndex
                }
            }
            MusicPlayerGlanceWidget().update(context, id)
        }
        MusicPlayerStateRepository.setSongIndex(context, prevIndex)
    }
}

/**
 * Action-Callback für den "Play/Pause"-Button im Widget.
 */
class PlayPauseActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(MusicPlayerGlanceWidget::class.java)

        var newPlaying = false
        glanceIds.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                val playing = prefs[MusicKeys.IS_PLAYING] ?: false
                newPlaying = !playing
                prefs.toMutablePreferences().apply {
                    this[MusicKeys.IS_PLAYING] = newPlaying
                }
            }
            MusicPlayerGlanceWidget().update(context, id)
        }
        MusicPlayerStateRepository.setIsPlaying(context, newPlaying)
    }
}

/**
 * BroadcastReceiver, der das Glance-Widget registriert und steuert.
 */
class MusicPlayerGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicPlayerGlanceWidget()
}

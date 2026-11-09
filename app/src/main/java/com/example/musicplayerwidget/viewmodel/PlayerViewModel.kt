package com.example.musicplayerwidget.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayerwidget.MusicKeys
import com.example.musicplayerwidget.MusicPlayerGlanceWidget
import com.example.musicplayerwidget.data.model.Song
import com.example.musicplayerwidget.data.repository.MusicPlayerStateRepository
import com.example.musicplayerwidget.data.repository.SongRepository
import kotlinx.coroutines.launch

/**
 * Das ViewModel verwaltet den Zustand des Musik-Players und synchronisiert
 * diesen in Echtzeit mit allen Glance AppWidget Instanzen über [GlanceAppWidgetManager].
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    init {
        MusicPlayerStateRepository.refresh(application)
    }

    private val songs = SongRepository.getAllSongs()

    private val _currentSongIndex = mutableIntStateOf(MusicPlayerStateRepository.songIndex.value)
    val currentSongIndex: State<Int> = _currentSongIndex

    private val _isPlaying = mutableStateOf(MusicPlayerStateRepository.isPlaying.value)
    val isPlaying: State<Boolean> = _isPlaying

    private val _currentPosition = mutableIntStateOf(0)
    val currentPosition: State<Int> = _currentPosition

    /**
     * Liefert das aktuell ausgewählte [Song]-Objekt.
     */
    val currentSong: Song
        get() = songs[_currentSongIndex.intValue]

    /**
     * Schaltet den Wiedergabestatus um und aktualisiert App und Widget.
     */
    fun playPause() {
        val newState = !_isPlaying.value
        _isPlaying.value = newState
        MusicPlayerStateRepository.setIsPlaying(getApplication(), newState)
        triggerWidgetUpdate()
    }

    /**
     * Springt zum nächsten Song und aktualisiert App und Widget.
     */
    fun nextSong() {
        val next = (_currentSongIndex.intValue + 1) % songs.size
        _currentSongIndex.intValue = next
        _currentPosition.intValue = 0
        MusicPlayerStateRepository.setSongIndex(getApplication(), next)
        triggerWidgetUpdate()
    }

    /**
     * Springt zum vorherigen Song und aktualisiert App und Widget.
     */
    fun previousSong() {
        val prev = if (_currentSongIndex.intValue > 0) _currentSongIndex.intValue - 1 else songs.size - 1
        _currentSongIndex.intValue = prev
        _currentPosition.intValue = 0
        MusicPlayerStateRepository.setSongIndex(getApplication(), prev)
        triggerWidgetUpdate()
    }

    /**
     * Setzt die aktuelle Abspielposition in Sekunden.
     */
    fun seekTo(position: Int) {
        _currentPosition.intValue = position
    }

    /**
     * Benachrichtigt alle Glance-Widget-Instanzen und schreibt synchronisiert in deren [PreferencesGlanceStateDefinition].
     */
    private fun triggerWidgetUpdate() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(MusicPlayerGlanceWidget::class.java)
                val nextIndex = _currentSongIndex.intValue
                val playing = _isPlaying.value
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[MusicKeys.SONG_INDEX] = nextIndex
                            this[MusicKeys.IS_PLAYING] = playing
                        }
                    }
                }
                MusicPlayerGlanceWidget().updateAll(context)
            } catch (_: Exception) {}
        }
    }
}

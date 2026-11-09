package com.example.musicplayerwidget.data.repository

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ein zentrales Repository, das den aktuellen Player-Zustand (Song-Index, Play/Pause)
 * in Echtzeit zwischen der Haupt-App (ViewModel) und dem Home-Screen-Widget synchron hält.
 * Verwendet SharedPreferences mit synchronem `commit = true`, um Race Conditions bei Updates zu verhindern.
 */
object MusicPlayerStateRepository {
    private const val PREFS_NAME = "music_player_shared_prefs"
    private const val KEY_SONG_INDEX = "song_index"
    private const val KEY_IS_PLAYING = "is_playing"

    private val _songIndex = MutableStateFlow(0)
    val songIndex: StateFlow<Int> = _songIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /**
     * Lädt den aktuellen Zustand frisch aus den SharedPreferences.
     */
    fun refresh(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _songIndex.value = prefs.getInt(KEY_SONG_INDEX, 0)
        _isPlaying.value = prefs.getBoolean(KEY_IS_PLAYING, false)
    }

    /**
     * Aktualisiert den aktuellen Song-Index und speichert ihn synchron (commit = true).
     */
    fun setSongIndex(context: Context, index: Int) {
        _songIndex.value = index
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit(commit = true) {
            putInt(KEY_SONG_INDEX, index)
        }
    }

    /**
     * Aktualisiert den Wiedergabestatus und speichert ihn synchron (commit = true).
     */
    fun setIsPlaying(context: Context, playing: Boolean) {
        _isPlaying.value = playing
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit(commit = true) {
            putBoolean(KEY_IS_PLAYING, playing)
        }
    }
}

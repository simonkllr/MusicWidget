package com.example.musicplayerwidget.data.repository

import com.example.musicplayerwidget.data.model.Song

/**
 * Ein Repository, das statische Mock-Daten (Beispiel-Songs) für die App
 * und das Home-Screen-Widget bereitstellt.
 */
object SongRepository {
    /**
     * Gibt eine Liste von Beispiel-Songs zurück.
     */
    fun getAllSongs(): List<Song> = listOf(
        Song(1, "Summer Vibes", "Artist A", 240, "cover_1"),
        Song(2, "Night Drive", "Artist B", 210, "cover_2"),
        Song(3, "Urban Groove", "Artist C", 195, "cover_3"),
        Song(4, "Sunset Dreams", "Artist D", 220, "cover_4"),
        Song(5, "Electric Pulse", "Artist E", 260, "cover_5"),
    )
}

package com.example.musicplayerwidget.data.model

import com.example.musicplayerwidget.R

/**
 * Repräsentiert ein einzelnes Musikstück mit allen relevanten Metadaten.
 *
 * @property id Eindeutige ID des Songs.
 * @property title Titel des Liedes.
 * @property artist Name des Künstlers oder der Band.
 * @property duration Spieldauer des Songs in Sekunden.
 * @property coverUrl Name oder Verweis auf die Cover-Ressource (z.B. "cover_1").
 */
data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val duration: Int,
    val coverUrl: String
) {
    /**
     * Zentrale Ermittlung der Cover-Ressource ID für App und Widget.
     */
    val coverResId: Int
        get() = when (coverUrl) {
            "cover_1" -> R.drawable.cover_2
            "cover_2" -> R.drawable.cover_1
            "cover_3" -> R.drawable.cover_4
            "cover_4" -> R.drawable.cover_3
            "cover_5" -> R.drawable.cover_5
            else -> R.drawable.ic_launcher_foreground
        }
}

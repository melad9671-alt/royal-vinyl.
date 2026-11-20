package com.melad.offlinemusic

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri
) {
    val durationLabel: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
        }
}

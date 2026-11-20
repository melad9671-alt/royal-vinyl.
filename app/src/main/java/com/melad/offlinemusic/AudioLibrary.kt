package com.melad.offlinemusic

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

object AudioLibrary {
    fun scan(context: Context): List<Track> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val tracks = mutableListOf<Track>()

        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val rawArtist = cursor.getString(artistColumn)
                tracks += Track(
                    id = id,
                    title = cursor.getString(titleColumn)?.takeIf(String::isNotBlank) ?: "Unknown title",
                    artist = rawArtist?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown artist",
                    album = cursor.getString(albumColumn)?.takeIf(String::isNotBlank) ?: "Unknown album",
                    durationMs = cursor.getLong(durationColumn),
                    uri = ContentUris.withAppendedId(collection, id)
                )
            }
        }
        return tracks
    }
}

package com.royalvinyl.player

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

private val Bg = Color(0xFF090610)
private val Panel = Color(0xFF171020)
private val Raised = Color(0xFF21182D)
private val Purple = Color(0xFF8155D9)
private val Lavender = Color(0xFFD6C4FF)
private val Ink = Color(0xFFF5F1FC)
private val Muted = Color(0xFFB3A8C4)

private data class Track(val id: Long, val title: String, val artist: String, val duration: Long, val uri: Uri)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 6, 16)
        window.navigationBarColor = android.graphics.Color.rgb(9, 6, 16)
        setContent { PlayerScreen() }
    }
}

@Composable
private fun PlayerScreen() {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var tracks by remember { mutableStateOf(emptyList<Track>()) }
    var selected by remember { mutableStateOf<Track?>(null) }
    var query by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf(false) }
    var allowed by remember { mutableStateOf(hasPermission(context)) }
    var tab by remember { mutableStateOf("Music") }
    var favorites by remember {
        mutableStateOf(context.getSharedPreferences("royal-vinyl", 0).getStringSet("favorites", emptySet())?.toSet() ?: emptySet())
    }
    fun scan() { tracks = scanAudio(context); allowed = true }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted
        if (granted) scan()
    }
    LaunchedEffect(Unit) { if (allowed) scan() }
    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    fun start(track: Track) {
        selected = track
        player.setMediaItem(MediaItem.fromUri(track.uri))
        player.prepare()
        player.play()
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Purple, onBackground = Ink, onSurface = Ink)) {
        Column(Modifier.fillMaxSize().background(Bg)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text("ROYAL VINYL", color = Lavender, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("YOUR SOUND. YOUR SPACE.", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
            }
            when (tab) {
                "Music" -> {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("YOUR LIBRARY", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text(tracks.size.toString() + " tracks on this device", color = Muted, fontSize = 12.sp)
                        }
                        TextButton(onClick = { if (allowed) scan() else permissionRequest.launch(audioPermission()) }) { Text("RESCAN", color = Lavender) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp).background(Panel, RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⌕", color = Lavender, fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(value = query, onValueChange = { query = it }, singleLine = true,
                            textStyle = TextStyle(color = Ink, fontSize = 14.sp), modifier = Modifier.weight(1f),
                            decorationBox = { inner -> if (query.isEmpty()) Text("Search songs and artists", color = Muted); inner() })
                    }
                    Spacer(Modifier.height(10.dp))
                    when {
                        !allowed -> EmptyPanel("ALLOW MUSIC ACCESS", "Grant access to scan songs stored on your phone.", "GRANT ACCESS") { permissionRequest.launch(audioPermission()) }
                        tracks.isEmpty() -> EmptyPanel("NO LOCAL TRACKS", "Audio files on your device will appear here.", "SCAN AGAIN", ::scan)
                        else -> {
                            val filtered = tracks.filter { (it.title + " " + it.artist).contains(query, true) }
                            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                items(filtered, key = { it.id }) { track ->
                                    Row(Modifier.fillMaxWidth().background(if (selected?.id == track.id) Raised else Panel, RoundedCornerShape(13.dp)).clickable { start(track) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("♫", color = Lavender, fontSize = 24.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(track.title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(track.artist, color = Muted, fontSize = 12.sp)
                                        }
                                        TextButton(onClick = {
                                            val key = track.id.toString()
                                            favorites = if (favorites.contains(key)) favorites - key else favorites + key
                                            context.getSharedPreferences("royal-vinyl", 0).edit().putStringSet("favorites", favorites).apply()
                                        }) { Text(if (favorites.contains(track.id.toString())) "♥" else "♡", color = Lavender) }
                                        Text(formatDuration(track.duration), color = Muted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                "Movies" -> EmptyPanel("MOVIES", "A new experience is on the way.", "COMING SOON") {}
                else -> Column(Modifier.padding(20.dp)) {
                    Text("SETTINGS", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Your music library is scanned directly from this device.", color = Muted)
                    TextButton(onClick = ::scan) { Text("RESCAN DEVICE", color = Lavender) }
                }
            }
            selected?.let { track ->
                Row(Modifier.fillMaxWidth().padding(10.dp).background(Raised, RoundedCornerShape(14.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = Muted, fontSize = 11.sp)
                    }
                    TextButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) { Text(if (playing) "Ⅱ" else "▶", color = Lavender, fontSize = 20.sp) }
                }
            }
            Row(Modifier.fillMaxWidth().background(Panel).padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Music", "Movies", "Settings").forEach { item ->
                    Column(Modifier.weight(1f).clickable { tab = item }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(when (item) { "Music" -> "♫"; "Movies" -> "▶"; else -> "⚙" }, color = if (tab == item) Lavender else Muted)
                        Text(item, color = if (tab == item) Lavender else Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPanel(title: String, detail: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp).background(Panel, RoundedCornerShape(18.dp)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("◉", color = Purple, fontSize = 48.sp)
        Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = Purple)) { Text(action, color = Ink) }
    }
}

private fun audioPermission() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
private fun hasPermission(context: Context) = ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED

private fun scanAudio(context: Context): List<Track> {
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION)
    val result = mutableListOf<Track>()
    try {
        context.contentResolver.query(collection, projection, MediaStore.Audio.Media.IS_MUSIC + " != 0", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                result.add(Track(id, cursor.getString(titleCol) ?: "Unknown title", cursor.getString(artistCol)?.takeUnless { it == "<unknown>" } ?: "Unknown artist", cursor.getLong(durationCol), ContentUris.withAppendedId(collection, id)))
            }
        }
    } catch (_: Exception) { return emptyList() }
    return result
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000).toInt()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

package com.melad.offlinemusic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

private val Background = Color(0xFF090610)
private val SurfaceDark = Color(0xFF171020)
private val SurfaceRaised = Color(0xFF21182D)
private val Purple = Color(0xFF8155D9)
private val Lavender = Color(0xFFD8C7FF)
private val MainText = Color(0xFFF7F3FC)
private val MutedText = Color(0xFFB4A9C2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 6, 16)
        window.navigationBarColor = android.graphics.Color.rgb(9, 6, 16)
        setContent { OfflineMusicApp() }
    }
}

private enum class LibraryTab(val label: String) {
    SONGS("Songs"), FAVORITES("Favorites"), SETTINGS("Settings")
}

@Composable
private fun OfflineMusicApp() {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var library by remember { mutableStateOf(emptyList<Track>()) }
    val latestLibrary by rememberUpdatedState(library)
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(LibraryTab.SONGS) }
    var permissionGranted by remember { mutableStateOf(hasAudioPermission(context)) }
    var favorites by remember {
        mutableStateOf(
            context.getSharedPreferences("offline_music", 0)
                .getStringSet("favorites", emptySet())
                ?.toSet() ?: emptySet()
        )
    }
    var position by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var shuffle by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(Player.REPEAT_MODE_OFF) }

    fun scanLibrary() {
        library = runCatching { AudioLibrary.scan(context) }.getOrDefault(emptyList())
        permissionGranted = true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) scanLibrary()
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) scanLibrary()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentTrack = latestLibrary.firstOrNull { it.id.toString() == mediaItem?.mediaId }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = player.duration.coerceAtLeast(0L).toFloat()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, isPlaying, currentTrack) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L).toFloat()
            duration = player.duration.coerceAtLeast(0L).toFloat()
            delay(if (isPlaying) 500 else 1000)
        }
    }

    fun playTrack(track: Track) {
        val index = library.indexOfFirst { it.id == track.id }
        if (index < 0) return
        currentTrack = track
        val queue = library.map { track ->
            MediaItem.Builder().setMediaId(track.id.toString()).setUri(track.uri).build()
        }
        player.setMediaItems(queue, index, 0L)
        player.prepare()
        player.play()
    }

    fun toggleFavorite(track: Track) {
        val key = track.id.toString()
        val updated = if (favorites.contains(key)) favorites - key else favorites + key
        favorites = updated
        context.getSharedPreferences("offline_music", 0)
            .edit().putStringSet("favorites", updated).apply()
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Background,
        surface = SurfaceDark,
        primary = Purple,
        onBackground = MainText,
        onSurface = MainText
    )) {
        Column(
            modifier = Modifier.fillMaxSize().background(Background)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Header()
            when (selectedTab) {
                LibraryTab.SONGS -> {
                    LibraryHeading(library.size) { if (permissionGranted) scanLibrary() else permissionLauncher.launch(audioPermission()) }
                    SearchField(search, onSearchChange = { search = it })
                    if (!permissionGranted) {
                        EmptyState("MUSIC ACCESS", "Allow access so the app can find audio files stored on your phone.", "GRANT ACCESS") {
                            permissionLauncher.launch(audioPermission())
                        }
                    } else {
                        val visibleTracks = library.filter {
                            "${it.title} ${it.artist} ${it.album}".contains(search.trim(), ignoreCase = true)
                        }
                        if (visibleTracks.isEmpty()) {
                            EmptyState(
                                if (library.isEmpty()) "YOUR LIBRARY IS EMPTY" else "NO MATCHES",
                                if (library.isEmpty()) "Audio files on your device will appear here after scanning." else "Try another song, artist, or album name.",
                                "SCAN AGAIN"
                            ) { scanLibrary() }
                        } else {
                            TrackList(visibleTracks, currentTrack, favorites, ::playTrack, ::toggleFavorite)
                        }
                    }
                }
                LibraryTab.FAVORITES -> {
                    SectionHeading("YOUR FAVORITES", "${favorites.size} saved tracks")
                    val favoriteTracks = library.filter { favorites.contains(it.id.toString()) }
                    if (favoriteTracks.isEmpty()) {
                        EmptyState("NO FAVORITES YET", "Tap the heart beside a song to keep it here.", "BROWSE SONGS") {
                            selectedTab = LibraryTab.SONGS
                        }
                    } else {
                        TrackList(favoriteTracks, currentTrack, favorites, ::playTrack, ::toggleFavorite)
                    }
                }
                LibraryTab.SETTINGS -> {
                    SectionHeading("SETTINGS", "A simple player for your own music")
                    Column(Modifier.padding(18.dp)) {
                        SettingRow("Music library", "Scans audio stored on this device")
                        SettingRow("Favorites", "Saved locally on this phone")
                        SettingRow("Internet", "Not required for local playback")
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { if (permissionGranted) scanLibrary() else permissionLauncher.launch(audioPermission()) },
                            colors = ButtonDefaults.buttonColors(containerColor = Purple)
                        ) { Text("RESCAN MUSIC LIBRARY") }
                    }
                }
            }

            Spacer(Modifier.weight(1f, fill = false))
            currentTrack?.let {
                PlayerPanel(
                    track = it,
                    playing = isPlaying,
                    position = position,
                    duration = duration,
                    shuffle = shuffle,
                    repeatMode = repeatMode,
                    onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                    onPrevious = { player.seekToPreviousMediaItem() },
                    onNext = { player.seekToNextMediaItem() },
                    onSeek = { player.seekTo(it.toLong()) },
                    onShuffle = {
                        shuffle = !shuffle
                        player.shuffleModeEnabled = shuffle
                    },
                    onRepeat = {
                        repeatMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        player.repeatMode = repeatMode
                    }
                )
            }
            BottomNavigation(selectedTab) { selectedTab = it }
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("OFFLINE MUSIC", color = Lavender, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        Text("YOUR SOUND. YOUR SPACE.", color = MutedText, fontSize = 10.sp, letterSpacing = 2.sp)
    }
}

@Composable
private fun LibraryHeading(count: Int, onRescan: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("YOUR LIBRARY", color = MainText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("$count tracks on this device", color = MutedText, fontSize = 12.sp)
        }
        TextButton(onClick = onRescan) { Text("RESCAN", color = Lavender) }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(title, color = MainText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MutedText, fontSize = 12.sp)
    }
}

@Composable
private fun SearchField(value: String, onSearchChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
            .background(SurfaceDark, RoundedCornerShape(15.dp)).padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⌕", color = Lavender, fontSize = 23.sp)
        Spacer(Modifier.size(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Search songs, artists, albums", color = MutedText, fontSize = 14.sp)
            BasicTextField(
                value = value,
                onValueChange = onSearchChange,
                singleLine = true,
                textStyle = TextStyle(color = MainText, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Text("×", color = MutedText, modifier = Modifier.clickable { onSearchChange("") })
        }
    }
}

@Composable
private fun ColumnScope.TrackList(
    tracks: List<Track>,
    currentTrack: Track?,
    favorites: Set<String>,
    onPlay: (Track) -> Unit,
    onFavorite: (Track) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            Row(
                Modifier.fillMaxWidth()
                    .background(if (currentTrack?.id == track.id) SurfaceRaised else SurfaceDark, RoundedCornerShape(14.dp))
                    .clickable { onPlay(track) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(42.dp).background(SurfaceRaised, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("♫", color = Lavender, fontSize = 22.sp) }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = MainText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = MutedText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = { onFavorite(track) }) {
                    Text(if (favorites.contains(track.id.toString())) "♥" else "♡", color = Lavender, fontSize = 19.sp)
                }
                Text(track.durationLabel, color = MutedText, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PlayerPanel(
    track: Track,
    playing: Boolean,
    position: Float,
    duration: Float,
    shuffle: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
            .background(SurfaceRaised, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(SurfaceDark, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Text("♫", color = Lavender, fontSize = 22.sp)
            }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, color = MainText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = MutedText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (playing) "PLAYING" else "PAUSED", color = Lavender, fontSize = 9.sp, letterSpacing = 1.sp)
        }
        Slider(
            value = if (duration > 0f) position.coerceIn(0f, duration) else 0f,
            onValueChange = onSeek,
            valueRange = 0f..duration.coerceAtLeast(1f),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Lavender,
                activeTrackColor = Purple,
                inactiveTrackColor = MutedText
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position), color = MutedText, fontSize = 10.sp)
            Text(formatTime(duration), color = MutedText, fontSize = 10.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onShuffle) { Text("SHUFFLE", color = if (shuffle) Lavender else MutedText, fontSize = 10.sp) }
            TextButton(onClick = onPrevious) { Text("|◀", color = MainText, fontSize = 18.sp) }
            Surface(
                modifier = Modifier.size(46.dp).clickable(onClick = onPlayPause),
                shape = CircleShape,
                color = Purple
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (playing) "Ⅱ" else "▶", color = MainText, fontSize = 20.sp)
                }
            }
            TextButton(onClick = onNext) { Text("▶|", color = MainText, fontSize = 18.sp) }
            TextButton(onClick = onRepeat) {
                Text(
                    when (repeatMode) {
                        Player.REPEAT_MODE_ALL -> "REPEAT"
                        Player.REPEAT_MODE_ONE -> "REPEAT 1"
                        else -> "REPEAT"
                    },
                    color = if (repeatMode == Player.REPEAT_MODE_OFF) MutedText else Lavender,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, description: String, action: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp).background(SurfaceDark, RoundedCornerShape(18.dp)).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("◉", color = Purple, fontSize = 42.sp)
        Text(title, color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(description, color = MutedText, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
            Text(action, color = MainText)
        }
    }
}

@Composable
private fun SettingRow(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(title, color = MainText, fontWeight = FontWeight.SemiBold)
        Text(description, color = MutedText, fontSize = 12.sp)
    }
}

@Composable
private fun BottomNavigation(selected: LibraryTab, onSelect: (LibraryTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(SurfaceDark).padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LibraryTab.entries.forEach { tab ->
            Column(
                Modifier.weight(1f).clickable { onSelect(tab) }.padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    when (tab) {
                        LibraryTab.SONGS -> "♫"
                        LibraryTab.FAVORITES -> "♥"
                        LibraryTab.SETTINGS -> "⚙"
                    },
                    color = if (selected == tab) Lavender else MutedText,
                    fontSize = 18.sp
                )
                Text(tab.label, color = if (selected == tab) Lavender else MutedText, fontSize = 10.sp)
            }
        }
    }
}

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED

private fun formatTime(milliseconds: Float): String {
    val seconds = (milliseconds.toLong() / 1000).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

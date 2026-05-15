package com.example.reproductor.presentation.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.reproductor.domain.model.Song
import com.example.reproductor.presentation.components.MostPlayedSection
import com.example.reproductor.presentation.components.QueueBottomSheet
import com.example.reproductor.presentation.components.SongOptionsSheet
import com.example.reproductor.presentation.components.toMostPlayedTrack
import com.example.reproductor.presentation.components.formatDuration
import com.example.reproductor.presentation.components.rememberArtworkRequest
import com.example.reproductor.presentation.library.LibraryViewModel
import com.example.reproductor.presentation.player.PlayerViewModel
import androidx.media3.common.Player
import kotlinx.coroutines.launch
import com.example.reproductor.ui.theme.AccentLime
import com.example.reproductor.ui.theme.CardGradientEnd
import com.example.reproductor.ui.theme.CardGradientStart
import com.example.reproductor.ui.theme.CoverGradientEnd
import com.example.reproductor.ui.theme.CoverGradientStart
import com.example.reproductor.ui.theme.PlayerBackground
import com.example.reproductor.ui.theme.RecentCardBg
import com.example.reproductor.ui.theme.TextMuted
import com.example.reproductor.ui.theme.TextSubtle

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
    val repeatMode by playerViewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleModeEnabled by playerViewModel.shuffleModeEnabled.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsStateWithLifecycle()
    val recentlyPlayedSongs by viewModel.recentlyPlayedSongs.collectAsStateWithLifecycle()

    val recentlyAddedSongs = remember(songs) { songs.take(10) }

    val mostPlayedTracks = remember(mostPlayedSongs) {
        mostPlayedSongs.map { it.toMostPlayedTrack() }
    }

    var showQueueSheet by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.refreshMusicOnFirstSessionEntry() }

    val currentSong = playerState.currentSong
    val featured = currentSong ?: songs.firstOrNull()
    val isPlaying = playerState.isPlaying
    val featuredArtRequest = rememberArtworkRequest(
        data = featured?.albumArt,
        size = 90.dp
    )

    var selectedSong by remember { mutableStateOf<Song?>(null) }

    val onSongClick = remember(viewModel, onNavigateToPlayer) {
        { song: Song ->
            viewModel.playSong(song)
            onNavigateToPlayer()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayerBackground)
                .windowInsetsPadding(WindowInsets.systemBars),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
        // ── Block 1: Header ──
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(48.dp))
                Text(
                    text = "BUENOS DÍAS",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Reproduciendo",
                    color = Color(0xFFF0F0F8),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp),
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Block 2: Now Playing Card ──
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { if (featured != null) onSongClick(featured) },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(CardGradientStart, CardGradientEnd)
                            )
                        )
                        .padding(20.dp)
                ) {
                    // Album art
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CoverGradientStart, CoverGradientEnd)
                                )
                            )
                    ) {
                        if (featured?.albumArt != null) {
                            AsyncImage(
                                model = featuredArtRequest,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Status label
                    Text(
                        text = "EN REPRODUCCIÓN",
                        color = CoverGradientStart,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.height(4.dp))

                    // Song title
                    Text(
                        text = featured?.title ?: "Sin canciones",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Artist · Album
                    Text(
                        text = if (featured != null) {
                            "${featured.artist} · ${featured.album}"
                        } else {
                            "Escanea tu biblioteca"
                        },
                        color = TextSubtle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(16.dp))

                    // Subtle divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(TextMuted.copy(alpha = 0.25f))
                    )

                    Spacer(Modifier.height(16.dp))

                    // Playback controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Repeat
                        IconButton(
                            onClick = { playerViewModel.toggleRepeatMode() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            val (icon, tint) = when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne to AccentLime
                                Player.REPEAT_MODE_ALL -> Icons.Default.Repeat to AccentLime
                                else -> Icons.Default.Replay to TextMuted
                            }
                            Icon(
                                icon,
                                contentDescription = "Repetir",
                                tint = tint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Previous
                        IconButton(
                            onClick = { playerViewModel.skipToPrevious() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Anterior",
                                tint = TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Play / Pause — Dominant visual center
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AccentLime)
                                .clickable { playerViewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = Color.Black,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Next
                        IconButton(
                            onClick = { playerViewModel.skipToNext() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Siguiente",
                                tint = TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Shuffle
                        IconButton(
                            onClick = { playerViewModel.toggleShuffleMode() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Aleatorio",
                                tint = if (shuffleModeEnabled) AccentLime else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
        }

        // ── Block 2.5: Most Played Section ──
        if (mostPlayedTracks.isNotEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    MostPlayedSection(
                        tracks = mostPlayedTracks,
                        onItemClick = { songId ->
                            val startIndex = mostPlayedSongs.indexOfFirst { it.id == songId }
                                .coerceAtLeast(0)
                            viewModel.playSongs(mostPlayedSongs, startIndex)
                            onNavigateToPlayer()
                        }
                    )
                }
                Spacer(Modifier.height(36.dp))
            }
        }

        // ── Block 3: Recently Added Section ──
        if (recentlyAddedSongs.isNotEmpty()) {
            item {
                HorizontalSongSection(
                    title = "Últimos agregados",
                    songs = recentlyAddedSongs,
                    onSongClick = { song ->
                        val startIndex = recentlyAddedSongs.indexOfFirst { it.id == song.id }
                            .coerceAtLeast(0)
                        viewModel.playSongs(recentlyAddedSongs, startIndex)
                        onNavigateToPlayer()
                    },
                    onSongLongClick = { selectedSong = it }
                )
                Spacer(Modifier.height(36.dp))
            }
        }

        // ── Block 4: Recently Played Section ──
        if (recentlyPlayedSongs.isNotEmpty()) {
            item {
                HorizontalSongSection(
                    title = "Reproducciones recientes",
                    songs = recentlyPlayedSongs,
                    onSongClick = { song ->
                        val startIndex = recentlyPlayedSongs.indexOfFirst { it.id == song.id }
                            .coerceAtLeast(0)
                        viewModel.playSongs(recentlyPlayedSongs, startIndex)
                        onNavigateToPlayer()
                    },
                    onSongLongClick = { selectedSong = it }
                )
                Spacer(Modifier.height(36.dp))
            }
        }

        // Bottom spacer for navigation
        item { Spacer(Modifier.height(100.dp)) }
    }


    }

    // ── Song Options Sheet ────────────────────────────────────────────────────
    selectedSong?.let { song ->
        val songIndex = songs.indexOf(song).coerceAtLeast(0)
        // Use a repeating 6-color gradient palette
        val gradientPalette = listOf(
            listOf(Color(0xFF7B61FF), Color(0xFFFF5F7E)),
            listOf(Color(0xFF4FD5FF), Color(0xFF1A6AFF)),
            listOf(Color(0xFF00C896), Color(0xFF4FD5FF)),
            listOf(Color(0xFFFFCC4F), Color(0xFFFF6B4A)),
            listOf(Color(0xFFFF73C2), Color(0xFF9C7BFF)),
            listOf(Color(0xFF4FA0FF), Color(0xFF00C896)),
        )
        SongOptionsSheet(
            song = song,
            playlists = playlists,
            coverGradient = gradientPalette[songIndex % gradientPalette.size],
            onDismiss = { selectedSong = null },
            onPlayNext = {
                viewModel.playNext(song)
                selectedSong = null
            },
            onAddToQueue = {
                viewModel.addToQueue(song)
                selectedSong = null
            },
            onAddToPlaylist = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, song.id)
                selectedSong = null
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(song.id)
                selectedSong = null
            }
        )
    }

    // ── Queue bottom sheet ───────────────────────────────────────
    if (showQueueSheet) {
        QueueBottomSheet(
            queue = playerState.queue,
            currentIndex = playerState.currentIndex,
            sheetState = queueSheetState,
            onDismiss = { showQueueSheet = false },
            onSkipToIndex = { index ->
                playerViewModel.skipToIndex(index)
                scope.launch { queueSheetState.hide() }.invokeOnCompletion {
                    showQueueSheet = false
                }
            },
            onRemoveAt = { index -> playerViewModel.removeFromQueue(index) },
            onMoveItem = { from, to -> playerViewModel.moveQueueItem(from, to) },
            onClearQueue = { playerViewModel.clearQueue() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalSongSection(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                HorizontalSongCard(
                    song = song,
                    onClick = { onSongClick(song) },
                    onLongClick = { onSongLongClick(song) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalSongCard(
    song: Song,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val albumArtRequest = rememberArtworkRequest(
        data = song.albumArt,
        size = 130.dp
    )

    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(CoverGradientStart, CoverGradientEnd)
                    )
                )
        ) {
            if (!song.albumArt.isNullOrBlank()) {
                AsyncImage(
                    model = albumArtRequest,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // Play indicator inside cover (bottom right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = song.title,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = TextSubtle,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

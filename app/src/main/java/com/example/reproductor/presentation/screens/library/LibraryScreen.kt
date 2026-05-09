package com.example.reproductor.presentation.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.reproductor.domain.model.Song
import com.example.reproductor.presentation.components.SongOptionsSheet
import com.example.reproductor.presentation.components.rememberArtworkRequest
import com.example.reproductor.presentation.library.LibraryFilter
import com.example.reproductor.presentation.library.LibraryViewModel



@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var selectedSong by remember { mutableStateOf<Song?>(null) }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refreshMusic() },
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060609))
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("Biblioteca", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF171722))
                    .clickable { onNavigateToSearch() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF6B6B85), modifier = Modifier.size(16.dp))
                Text("Buscar canciones...", color = Color(0xFF6B6B85), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                FilterChipLike("Todas", selected = selectedFilter == LibraryFilter.TODAS, onClick = { viewModel.setFilter(LibraryFilter.TODAS) })
                FilterChipLike("Recientes", selected = selectedFilter == LibraryFilter.RECIENTES, onClick = { viewModel.setFilter(LibraryFilter.RECIENTES) })
                FilterChipLike("Favoritas", selected = selectedFilter == LibraryFilter.FAVORITAS, onClick = { viewModel.setFilter(LibraryFilter.FAVORITAS) })
            }
            Text("${filteredSongs.size} canciones", color = Color(0xFF6B6B85), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            error?.let { errorMessage ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF321C24))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFFC7D4),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            viewModel.clearError()
                            viewModel.refreshMusic()
                        }
                    ) { Text("Reintentar", color = Color.White) }
                }
            }
        }

        itemsIndexed(filteredSongs, key = { _, s -> s.id }, contentType = { _, _ -> "SongItem" }) { index, song ->
            val albumArtRequest = rememberArtworkRequest(
                data = song.albumArt,
                size = 42.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            viewModel.playSongs(filteredSongs, index)
                            onNavigateToPlayer()
                        },
                        onLongClick = { selectedSong = song }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${index + 1}",
                    color = Color(0xFF3A3A50),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFF171722)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!song.albumArt.isNullOrBlank()) {
                        AsyncImage(
                            model = albumArtRequest,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF6B6B85),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(song.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(song.artist, color = Color(0xFF6B6B85), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // More options button
                IconButton(
                    onClick = { selectedSong = song },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color(0xFF6B6B85),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

            item { Spacer(Modifier.height(90.dp)) }
        }
    } // end PullToRefreshBox

    // Song Options Sheet
    selectedSong?.let { song ->
        val songIndex = filteredSongs.indexOf(song).coerceAtLeast(0)
        SongOptionsSheet(
            song = song,
            playlists = playlists,
            coverGradient = null,
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
            // onRemoveFromPlaylist is null here (not a playlist context)
        )
    }
}

@Composable
private fun FilterChipLike(label: String, selected: Boolean = false, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFFE8FF47) else Color(0xFF171722))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.Black else Color(0xFF6B6B85),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

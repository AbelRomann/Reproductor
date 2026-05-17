package com.example.reproductor.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reproductor.domain.model.Album
import com.example.reproductor.domain.model.Playlist
import com.example.reproductor.domain.model.Song
import com.example.reproductor.domain.repository.MusicRepository
import com.example.reproductor.domain.usecase.GetAlbumsUseCase
import com.example.reproductor.domain.usecase.GetAllSongsUseCase
import com.example.reproductor.domain.usecase.RefreshMusicUseCase
import com.example.reproductor.presentation.player.MusicPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

enum class LibraryFilter { TODAS, RECIENTES, FAVORITAS }

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val getAlbumsUseCase: GetAlbumsUseCase,
    private val refreshMusicUseCase: RefreshMusicUseCase,
    private val musicRepository: MusicRepository,
    private val playerController: MusicPlayerController
) : ViewModel() {

    companion object {
        private val initialRefreshLock = Any()

        @Volatile
        private var initialRefreshTriggeredInSession: Boolean = false
    }

    private val refreshMutex = Mutex()
    private var refreshJob: Job? = null

    val songs: StateFlow<List<Song>> = getAllSongsUseCase()
        .catch { e ->
            _error.value = "Error al cargar canciones: ${e.message}"
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val favoriteSongs = musicRepository.getFavoriteSongs()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mostPlayedSongs: StateFlow<List<Song>> = musicRepository.getMostPlayedSongs(20)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyPlayedSongs: StateFlow<List<Song>> = musicRepository.getRecentlyPlayedSongs(15)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedFilter = MutableStateFlow(LibraryFilter.TODAS)
    val selectedFilter: StateFlow<LibraryFilter> = _selectedFilter.asStateFlow()

    val filteredSongs: StateFlow<List<Song>> = combine(
        songs,
        favoriteSongs,
        _selectedFilter
    ) { allSongs, favorites, filter ->
        when (filter) {
            LibraryFilter.TODAS -> allSongs
            LibraryFilter.RECIENTES -> allSongs.take(30)
            LibraryFilter.FAVORITAS -> favorites
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<Album>> = getAlbumsUseCase()
        .onEach { albumList ->
            if (_isLoading.value && albumList.isNotEmpty()) {
                _isLoading.value = false
            }
        }
        .catch { e ->
            _error.value = "Error al cargar álbumes: ${e.message}"
            _isLoading.value = false
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<Playlist>> = musicRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _lightListModeEnabled = MutableStateFlow(true)
    val lightListModeEnabled: StateFlow<Boolean> = _lightListModeEnabled.asStateFlow()

    fun setFilter(filter: LibraryFilter) {
        _selectedFilter.value = filter
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(songId)
        }
    }

    fun updateCustomAlbumArt(songId: Long, albumArtUri: String?) {
        viewModelScope.launch {
            musicRepository.updateCustomAlbumArt(songId, albumArtUri)
        }
    }

    fun getSongsForPlaylist(playlistId: Long) = musicRepository.getSongsInPlaylist(playlistId)

    fun getSongsByArtistName(artistName: String) = musicRepository.getSongsByArtistName(artistName)

    fun refreshMusicOnFirstSessionEntry() {
        if (initialRefreshTriggeredInSession) return
        synchronized(initialRefreshLock) {
            if (initialRefreshTriggeredInSession) return
            initialRefreshTriggeredInSession = true
        }
        refreshMusic()
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            musicRepository.createPlaylist(name.trim())
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            musicRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            musicRepository.addSongsToPlaylist(playlistId, songIds)
        }
    }

    fun createPlaylistAndAddSongs(name: String, songIds: List<Long>) {
        if (name.isBlank() || songIds.isEmpty()) return
        viewModelScope.launch {
            musicRepository.createPlaylistAndAddSongs(name.trim(), songIds)
        }
    }

    fun moveSongInPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            musicRepository.moveSongInPlaylist(playlistId, fromIndex, toIndex)
        }
    }

    fun playSong(song: Song) {
        playerController.playSong(song)
    }

    fun playNext(song: Song) {
        playerController.playNext(song)
    }

    fun addToQueue(song: Song) {
        playerController.addToQueue(song)
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        playerController.playSongs(songs, startIndex)
    }

    fun refreshMusic() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            refreshMutex.withLock {
                _isLoading.value = true
                _error.value = null
                try {
                    refreshMusicUseCase()
                } catch (e: Exception) {
                    _error.value = "Error al escanear música: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun setLightListMode(enabled: Boolean) {
        _lightListModeEnabled.value = enabled
    }

    fun toggleLightListMode() {
        _lightListModeEnabled.value = !_lightListModeEnabled.value
    }

    fun clearError() {
        _error.value = null
    }
}

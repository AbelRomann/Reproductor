package com.example.reproductor.presentation.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.reproductor.domain.model.LoopRange
import com.example.reproductor.domain.model.PlaybackProgress
import com.example.reproductor.domain.model.PlayerState
import com.example.reproductor.domain.model.Playlist
import com.example.reproductor.domain.model.SavedSongLoop
import com.example.reproductor.domain.model.Song
import com.example.reproductor.domain.repository.MusicRepository
import com.example.reproductor.presentation.player.EqPreset
import com.example.reproductor.presentation.player.MusicPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val controller: MusicPlayerController,
    private val musicRepository: MusicRepository
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = controller.playerState
    val playbackProgress: StateFlow<PlaybackProgress> = controller.playbackProgress
    val repeatMode: StateFlow<Int> = controller.repeatMode
    val shuffleModeEnabled: StateFlow<Boolean> = controller.shuffleModeEnabled
    val sleepTimerRemainingMs: StateFlow<Long?> = controller.sleepTimerRemainingMs
    val eqPreset: StateFlow<EqPreset> = controller.eqPreset
    val loopRange: StateFlow<LoopRange> = controller.loopRange
    val savedLoopsForCurrentSong: StateFlow<List<SavedSongLoop>> = controller.playerState
        .flatMapLatest { state ->
            val songId = state.currentSong?.id
            if (songId == null || songId <= 0L) flowOf(emptyList())
            else musicRepository.getSavedLoopsForSong(songId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<Playlist>> = musicRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun togglePlayPause() {
        if (controller.playerState.value.isPlaying) controller.pause()
        else controller.play()
    }

    fun skipToNext() = controller.skipToNext()
    fun skipToPrevious() = controller.skipToPrevious()
    fun seekTo(position: Long) = controller.seekTo(position)
    fun getCurrentPlaybackPosition(): Long = controller.playbackProgress.value.currentPosition
    fun toggleRepeatMode() = controller.toggleRepeatMode()
    fun toggleShuffleMode() = controller.toggleShuffleMode()
    fun startSleepTimer(minutes: Int) = controller.startSleepTimer(minutes)
    fun cancelSleepTimer() = controller.cancelSleepTimer()
    fun setEqPreset(preset: EqPreset) = controller.setEqPreset(preset)
    fun markLoopStartAtCurrentPosition() = controller.markLoopStartAtCurrentPosition()
    fun markLoopEndAtCurrentPosition() = controller.markLoopEndAtCurrentPosition()
    fun enableLoopRange() = controller.enableLoopRange()
    fun disableLoopRange() = controller.disableLoopRange()
    fun clearLoopRange() = controller.clearLoopRange()
    fun setLoopToLastSeconds(seconds: Int) = controller.setLoopToLastSeconds(seconds)
    fun applySavedLoop(loop: SavedSongLoop) = controller.applySavedLoop(loop)

    fun saveCurrentLoop(name: String) {
        val songId = playerState.value.currentSong?.id ?: return
        val currentLoop = loopRange.value
        val start = currentLoop.startMs ?: return
        val end = currentLoop.endMs ?: return
        if (end <= start) return

        viewModelScope.launch {
            musicRepository.saveLoopForSong(
                songId = songId,
                name = name.trim(),
                startMs = start,
                endMs = end
            )
        }
    }

    fun deleteSavedLoop(loopId: Long) {
        viewModelScope.launch {
            musicRepository.deleteSavedLoop(loopId)
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(songId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun playNext(song: Song) = controller.playNext(song)
    fun addToQueue(song: Song) = controller.addToQueue(song)

    fun removeFromQueue(index: Int) = controller.removeFromQueue(index)
    fun skipToIndex(index: Int) = controller.skipToIndex(index)
    fun moveQueueItem(from: Int, to: Int) = controller.moveQueueItem(from, to)
    fun clearQueue() = controller.clearQueue()
}

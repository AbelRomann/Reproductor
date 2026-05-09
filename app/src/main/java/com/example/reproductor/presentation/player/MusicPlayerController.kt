package com.example.reproductor.presentation.player

import android.content.ComponentName
import android.content.Context
import android.media.audiofx.Equalizer
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.reproductor.domain.model.LoopRange
import com.example.reproductor.domain.model.PlaybackProgress
import com.example.reproductor.domain.model.PlayerState
import com.example.reproductor.domain.model.Song
import com.example.reproductor.domain.repository.MusicRepository
import com.example.reproductor.service.MusicPlayerService
import com.example.reproductor.util.AudioFadeManager
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicPlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val audioFadeManager: AudioFadeManager
) {
    private var mediaController: MediaController? = null
    private var equalizer: Equalizer? = null
    private var equalizerSessionId: Int = -1
    // SupervisorJob prevents one failure from cancelling the entire scope
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressUpdateJob: Job? = null

    // ── Play-count threshold ──────────────────────────────────────────────────
    private val minListenMs = 30_000L
    private val minListenMsForHistory = 15_000L
    private var currentListenSongId: Long? = null
    private var listenStartMs: Long? = null
    private var listenedMs: Long = 0L
    private var playCountCounted: Boolean = false
    private var historyCounted: Boolean = false
    private var lastAutoTransitionHandledMs: Long = 0L
    private var lastKnownPosition: Long = 0L  // para detectar loops silenciosos

    // ── State flows ───────────────────────────────────────────────────────────
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _playbackProgress = MutableStateFlow(PlaybackProgress())
    val playbackProgress: StateFlow<PlaybackProgress> = _playbackProgress.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()
    private var sleepTimerJob: Job? = null
    // Good compromise: smoother UI feedback without turning playback progress
    // into a frame-by-frame state publisher.
    private val progressUpdateIntervalMs = 250L

    private val _eqPreset = MutableStateFlow(EqPreset.FLAT)
    val eqPreset: StateFlow<EqPreset> = _eqPreset.asStateFlow()

    private val _loopRange = MutableStateFlow(LoopRange())
    val loopRange: StateFlow<LoopRange> = _loopRange.asStateFlow()

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlayerService::class.java)
        )

        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                mediaController?.let { controller ->
                    _repeatMode.value = controller.repeatMode
                    _shuffleModeEnabled.value = controller.shuffleModeEnabled
                    setupEqualizer(0)
                }
                setupPlayerListener()
                if (mediaController?.isPlaying == true) {
                    startProgressUpdates()
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayerState()
                if (isPlaying) {
                    startProgressUpdates()
                    recordListenStart()
                } else {
                    stopProgressUpdates()
                    updateProgress()
                    pauseListenClock()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                commitListenTimeIfNeeded()
                val newSongId = mediaItem?.mediaId?.toLongOrNull()
                currentListenSongId = newSongId
                listenStartMs = null
                listenedMs = 0L
                playCountCounted = false
                historyCounted = false
                lastKnownPosition = 0L
                clearLoopRange()
                updatePlayerState()
                if (mediaController?.isPlaying == true) {
                    recordListenStart()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                // DISCONTINUITY_REASON_AUTO_TRANSITION cubre tanto el paso a la siguiente
                // canción como el loop de REPEAT_MODE_ONE. En ambos casos reseteamos el
                // contador para que cada repetición sume su propio play count.
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    val now = System.currentTimeMillis()
                    if (now - lastAutoTransitionHandledMs > 500L) {
                        lastAutoTransitionHandledMs = now
                        commitListenTimeIfNeeded()
                        val newSongId = mediaController?.currentMediaItem?.mediaId?.toLongOrNull()
                        currentListenSongId = newSongId
                        listenStartMs = null
                        listenedMs = 0L
                        playCountCounted = false
                        historyCounted = false
                        lastKnownPosition = 0L
                        clearLoopRange()
                        if (mediaController?.isPlaying == true) {
                            recordListenStart()
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && restartLoopRangeIfNeeded()) {
                    return
                }
                updatePlayerState()
                setupEqualizer(0)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleModeEnabled.value = shuffleModeEnabled
            }
        })
    }

    // ── Sleep timer ───────────────────────────────────────────────────────────

    fun startSleepTimer(minutes: Int) {
        if (minutes <= 0) return
        val durationMs = minutes * 60_000L
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMs.value = durationMs
        sleepTimerJob = coroutineScope.launch {
            val endAt = System.currentTimeMillis() + durationMs
            while (isActive) {
                val remaining = (endAt - System.currentTimeMillis()).coerceAtLeast(0L)
                _sleepTimerRemainingMs.value = remaining
                if (remaining == 0L) {
                    pause()
                    break
                }
                delay(1000L)
            }
            _sleepTimerRemainingMs.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = null
    }

    // ── Equalizer ─────────────────────────────────────────────────────────────

    fun setEqPreset(preset: EqPreset) {
        _eqPreset.value = preset
        applyEqPreset(preset)
    }

    private fun setupEqualizer(audioSessionId: Int) {
        if (audioSessionId < 0 || audioSessionId == equalizerSessionId) return
        runCatching {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            equalizerSessionId = audioSessionId
            applyEqPreset(_eqPreset.value)
        }
    }

    private fun applyEqPreset(preset: EqPreset) {
        val eq = equalizer ?: return
        runCatching {
            val numberOfBands = eq.numberOfBands.toInt()
            val minLevel = eq.bandLevelRange[0].toInt()
            val maxLevel = eq.bandLevelRange[1].toInt()
            val boost = ((maxLevel - minLevel) * 0.32f).toInt()
            val bassLimit = 250_000
            val trebleStart = 4_000_000

            for (band in 0 until numberOfBands) {
                val centerFreq = eq.getCenterFreq(band.toShort())
                val level = when (preset) {
                    EqPreset.FLAT -> 0
                    EqPreset.BASS_BOOST -> when {
                        centerFreq <= bassLimit -> boost
                        centerFreq >= trebleStart -> -boost / 2
                        else -> 0
                    }
                    EqPreset.VOCAL -> when {
                        centerFreq in 800_000..3_000_000 -> boost
                        centerFreq <= bassLimit -> -boost / 3
                        else -> 0
                    }
                    EqPreset.TREBLE_BOOST -> when {
                        centerFreq >= trebleStart -> boost
                        centerFreq <= bassLimit -> -boost / 2
                        else -> 0
                    }
                }.coerceIn(minLevel, maxLevel)
                eq.setBandLevel(band.toShort(), level.toShort())
            }
        }
    }

    // ── Progress updates ──────────────────────────────────────────────────────

    private fun startProgressUpdates() {
        if (progressUpdateJob?.isActive == true) return
        progressUpdateJob = coroutineScope.launch {
            while (isActive) {
                updateProgress()
                delay(progressUpdateIntervalMs)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    private fun restartLoopRangeIfNeeded(): Boolean {
        val controller = mediaController ?: return false
        val loopRange = _loopRange.value
        if (!loopRange.isEnabled || !loopRange.isComplete) return false

        val loopStart = loopRange.startMs ?: return false
        controller.seekTo(loopStart)
        controller.play()
        publishPlaybackProgress(loopStart, controller.duration.coerceAtLeast(0L))
        lastKnownPosition = loopStart
        return true
    }

    // ── Listen-time tracking ──────────────────────────────────────────────────

    private fun recordListenStart() {
        if (_loopRange.value.isEnabled) return
        if (listenStartMs == null) {
            listenStartMs = System.currentTimeMillis()
        }
    }

    private fun pauseListenClock() {
        listenStartMs?.let { start ->
            listenedMs += System.currentTimeMillis() - start
            listenStartMs = null
        }
        checkListenThresholds()
    }

    private fun commitListenTimeIfNeeded() {
        pauseListenClock()
    }

    private fun checkListenThresholds() {
        val songId = currentListenSongId ?: return

        if (!historyCounted && listenedMs >= minListenMsForHistory) {
            historyCounted = true
            coroutineScope.launch {
                musicRepository.updateLastPlayed(songId, System.currentTimeMillis())
            }
        }

        if (!playCountCounted && listenedMs >= minListenMs) {
            playCountCounted = true
            coroutineScope.launch {
                musicRepository.incrementPlayCount(songId)
            }
        }
    }

    private fun updateProgress() {
        mediaController?.let { controller ->
            val currentPosition = controller.currentPosition.coerceAtLeast(0)
            val duration = controller.duration.coerceAtLeast(0)
            val loopRange = _loopRange.value

            if (loopRange.isEnabled && loopRange.isComplete) {
                val loopStart = loopRange.startMs ?: 0L
                val loopEnd = loopRange.endMs ?: duration
                val loopTriggerThreshold = (loopEnd - 100L).coerceAtLeast(loopStart)
                if (currentPosition >= loopTriggerThreshold) {
                    controller.seekTo(loopStart)
                    publishPlaybackProgress(loopStart, duration)
                    lastKnownPosition = loopStart
                    return
                }
            }

            publishPlaybackProgress(currentPosition, duration)

            // Detección de loop silencioso: si la posición retrocedió más de 3 segundos
            // y ya contamos el play, significa que la canción se repitió sin que
            // onPositionDiscontinuity se disparara (comportamiento variable en Media3).
            val positionJumpedBack = lastKnownPosition > 3_000L && currentPosition < lastKnownPosition - 3_000L
            if (!loopRange.isEnabled && positionJumpedBack && (playCountCounted || historyCounted)) {
                val now = System.currentTimeMillis()
                if (now - lastAutoTransitionHandledMs > 500L) {
                    lastAutoTransitionHandledMs = now
                    commitListenTimeIfNeeded()
                    listenedMs = 0L
                    playCountCounted = false
                    historyCounted = false
                    if (controller.isPlaying) recordListenStart()
                }
            }
            lastKnownPosition = currentPosition
        }
        if (_loopRange.value.isEnabled) return
        if (playCountCounted && historyCounted) return
        val currentListenStart = listenStartMs ?: return
        val currentListenedMs = listenedMs + (System.currentTimeMillis() - currentListenStart)
        if (!historyCounted && currentListenedMs >= minListenMsForHistory ||
            !playCountCounted && currentListenedMs >= minListenMs) {
            pauseListenClock()
            if (mediaController?.isPlaying == true) {
                recordListenStart()
            }
        }
    }

    private fun updatePlayerState() {
        mediaController?.let { controller ->
            val currentMediaItem = controller.currentMediaItem
            val currentSong = currentMediaItem?.let { item ->
                Song(
                    id = item.mediaId.toLongOrNull() ?: 0,
                    title = item.mediaMetadata.title?.toString() ?: "Unknown",
                    artist = item.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                    album = item.mediaMetadata.albumTitle?.toString() ?: "Unknown Album",
                    duration = controller.duration.coerceAtLeast(0),
                    path = item.requestMetadata.mediaUri?.toString() ?: "",
                    albumId = 0,
                    artistId = 0,
                    albumArt = item.mediaMetadata.artworkUri?.toString()
                )
            }

            _playerState.value = _playerState.value.copy(
                currentSong = currentSong,
                isPlaying = controller.isPlaying,
                currentIndex = controller.currentMediaItemIndex
            )

            publishPlaybackProgress(
                currentPosition = controller.currentPosition.coerceAtLeast(0),
                duration = controller.duration.coerceAtLeast(0)
            )
        }
    }

    // ── Playback controls ─────────────────────────────────────────────────────

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        mediaController?.let { controller ->
            val mediaItems = songs.map { song -> song.toMediaItem() }
            controller.setMediaItems(mediaItems, startIndex, 0)
            controller.prepare()
            controller.play()
            _playerState.value = _playerState.value.copy(
                queue = songs,
                currentIndex = startIndex
            )
        }
    }

    fun playSong(song: Song) {
        playSongs(listOf(song), 0)
    }

    fun playNext(song: Song) {
        mediaController?.let { controller ->
            val insertIndex = (controller.currentMediaItemIndex + 1)
                .coerceAtMost(controller.mediaItemCount)
            controller.addMediaItem(insertIndex, song.toMediaItem())
            val updatedQueue = _playerState.value.queue.toMutableList()
            updatedQueue.add(insertIndex, song)
            _playerState.value = _playerState.value.copy(queue = updatedQueue)
        }
    }

    fun addToQueue(song: Song) {
        mediaController?.let { controller ->
            controller.addMediaItem(song.toMediaItem())
            val updatedQueue = _playerState.value.queue.toMutableList()
            updatedQueue.add(song)
            _playerState.value = _playerState.value.copy(queue = updatedQueue)
        }
    }

    fun removeFromQueue(index: Int) {
        mediaController?.let { controller ->
            if (index < 0 || index >= controller.mediaItemCount) return
            controller.removeMediaItem(index)
            val updatedQueue = _playerState.value.queue.toMutableList()
            if (index < updatedQueue.size) updatedQueue.removeAt(index)
            _playerState.value = _playerState.value.copy(queue = updatedQueue)
        }
    }

    fun skipToIndex(index: Int) {
        mediaController?.let { controller ->
            controller.seekTo(index, 0L)
            controller.play()
            _playerState.value = _playerState.value.copy(currentIndex = index)
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        if (from == to) return
        mediaController?.let { controller ->
            controller.moveMediaItem(from, to)
            val updatedQueue = _playerState.value.queue.toMutableList()
            val item = updatedQueue.removeAt(from)
            updatedQueue.add(to, item)
            val newCurrentIndex = when {
                _playerState.value.currentIndex == from -> to
                from < _playerState.value.currentIndex && to >= _playerState.value.currentIndex ->
                    _playerState.value.currentIndex - 1
                from > _playerState.value.currentIndex && to <= _playerState.value.currentIndex ->
                    _playerState.value.currentIndex + 1
                else -> _playerState.value.currentIndex
            }
            _playerState.value = _playerState.value.copy(queue = updatedQueue, currentIndex = newCurrentIndex)
        }
    }

    fun clearQueue() {
        mediaController?.let { controller ->
            val currentIndex = controller.currentMediaItemIndex
            val count = controller.mediaItemCount
            if (currentIndex + 1 < count) {
                controller.removeMediaItems(currentIndex + 1, count)
            }
            if (currentIndex > 0) {
                controller.removeMediaItems(0, currentIndex)
            }
            val currentSong = _playerState.value.currentSong
            val newQueue = if (currentSong != null) listOf(currentSong) else emptyList()
            _playerState.value = _playerState.value.copy(queue = newQueue, currentIndex = 0)
        }
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        publishPlaybackProgress(
            currentPosition = position,
            duration = _playbackProgress.value.duration
        )
    }

    fun markLoopStartAtCurrentPosition() {
        val position = mediaController?.currentPosition?.coerceAtLeast(0L) ?: return
        val current = _loopRange.value
        _loopRange.value = current.copy(
            isEnabled = false,
            startMs = position,
            endMs = current.endMs?.takeIf { it > position }
        )
        pauseListenClock()
        if (mediaController?.isPlaying == true) {
            recordListenStart()
        }
    }

    fun markLoopEndAtCurrentPosition() {
        val controller = mediaController ?: return
        val current = _loopRange.value
        val start = current.startMs ?: return
        val currentPosition = controller.currentPosition.coerceAtLeast(0L)
        val duration = controller.duration.coerceAtLeast(0L)
        val upperBound = if (duration > 0L) duration else currentPosition
        val end = currentPosition.coerceIn(start + 250L, upperBound)
        _loopRange.value = current.copy(
            isEnabled = false,
            startMs = start,
            endMs = end
        )
        pauseListenClock()
        if (mediaController?.isPlaying == true) {
            recordListenStart()
        }
    }

    fun setLoopToLastSeconds(seconds: Int) {
        val controller = mediaController ?: return
        if (seconds <= 0) return
        val duration = controller.duration.coerceAtLeast(0L)
        val loopEnd = controller.currentPosition.coerceAtLeast(0L).coerceAtMost(duration)
        val loopStart = (loopEnd - seconds * 1000L).coerceAtLeast(0L)
        if (loopEnd <= loopStart) return

        _loopRange.value = LoopRange(
            isEnabled = true,
            startMs = loopStart,
            endMs = loopEnd
        )
        pauseListenClock()
        controller.seekTo(loopStart)
        publishPlaybackProgress(loopStart, duration)
    }

    fun enableLoopRange() {
        val controller = mediaController ?: return
        val current = _loopRange.value
        if (!current.isComplete) return
        _loopRange.value = current.copy(isEnabled = true)
        pauseListenClock()

        val start = current.startMs ?: return
        val end = current.endMs ?: return
        val position = controller.currentPosition.coerceAtLeast(0L)
        if (position < start || position >= end) {
            controller.seekTo(start)
            publishPlaybackProgress(start, controller.duration.coerceAtLeast(0L))
        }
    }

    fun disableLoopRange() {
        val wasEnabled = _loopRange.value.isEnabled
        _loopRange.value = _loopRange.value.copy(isEnabled = false)
        if (wasEnabled && mediaController?.isPlaying == true) {
            recordListenStart()
        }
    }

    fun clearLoopRange() {
        val wasEnabled = _loopRange.value.isEnabled
        _loopRange.value = LoopRange()
        if (wasEnabled && mediaController?.isPlaying == true) {
            recordListenStart()
        }
    }

    private fun publishPlaybackProgress(currentPosition: Long, duration: Long) {
        val sanitizedPosition = currentPosition.coerceAtLeast(0L)
        val sanitizedDuration = duration.coerceAtLeast(0L)
        val current = _playbackProgress.value

        if (sanitizedPosition == current.currentPosition && sanitizedDuration == current.duration) {
            return
        }

        _playbackProgress.value = PlaybackProgress(
            currentPosition = sanitizedPosition,
            duration = sanitizedDuration
        )
    }

    fun skipToNext() {
        mediaController?.seekToNext()
    }

    fun skipToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun toggleRepeatMode() {
        mediaController?.let { controller ->
            controller.repeatMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun toggleShuffleMode() {
        mediaController?.let { controller ->
            controller.shuffleModeEnabled = !controller.shuffleModeEnabled
        }
    }

    fun release() {
        commitListenTimeIfNeeded()
        audioFadeManager.cancelFade()
        cancelSleepTimer()
        progressUpdateJob?.cancel()
        equalizer?.release()
        equalizer = null
        equalizerSessionId = -1
        mediaController?.release()
        mediaController = null
    }

    private fun Song.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArt?.let { Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(path)
            .setMediaMetadata(metadata)
            .build()
    }
}

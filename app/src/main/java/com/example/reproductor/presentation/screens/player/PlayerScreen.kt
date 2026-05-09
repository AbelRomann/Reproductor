package com.example.reproductor.presentation.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImagePainter
import coil.ImageLoader
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.reproductor.presentation.components.PlayerOptionsSheet
import com.example.reproductor.presentation.components.QueueBottomSheet
import com.example.reproductor.presentation.components.SongOptionsSheet
import com.example.reproductor.presentation.components.formatDuration
import com.example.reproductor.presentation.screens.player.PlayerViewModel
import com.example.reproductor.presentation.player.EqPreset
import kotlin.math.absoluteValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Default Color palette ──────────────────────────────────────
private val DefaultBgTop = Color(0xFF12022A)
private val DefaultBgBottom = Color(0xFF000000)
private val AccentLime = Color(0xFFE8FF47)
private val NeutralMuted = Color(0xFF6B6B85)
private val NeutralDark = Color(0xFF3A3A50)
private val PinkHeart = Color(0xFFFF5F7E)
private val TrackInactive = Color(0xFF1E1E2E)
private val DefaultArtworkGrad1 = Color(0xFF7B61FF)
private val DefaultArtworkGrad2 = Color(0xFFFF5F7E)
private val DefaultArtworkGrad3 = Color(0xFFFF9A3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsStateWithLifecycle()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    val eqPreset by viewModel.eqPreset.collectAsStateWithLifecycle()
    val loopRange by viewModel.loopRange.collectAsStateWithLifecycle()

    // Fix #9: derivedStateOf to avoid full-tree recomposition
    val queue by remember { derivedStateOf { playerState.queue } }
    val currentIndex by remember { derivedStateOf { playerState.currentIndex } }
    val currentSong by remember { derivedStateOf { playerState.currentSong } }

    // ── Queue sheet state ────────────────────────────────────────
    var showQueueSheet by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // ── Song options sheet state ────────────────────────────────
    var showSongOptionsSheet by remember { mutableStateOf(false) }
    var showAudioToolsSheet by remember { mutableStateOf(false) }
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    // ── Player options sheet (EQ + Sleep Timer) ─────────────────
    var showPlayerOptionsSheet by remember { mutableStateOf(false) }

    // ── Seek state ──────────────────────────────────────────────
    // ── Dynamic Colors State ─────────────────────────────────────
    var bgTop by remember { mutableStateOf(DefaultBgTop) }
    var bgBottom by remember { mutableStateOf(DefaultBgBottom) }
    var artworkGrad1 by remember { mutableStateOf(DefaultArtworkGrad1) }
    var artworkGrad2 by remember { mutableStateOf(DefaultArtworkGrad2) }
    var artworkGrad3 by remember { mutableStateOf(DefaultArtworkGrad3) }


    val animatedBgTop by animateColorAsState(targetValue = bgTop, label = "bgTop", animationSpec = tween(800))
    val animatedBgBottom by animateColorAsState(targetValue = bgBottom, label = "bgBottom", animationSpec = tween(800))
    val animatedArtworkGrad1 by animateColorAsState(targetValue = artworkGrad1, label = "grad1", animationSpec = tween(800))
    val animatedArtworkGrad2 by animateColorAsState(targetValue = artworkGrad2, label = "grad2", animationSpec = tween(800))
    val animatedArtworkGrad3 by animateColorAsState(targetValue = artworkGrad3, label = "grad3", animationSpec = tween(800))

    // ── HorizontalPager state ────────────────────────────────────
    val pageCount = if (queue.isNotEmpty()) queue.size else 1
    val pagerState = rememberPagerState(
        initialPage = currentIndex.coerceAtLeast(0),
        pageCount = { pageCount }
    )

    // Fix #1 + #2: Reuse singleton ImageLoader & extract palette on IO thread
    val context = LocalContext.current
    val imageLoader = remember { ImageLoader(context) }
    LaunchedEffect(pagerState.settledPage, queue) {
        val song = queue.getOrNull(pagerState.settledPage)
        val artUri = song?.albumArt
        if (artUri == null) {
            bgTop = DefaultBgTop
            bgBottom = DefaultBgBottom
            artworkGrad1 = DefaultArtworkGrad1
            artworkGrad2 = DefaultArtworkGrad2
            artworkGrad3 = DefaultArtworkGrad3
        } else {
            try {
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(artUri)
                        .size(100)
                        .allowHardware(false)
                        .build()
                    val result = imageLoader.execute(request)
                    val bitmap = result.drawable?.toBitmap() ?: return@withContext
                    val palette = Palette.from(bitmap).generate()
                    withContext(Dispatchers.Main) {
                        bgTop = palette.darkMutedSwatch?.let { Color(it.rgb) }
                            ?: palette.darkVibrantSwatch?.let { Color(it.rgb) }
                            ?: palette.dominantSwatch?.let { Color(it.rgb) }
                            ?: DefaultBgTop
                        bgBottom = Color(0xFF000000)
                        artworkGrad1 = palette.vibrantSwatch?.let { Color(it.rgb) } ?: DefaultArtworkGrad1
                        artworkGrad2 = palette.lightVibrantSwatch?.let { Color(it.rgb) } ?: DefaultArtworkGrad2
                        artworkGrad3 = palette.dominantSwatch?.let { Color(it.rgb) } ?: DefaultArtworkGrad3
                    }
                }
            } catch (_: Exception) { }
        }
    }

    // Flag to prevent sync loops between pager and player
    var isAnimatingFromPlayer by remember { mutableStateOf(false) }

    // Pager → Player: when user swipes and settles on a new page, skip to that song
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { settledPage ->
                if (isAnimatingFromPlayer) {
                    isAnimatingFromPlayer = false
                    return@collect
                }
                if (settledPage != currentIndex && queue.isNotEmpty()) {
                    viewModel.skipToIndex(settledPage)
                }
            }
    }

    // Player → Pager: when currentIndex changes externally (buttons, headset), animate pager
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex != pagerState.currentPage) {
            isAnimatingFromPlayer = true
            pagerState.animateScrollToPage(currentIndex)
        }
    }

    // ── Root container ──────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            // Fix #5: drawBehind avoids layout-tree recomposition during color animation
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(animatedBgTop, animatedBgBottom)))
                }
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

        Spacer(Modifier.height(12.dp))

        // ── Top bar ─────────────────────────────────────────────
        TopBar(onBackClick = onBackClick, onMenuClick = { showPlayerOptionsSheet = true })

        Spacer(Modifier.height(24.dp))

        // ── Album artwork carousel (HorizontalPager) ────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            beyondViewportPageCount = 1
        ) { page ->
            val song = queue.getOrNull(page)
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
            AlbumArtwork(
                albumArt = song?.albumArt,
                gradColor1 = animatedArtworkGrad1,
                gradColor2 = animatedArtworkGrad2,
                gradColor3 = animatedArtworkGrad3,
                modifier = Modifier.graphicsLayer {
                    // Scale + fade effect for adjacent pages
                    val scale = 1f - (pageOffset * 0.15f).coerceAtMost(0.15f)
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - (pageOffset * 0.4f).coerceAtMost(0.4f)
                }
            )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Fix #6: Use songId as targetState to avoid phantom transitions
        val displaySong = queue.getOrNull(pagerState.settledPage) ?: currentSong

        AnimatedContent(
            targetState = displaySong?.id,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 })
                    .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 })
            },
            label = "song_info"
        ) { songId ->
            val song = queue.firstOrNull { it.id == songId } ?: displaySong
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = song?.title ?: "Sin canción",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = song?.artist ?: "Artista desconocido",
                    color = NeutralMuted,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                // Album badge
                if (song?.album != null && song.album.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = song.album.uppercase(),
                        color = AccentLime.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentLime.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Secondary actions ───────────────────────────────────
        SecondaryActionsRow(
            repeatMode = repeatMode,
            shuffleModeEnabled = shuffleModeEnabled,
            isFavorite = currentSong?.isFavorite == true,
            queueSize = playerState.queue.size,
            onToggleRepeatMode = { viewModel.toggleRepeatMode() },
            onToggleShuffleMode = { viewModel.toggleShuffleMode() },
            onOpenQueue = { showQueueSheet = true }
        )

        Spacer(Modifier.height(16.dp))

        // ── Progress bar ────────────────────────────────────────
        PlayerProgressSection(viewModel = viewModel)

        Spacer(Modifier.height(8.dp))

        // ── Transport controls ──────────────────────────────────
        TransportControls(
            isPlaying = playerState.isPlaying,
            onPlayPause = { viewModel.togglePlayPause() },
            onNext = { viewModel.skipToNext() },
            onPrevious = { viewModel.skipToPrevious() },
            onAddClick = { showSongOptionsSheet = true }
        )

        Spacer(Modifier.weight(1f))

        Spacer(Modifier.height(20.dp))
        }

        // ── Drag handle para abrir la cola ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -15) {
                            showQueueSheet = true
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    }

    // ── Queue bottom sheet ───────────────────────────────────────
    if (showQueueSheet) {
        QueueBottomSheet(
            queue = playerState.queue,
            currentIndex = playerState.currentIndex,
            sheetState = queueSheetState,
            onDismiss = { showQueueSheet = false },
            onSkipToIndex = { index ->
                viewModel.skipToIndex(index)
                scope.launch { queueSheetState.hide() }.invokeOnCompletion {
                    showQueueSheet = false
                }
            },
            onRemoveAt = { index -> viewModel.removeFromQueue(index) },
            onMoveItem = { from, to -> viewModel.moveQueueItem(from, to) },
            onClearQueue = { viewModel.clearQueue() }
        )
    }

    // ── Song options bottom sheet ────────────────────────────────
    // Local val needed because delegated properties can't be smart-cast
    val activeSong = currentSong
    if (showSongOptionsSheet && activeSong != null) {
        SongOptionsSheet(
            song = activeSong,
            playlists = playlists,
            coverGradient = listOf(animatedArtworkGrad1, animatedArtworkGrad2, animatedArtworkGrad3),
            onDismiss = { showSongOptionsSheet = false },
            onPlayNext = {
                viewModel.playNext(activeSong)
                showSongOptionsSheet = false
            },
            onAddToQueue = {
                viewModel.addToQueue(activeSong)
                showSongOptionsSheet = false
            },
            onAddToPlaylist = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, activeSong.id)
                showSongOptionsSheet = false
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(activeSong.id)
                showSongOptionsSheet = false
            }
        )
    }

    // ── Player options sheet (EQ + Sleep Timer) ──────────────────
    if (showPlayerOptionsSheet) {
        PlayerOptionsSheet(
            currentEqPreset = eqPreset,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            loopRange = loopRange,
            currentPosition = viewModel.getCurrentPlaybackPosition(),
            onDismiss = { showPlayerOptionsSheet = false },
            onSetEqPreset = { preset -> viewModel.setEqPreset(preset) },
            onStartSleepTimer = { minutes ->
                viewModel.startSleepTimer(minutes)
                showPlayerOptionsSheet = false
            },
            onCancelSleepTimer = { viewModel.cancelSleepTimer() },
            onMarkLoopStart = { viewModel.markLoopStartAtCurrentPosition() },
            onMarkLoopEnd = { viewModel.markLoopEndAtCurrentPosition() },
            onEnableLoop = { viewModel.enableLoopRange() },
            onDisableLoop = { viewModel.disableLoopRange() },
            onClearLoop = { viewModel.clearLoopRange() },
            onLoopLast3Seconds = { viewModel.setLoopToLastSeconds(3) }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  Sub-composables
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TopBar(onBackClick: () -> Unit, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = NeutralMuted
            )
        }
        Text(
            text = "REPRODUCIENDO",
            color = NeutralMuted,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onMenuClick) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Opciones de audio",
                tint = NeutralMuted
            )
        }
    }
}

@Composable
private fun AudioToolsSheet(
    sleepTimerRemainingMs: Long?,
    currentPreset: EqPreset,
    onSleepTimerSelected: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onEqPresetSelected: (EqPreset) -> Unit
) {
    val remainingText = sleepTimerRemainingMs?.let {
        val totalSeconds = it / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        "Activo: %02d:%02d".format(minutes, seconds)
    } ?: "Sin temporizador"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = AccentLime)
            Spacer(Modifier.width(8.dp))
            Text("Sleep timer", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(remainingText, color = NeutralMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 20, 30).forEach { min ->
                AssistChip(
                    onClick = { onSleepTimerSelected(min) },
                    label = { Text("${min}m") },
                    leadingIcon = {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF1E1E2E),
                        labelColor = Color.White
                    )
                )
            }
            if (sleepTimerRemainingMs != null) {
                AssistChip(
                    onClick = onCancelSleepTimer,
                    label = { Text("Cancelar") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF3A1E28),
                        labelColor = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = AccentLime)
            Spacer(Modifier.width(8.dp))
            Text("Ecualizador básico", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))

        val presets = listOf(
            EqPreset.FLAT to "Flat",
            EqPreset.BASS_BOOST to "Bass",
            EqPreset.VOCAL to "Vocal",
            EqPreset.TREBLE_BOOST to "Treble"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { (preset, label) ->
                val selected = preset == currentPreset
                AssistChip(
                    onClick = { onEqPresetSelected(preset) },
                    label = { Text(label) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) AccentLime else Color(0xFF1E1E2E),
                        labelColor = if (selected) Color.Black else Color.White
                    )
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlayerProgressSection(
    viewModel: PlayerViewModel
) {
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    var isUserSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember(playbackProgress.duration) { mutableFloatStateOf(0f) }

    LaunchedEffect(playbackProgress.currentPosition, playbackProgress.duration, isUserSeeking) {
        if (!isUserSeeking) {
            sliderPosition = if (playbackProgress.duration > 0) {
                (playbackProgress.currentPosition.toFloat() / playbackProgress.duration.toFloat())
                    .coerceIn(0f, 1f)
            } else 0f
        }
    }

    ProgressSection(
        sliderPosition = sliderPosition,
        currentPosition = playbackProgress.currentPosition,
        duration = playbackProgress.duration,
        onValueChange = { value ->
            isUserSeeking = true
            sliderPosition = value
        },
        onValueChangeFinished = {
            viewModel.seekTo((sliderPosition * playbackProgress.duration).toLong())
            isUserSeeking = false
        }
    )
}

@Composable
private fun AlbumArtwork(
    albumArt: String?,
    gradColor1: Color,
    gradColor2: Color,
    gradColor3: Color,
    modifier: Modifier = Modifier
) {
    // Fix #8: drawBehind avoids recreating Brush/List objects in composition
    Box(
        modifier = modifier
            .fillMaxWidth(0.82f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(gradColor1, gradColor2, gradColor3)))
            },
        contentAlignment = Alignment.Center
    ) {
        if (albumArt != null) {
            // Fix #7: removed allowHardware(false) — only needed for Palette, not display
            AsyncImage(
                model = albumArt,
                contentDescription = "Portada del álbum",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

@Composable
private fun SecondaryActionsRow(
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    isFavorite: Boolean,
    queueSize: Int,
    onToggleRepeatMode: () -> Unit,
    onToggleShuffleMode: () -> Unit,
    onOpenQueue: () -> Unit
) {
    // ── Micro-animation: repeat scale pulse ──────────────────────
    val repeatScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "repeatScale"
    )
    // Trigger a pulse by toggling a key each time repeatMode changes
    var repeatPulseKey by remember { mutableStateOf(false) }
    var repeatAnimScale by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(repeatMode) {
        repeatAnimScale = 1.25f
        repeatPulseKey = !repeatPulseKey
    }
    val animatedRepeatScale by animateFloatAsState(
        targetValue = if (repeatAnimScale > 1f) { repeatAnimScale = 1f; 1.25f } else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "repeatPulse"
    )

    // ── Micro-animation: favorite bounce ─────────────────────────
    var favAnimTrigger by remember { mutableStateOf(isFavorite) }
    val favScale by animateFloatAsState(
        targetValue = if (favAnimTrigger != isFavorite) 1.35f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = { favAnimTrigger = isFavorite },
        label = "favBounce"
    )

    // ── Micro-animation: shuffle rotation ────────────────────────
    var shuffleRotationTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(shuffleModeEnabled) {
        shuffleRotationTarget += 360f
    }
    val shuffleRotation by animateFloatAsState(
        targetValue = shuffleRotationTarget,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "shuffleRotation"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Repeat mode icon — scale pulse on change
        IconButton(onClick = onToggleRepeatMode, modifier = Modifier.size(40.dp)) {
            val (icon, tint) = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne to AccentLime
                Player.REPEAT_MODE_ALL -> Icons.Default.Repeat to AccentLime
                else -> Icons.Default.Replay to NeutralMuted
            }
            Icon(
                icon,
                contentDescription = "Repetir",
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = animatedRepeatScale
                        scaleY = animatedRepeatScale
                    }
            )
        }

        // Favorite — bounce on toggle
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Favorito",
            tint = if (isFavorite) PinkHeart else NeutralMuted,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = favScale
                    scaleY = favScale
                }
        )

        // Shuffle indicator — rotate on toggle
        IconButton(onClick = onToggleShuffleMode, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = "Aleatorio",
                tint = if (shuffleModeEnabled) AccentLime else NeutralMuted,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        rotationZ = shuffleRotation
                    }
            )
        }

        // Queue — tinted accent when queue has songs
        IconButton(onClick = onOpenQueue, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Cola",
                tint = if (queueSize > 0) AccentLime else NeutralMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressSection(
    sliderPosition: Float,
    currentPosition: Long,
    duration: Long,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val trackGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF7B61FF), // purple
            Color(0xFF9F8FFF),
            Color(0xFFCCDD77), // soft lime-yellow
            Color(0xFFE8FF47)  // bright lime
        )
    )
    val thumbGlowColor = Color.White.copy(alpha = 0.25f)
    val trackHeight = 3.dp
    val thumbRadius = 6.dp
    val glowRadius = 14.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(glowRadius * 2)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                            onValueChange(newValue)
                        },
                        onHorizontalDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, _ ->
                            val newValue = (change.position.x / size.width).coerceIn(0f, 1f)
                            onValueChange(newValue)
                            change.consume()
                        },
                        onDragEnd = { onValueChangeFinished() }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackY = center.y
                val trackHeightPx = trackHeight.toPx()
                val thumbRadiusPx = thumbRadius.toPx()
                val glowRadiusPx = glowRadius.toPx()
                val progressX = sliderPosition * size.width

                // Inactive track (right of thumb)
                drawRoundRect(
                    color = Color(0xFF2A2A3F),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, trackY - trackHeightPx / 2),
                    size = androidx.compose.ui.geometry.Size(size.width, trackHeightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeightPx / 2)
                )

                // Active gradient track (left of thumb)
                if (progressX > 0f) {
                    drawRoundRect(
                        brush = trackGradient,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, trackY - trackHeightPx / 2),
                        size = androidx.compose.ui.geometry.Size(progressX, trackHeightPx),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeightPx / 2)
                    )
                }

                // Thumb glow (soft halo)
                drawCircle(
                    color = thumbGlowColor,
                    radius = glowRadiusPx,
                    center = androidx.compose.ui.geometry.Offset(progressX, trackY),
                    blendMode = BlendMode.Screen
                )

                // Thumb circle (bright white)
                drawCircle(
                    color = Color.White,
                    radius = thumbRadiusPx,
                    center = androidx.compose.ui.geometry.Offset(progressX, trackY)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Time labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPosition),
                color = Color(0xFF6B6B8A),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatDuration(duration),
                color = Color(0xFF6B6B8A),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onAddClick: () -> Unit
) {
    // ── Micro-animation: press-scale on hero play/pause button ──
    val playPauseInteractionSource = remember { MutableInteractionSource() }
    val isPressed by playPauseInteractionSource.collectIsPressedAsState()
    val heroScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "heroScale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Replay / rewind
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.Default.Replay,
                contentDescription = "Rebobinar",
                tint = NeutralMuted,
                modifier = Modifier.size(28.dp)
            )
        }

        // Skip previous
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Anterior",
                tint = NeutralMuted,
                modifier = Modifier.size(36.dp)
            )
        }

        // ── PLAY / PAUSE — hero button with press-scale ─────────
        IconButton(
            onClick = onPlayPause,
            interactionSource = playPauseInteractionSource,
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer {
                    scaleX = heroScale
                    scaleY = heroScale
                }
                .shadow(
                    elevation = 18.dp,
                    shape = CircleShape,
                    ambientColor = AccentLime.copy(alpha = 0.45f),
                    spotColor = AccentLime.copy(alpha = 0.55f)
                )
                .clip(CircleShape)
                .background(AccentLime)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color(0xFF0A0A0F),
                modifier = Modifier.size(40.dp)
            )
        }

        // Skip next
        IconButton(onClick = onNext) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Siguiente",
                tint = NeutralMuted,
                modifier = Modifier.size(36.dp)
            )
        }

        // Add to queue / playlist
        IconButton(onClick = onAddClick) {
            Icon(
                Icons.Default.AddBox,
                contentDescription = "Opciones de canción",
                tint = NeutralMuted,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

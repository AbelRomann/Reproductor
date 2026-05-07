package com.example.reproductor.presentation.components

import android.graphics.Paint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.reproductor.presentation.player.PlayerViewModel
import com.example.reproductor.ui.theme.AccentLime
import com.example.reproductor.ui.theme.SurfaceDark
import com.example.reproductor.util.PaletteUtil

// ─────────────────────────────────────────────────────────────────────────────
// MiniPlayer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MiniPlayer(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val currentSong = playerState.currentSong ?: return
    val context = LocalContext.current
    val albumArtRequest = rememberArtworkRequest(
        data = currentSong.albumArt,
        size = 50.dp
    )

    // ── Palette background color ──────────────────────────────────────────────
    var paletteColor by remember { mutableStateOf(SurfaceDark) }

    LaunchedEffect(currentSong.albumArt) {
        paletteColor = PaletteUtil.extractDominantColor(
            context = context,
            artworkUri = currentSong.albumArt,
            fallback = SurfaceDark
        )
    }

    val animatedBgColor by animateColorAsState(
        targetValue = paletteColor,
        animationSpec = tween(durationMillis = 600),
        label = "miniPlayerBg"
    )

    // Accent ring color: lighter tint of the palette color
    val ringColor by remember(animatedBgColor) {
        derivedStateOf {
            Color(
                red   = (animatedBgColor.red   * 1.8f).coerceIn(0f, 1f),
                green = (animatedBgColor.green * 1.8f).coerceIn(0f, 1f),
                blue  = (animatedBgColor.blue  * 1.8f).coerceIn(0f, 1f),
                alpha = 1f
            ).takeIf { it != Color.White } ?: AccentLime
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedBgColor,
                        animatedBgColor.copy(alpha = 0.88f)
                    )
                )
            )
            // Subtle dark scrim for text readability + top separator line
            .drawWithContent {
                drawContent()
                drawRect(Color.Black.copy(alpha = 0.18f))
                // Thin top border for visual separation from content above
                drawRect(
                    color = Color.White.copy(alpha = 0.06f),
                    size = androidx.compose.ui.geometry.Size(size.width, 0.5.dp.toPx())
                )
            }
            .clickable(onClick = onExpand)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Album art with visualizer ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (currentSong.albumArt != null) {
                    AsyncImage(
                        model = albumArtRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

            }

            Spacer(Modifier.width(10.dp))

            // ── Song info ─────────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                MarqueeText(
                    text = currentSong.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = currentSong.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }

            Spacer(Modifier.width(4.dp))

            // ── Animated Play/Pause button with progress ring ─────────────────
            PlayPauseProgressButton(
                viewModel = viewModel,
                isPlaying = playerState.isPlaying,
                ringColor = ringColor,
                onToggle = { viewModel.togglePlayPause() }
            )

            // ── Skip next ─────────────────────────────────────────────────────
            IconButton(onClick = { viewModel.skipToNext() }) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Skip next",
                    tint = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PlayPauseProgressButton
// Isolated composable so only it recomposes when playbackProgress ticks.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayPauseProgressButton(
    viewModel: PlayerViewModel,
    isPlaying: Boolean,
    ringColor: Color,
    onToggle: () -> Unit
) {
    // Collected ONLY here → only this composable recomposes every ~1 s
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val rawProgress = remember(playbackProgress) {
        if (playbackProgress.duration > 0)
            playbackProgress.currentPosition.toFloat() / playbackProgress.duration.toFloat()
        else 0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progressRing"
    )

    // Icon cross-fade: 1f = playing (Pause icon), 0f = paused (Play icon)
    val playAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0f else 1f,
        animationSpec = tween(200),
        label = "playAlpha"
    )
    val pauseAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(200),
        label = "pauseAlpha"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val inset = strokeWidth / 2f
            val diameter = size.minDimension - strokeWidth

            // Background track ring
            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress ring
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Play icon (visible when paused)
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = Color.White.copy(alpha = playAlpha),
            modifier = Modifier.size(26.dp)
        )
        // Pause icon (visible when playing)
        Icon(
            imageVector = Icons.Default.Pause,
            contentDescription = "Pause",
            tint = Color.White.copy(alpha = pauseAlpha),
            modifier = Modifier.size(26.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MarqueeText
// Scrolls text horizontally when it overflows; static otherwise.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified
) {
    // Total cycle duration in ms
    val scrollDurationMs = 7000

    SubcomposeLayout(modifier = modifier) { constraints ->
        // Measure text unconstrained to find its natural width
        val textPlaceable = subcompose("text") {
            Text(text = text, style = style, color = color, maxLines = 1, softWrap = false)
        }.first().measure(Constraints())

        val containerWidth = constraints.maxWidth
        val textWidth = textPlaceable.width
        val overflows = textWidth > containerWidth

        val finalWidth = containerWidth.coerceAtLeast(0)
        val finalHeight = textPlaceable.height

        layout(finalWidth, finalHeight) {
            if (!overflows) {
                // No overflow → static placement, no animation object created
                val staticPlaceable = subcompose("static") {
                    Text(text = text, style = style, color = color, maxLines = 1)
                }.first().measure(Constraints(maxWidth = containerWidth))
                staticPlaceable.placeRelative(0, 0)
            } else {
                // Overflow → marquee
                val scrollPlaceable = subcompose("marquee") {
                    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
                    val rawOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = scrollDurationMs,
                                easing = LinearEasing
                            ),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "marqueeOffset"
                    )

                    // Only scroll the overflow amount so the title starts fully visible
                    val totalScroll = (textWidth - containerWidth).coerceAtLeast(0).toFloat()

                    // Cycle breakdown:
                    //  0.00–0.25 → pause at start (full title visible)
                    //  0.25–0.80 → smooth scroll to end
                    //  0.80–1.00 → pause at end before restart
                    val scrollPx = when {
                        rawOffset < 0.25f -> 0f
                        rawOffset < 0.80f -> totalScroll * ((rawOffset - 0.25f) / 0.55f)
                        else              -> totalScroll
                    }

                    Box(
                        modifier = Modifier
                            .width(with(LocalDensity.current) { finalWidth.toDp() })
                            .clipToBounds()           // prevent text from bleeding over album art
                            .drawWithContent {
                                // Edge fade masks
                                drawContent()
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.Black.copy(alpha = 0f),
                                        0.04f to Color.Transparent,
                                        0.90f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.9f)
                                    )
                                )
                            }
                    ) {
                        Text(
                            text = text,
                            style = style,
                            color = color,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.graphicsLayer {
                                translationX = -scrollPx
                            }
                        )
                    }
                }.first().measure(Constraints(maxWidth = finalWidth))
                scrollPlaceable.placeRelative(0, 0)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PlayingVisualizer  (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayingVisualizer(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    color: Color = MaterialTheme.colorScheme.primary
) {
    if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "visualizer")

        val bar1 by infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
            label = "b1"
        )
        val bar2 by infiniteTransition.animateFloat(
            initialValue = 0.1f, targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
            label = "b2"
        )
        val bar3 by infiniteTransition.animateFloat(
            initialValue = 0.4f, targetValue = 0.8f,
            animationSpec = infiniteRepeatable(tween(300, easing = LinearEasing), RepeatMode.Reverse),
            label = "b3"
        )

        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(Modifier.weight(1f).fillMaxHeight(bar1).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(color))
            Box(Modifier.weight(1f).fillMaxHeight(bar2).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(color))
            Box(Modifier.weight(1f).fillMaxHeight(bar3).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(color))
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(Modifier.weight(1f).fillMaxHeight(0.2f).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(color))
            Box(Modifier.weight(1f).fillMaxHeight(0.2f).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(color))
            Box(Modifier.weight(1f).fillMaxHeight(0.2f).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(color))
        }
    }
}

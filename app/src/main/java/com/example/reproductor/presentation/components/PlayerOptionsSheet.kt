package com.example.reproductor.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reproductor.domain.model.LoopRange
import com.example.reproductor.domain.model.SavedSongLoop
import com.example.reproductor.presentation.player.EqPreset

private val SheetBg = Color(0xFF0D1320)
private val SectionBg = Color(0xFF141C2E)
private val AccentLime = Color(0xFFE8FF47)
private val AccentBlue = Color(0xFF4FD5FF)
private val TextMutedC = Color(0xFF6B6B85)
private val Divider = Color(0xFF1B2238)
private val DangerPink = Color(0xFFFF5F7E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOptionsSheet(
    currentEqPreset: EqPreset,
    sleepTimerRemainingMs: Long?,
    loopRange: LoopRange,
    savedLoops: List<SavedSongLoop>,
    currentPosition: Long,
    onDismiss: () -> Unit,
    onSetEqPreset: (EqPreset) -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onMarkLoopStart: () -> Unit,
    onMarkLoopEnd: () -> Unit,
    onEnableLoop: () -> Unit,
    onDisableLoop: () -> Unit,
    onClearLoop: () -> Unit,
    onLoopLast3Seconds: () -> Unit,
    onSaveLoop: (String) -> Unit,
    onApplySavedLoop: (SavedSongLoop) -> Unit,
    onDeleteSavedLoop: (SavedSongLoop) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var loopName by remember { mutableStateOf("") }
    val canSaveLoop = loopRange.isComplete && loopName.trim().isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.18f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Opciones del reproductor",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, top = 4.dp)
            )

            HorizontalDivider(thickness = 0.5.dp, color = Divider)
            Spacer(Modifier.height(20.dp))

            SectionHeader(icon = Icons.Default.Equalizer, title = "Ecualizador")
            Spacer(Modifier.height(12.dp))

            val presets = listOf(
                EqPreset.FLAT to "Plano",
                EqPreset.BASS_BOOST to "Graves",
                EqPreset.VOCAL to "Vocal",
                EqPreset.TREBLE_BOOST to "Agudos"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presets.forEach { (preset, label) ->
                    val isSelected = currentEqPreset == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentLime.copy(alpha = 0.15f) else SectionBg)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) AccentLime else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSetEqPreset(preset) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) AccentLime else TextMutedC,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Divider)
            Spacer(Modifier.height(20.dp))

            SectionHeader(icon = Icons.Default.Repeat, title = "Loop A-B")
            Spacer(Modifier.height(12.dp))

            Text(
                text = buildLoopSummary(loopRange),
                color = if (loopRange.isEnabled) AccentBlue else TextMutedC,
                fontSize = 13.sp,
                fontWeight = if (loopRange.isEnabled) FontWeight.SemiBold else FontWeight.Normal
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionChip(
                    label = "Inicio aqui",
                    modifier = Modifier.weight(1f),
                    onClick = onMarkLoopStart
                )
                ActionChip(
                    label = "Fin aqui",
                    modifier = Modifier.weight(1f),
                    onClick = onMarkLoopEnd
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionChip(
                    label = if (loopRange.isEnabled) "Desactivar" else "Activar",
                    modifier = Modifier.weight(1f),
                    accent = loopRange.isEnabled,
                    onClick = if (loopRange.isEnabled) onDisableLoop else onEnableLoop
                )
                ActionChip(
                    label = "Ultimos 3 s",
                    modifier = Modifier.weight(1f),
                    onClick = onLoopLast3Seconds
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Posicion actual: ${formatDuration(currentPosition)}",
                    color = TextMutedC,
                    fontSize = 12.sp
                )
                Text(
                    text = "Borrar tramo",
                    color = DangerPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onClearLoop)
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Divider)
            Spacer(Modifier.height(20.dp))

            SectionHeader(icon = Icons.Default.LibraryMusic, title = "Secciones guardadas")
            Spacer(Modifier.height(12.dp))

            Text(
                text = "Cada mini loop se guarda solo para esta cancion.",
                color = TextMutedC,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))

            TextField(
                value = loopName,
                onValueChange = { loopName = it.take(40) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                placeholder = {
                    Text(
                        "Nombre del mini loop",
                        color = TextMutedC
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SectionBg,
                    unfocusedContainerColor = SectionBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = AccentBlue,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedPlaceholderColor = TextMutedC,
                    unfocusedPlaceholderColor = TextMutedC
                )
            )

            Spacer(Modifier.height(10.dp))

            ActionChip(
                label = "Guardar tramo actual",
                modifier = Modifier.fillMaxWidth(),
                accent = canSaveLoop,
                onClick = {
                    val trimmedName = loopName.trim()
                    if (loopRange.isComplete && trimmedName.isNotEmpty()) {
                        onSaveLoop(trimmedName)
                        loopName = ""
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            if (savedLoops.isEmpty()) {
                Text(
                    text = "No hay mini loops guardados para esta cancion.",
                    color = TextMutedC,
                    fontSize = 12.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedLoops.forEach { loop ->
                        SavedLoopRow(
                            loop = loop,
                            onClick = { onApplySavedLoop(loop) },
                            onDelete = { onDeleteSavedLoop(loop) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Divider)
            Spacer(Modifier.height(20.dp))

            SectionHeader(icon = Icons.Default.Bedtime, title = "Temporizador de sueno")
            Spacer(Modifier.height(12.dp))

            if (sleepTimerRemainingMs != null) {
                val totalSec = (sleepTimerRemainingMs / 1000).toInt()
                val mins = totalSec / 60
                val secs = totalSec % 60
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tiempo restante: %02d:%02d".format(mins, secs),
                        color = AccentBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    IconButton(onClick = onCancelSleepTimer) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancelar temporizador",
                            tint = DangerPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                val options = listOf(5 to "5 min", 10 to "10 min", 15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "60 min")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { (minutes, label) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SectionBg)
                                        .clickable { onStartSleepTimer(minutes) }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SavedLoopRow(
    loop: SavedSongLoop,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SectionBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            Column {
                Text(
                    text = loop.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${formatDuration(loop.startMs)} - ${formatDuration(loop.endMs)}",
                    color = AccentBlue,
                    fontSize = 12.sp
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Borrar mini loop",
                tint = DangerPink,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (accent) AccentBlue.copy(alpha = 0.18f) else SectionBg)
            .border(
                width = if (accent) 1.dp else 0.dp,
                color = if (accent) AccentBlue else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (accent) AccentBlue else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun buildLoopSummary(loopRange: LoopRange): String {
    return when {
        loopRange.isEnabled && loopRange.isComplete ->
            "Activo: ${formatDuration(loopRange.startMs ?: 0L)} - ${formatDuration(loopRange.endMs ?: 0L)}"
        loopRange.isComplete ->
            "Listo: ${formatDuration(loopRange.startMs ?: 0L)} - ${formatDuration(loopRange.endMs ?: 0L)}"
        loopRange.startMs != null ->
            "Inicio: ${formatDuration(loopRange.startMs)} - falta fin"
        else -> "Sin tramo definido"
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AccentLime, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

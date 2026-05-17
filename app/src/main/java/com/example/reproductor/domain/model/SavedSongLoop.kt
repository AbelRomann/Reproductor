package com.example.reproductor.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class SavedSongLoop(
    val id: Long,
    val songId: Long,
    val name: String,
    val startMs: Long,
    val endMs: Long,
    val createdAt: Long
)

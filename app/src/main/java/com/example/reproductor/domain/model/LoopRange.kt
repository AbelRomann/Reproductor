package com.example.reproductor.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class LoopRange(
    val isEnabled: Boolean = false,
    val startMs: Long? = null,
    val endMs: Long? = null
) {
    val isComplete: Boolean
        get() = startMs != null && endMs != null && endMs > startMs
}

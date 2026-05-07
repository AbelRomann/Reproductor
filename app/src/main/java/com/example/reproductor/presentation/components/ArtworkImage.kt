package com.example.reproductor.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil.request.ImageRequest
import coil.size.Precision

@Composable
fun rememberArtworkRequest(
    data: String?,
    size: Dp,
    crossfade: Boolean = false
): ImageRequest? {
    if (data.isNullOrBlank()) return null

    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }

    return remember(context, data, sizePx, crossfade) {
        ImageRequest.Builder(context)
            .data(data)
            .size(sizePx)
            .precision(Precision.INEXACT)
            .crossfade(crossfade)
            .memoryCacheKey(data)
            .diskCacheKey(data)
            .build()
    }
}

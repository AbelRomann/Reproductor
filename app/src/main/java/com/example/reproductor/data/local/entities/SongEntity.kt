package com.example.reproductor.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.reproductor.domain.model.Song

@Entity(
    tableName = "songs",
    indices = [
        androidx.room.Index("artistId"),
        androidx.room.Index("albumId"),
        androidx.room.Index("isFavorite"),
        androidx.room.Index("dateAdded")
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumId: Long,
    val artistId: Long,
    val albumArt: String?,
    val customAlbumArt: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L
)

@androidx.room.Fts4(contentEntity = SongEntity::class)
@Entity(tableName = "songs_fts")
data class SongFtsEntity(
    @androidx.room.ColumnInfo(name = "rowid")
    @PrimaryKey
    val rowid: Long,
    val title: String,
    val artist: String,
    val album: String
)

fun SongEntity.toDomain() = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    duration = duration,
    path = path,
    albumId = albumId,
    artistId = artistId,
    albumArt = customAlbumArt ?: albumArt,
    hasCustomAlbumArt = !customAlbumArt.isNullOrBlank(),
    isFavorite = isFavorite,
    playCount = playCount,
    lastPlayed = lastPlayed
)

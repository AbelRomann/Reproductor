package com.example.reproductor.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.reproductor.domain.model.SavedSongLoop

@Entity(
    tableName = "saved_song_loops",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("songId")
    ]
)
data class SavedSongLoopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val songId: Long,
    val name: String,
    val startMs: Long,
    val endMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)

fun SavedSongLoopEntity.toDomain() = SavedSongLoop(
    id = id,
    songId = songId,
    name = name,
    startMs = startMs,
    endMs = endMs,
    createdAt = createdAt
)

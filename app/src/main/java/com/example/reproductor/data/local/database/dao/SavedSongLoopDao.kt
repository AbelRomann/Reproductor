package com.example.reproductor.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reproductor.data.local.entities.SavedSongLoopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSongLoopDao {
    @Query("SELECT * FROM saved_song_loops WHERE songId = :songId ORDER BY createdAt DESC, id DESC")
    fun getLoopsForSong(songId: Long): Flow<List<SavedSongLoopEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoop(loop: SavedSongLoopEntity): Long

    @Query("DELETE FROM saved_song_loops WHERE id = :loopId")
    suspend fun deleteLoop(loopId: Long)

    @Query("DELETE FROM saved_song_loops WHERE songId IN (:songIds)")
    suspend fun deleteLoopsBySongIds(songIds: List<Long>)
}

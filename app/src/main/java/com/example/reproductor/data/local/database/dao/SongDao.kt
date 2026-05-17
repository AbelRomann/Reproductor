package com.example.reproductor.data.local.database.dao

import androidx.room.*
import com.example.reproductor.data.local.entities.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC, id DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY dateAdded DESC, id DESC")
    fun getSongsByAlbum(albumId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artistId = :artistId ORDER BY dateAdded DESC, id DESC")
    fun getSongsByArtist(artistId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artist = :artistName ORDER BY title ASC")
    fun getSongsByArtistName(artistName: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getSongsSnapshot(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("DELETE FROM songs WHERE id IN (:songIds)")
    suspend fun deleteSongsByIds(songIds: List<Long>)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Query("""
        SELECT songs.* FROM songs 
        JOIN songs_fts ON songs.id = songs_fts.rowid 
        WHERE songs_fts MATCH :query 
        ORDER BY songs.dateAdded DESC, songs.id DESC
    """)
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("UPDATE songs SET isFavorite = NOT isFavorite WHERE id = :songId")
    suspend fun toggleFavorite(songId: Long)

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY dateAdded DESC, id DESC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long)

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedSongs(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayedSongs(limit: Int): Flow<List<SongEntity>>

    @Query("UPDATE songs SET lastPlayed = :timestamp WHERE id = :songId")
    suspend fun updateLastPlayed(songId: Long, timestamp: Long)

    @Query("UPDATE songs SET customAlbumArt = :albumArtUri WHERE id = :songId")
    suspend fun updateCustomAlbumArt(songId: Long, albumArtUri: String?)
}

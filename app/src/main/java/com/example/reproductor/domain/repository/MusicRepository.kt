package com.example.reproductor.domain.repository

import com.example.reproductor.domain.model.Album
import com.example.reproductor.domain.model.Playlist
import com.example.reproductor.domain.model.SavedSongLoop
import com.example.reproductor.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun getAllAlbums(): Flow<List<Album>>
    fun getSongsByAlbum(albumId: Long): Flow<List<Song>>
    fun getSongsByArtist(artistId: Long): Flow<List<Song>>
    fun getSongsByArtistName(artistName: String): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>>
    fun getFavoriteSongs(): Flow<List<Song>>
    suspend fun toggleFavorite(songId: Long)
    suspend fun refreshMusic()
    suspend fun forceRefreshMusic()

    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>>
    suspend fun createPlaylist(name: String): Long
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>)
    suspend fun createPlaylistAndAddSongs(name: String, songIds: List<Long>): Long
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun moveSongInPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int)

    fun getMostPlayedSongs(limit: Int = 20): Flow<List<Song>>
    suspend fun incrementPlayCount(songId: Long)
    fun getRecentlyPlayedSongs(limit: Int = 15): Flow<List<Song>>
    suspend fun updateLastPlayed(songId: Long, timestamp: Long)
    suspend fun updateCustomAlbumArt(songId: Long, albumArtUri: String?)
    fun getSavedLoopsForSong(songId: Long): Flow<List<SavedSongLoop>>
    suspend fun saveLoopForSong(songId: Long, name: String, startMs: Long, endMs: Long)
    suspend fun deleteSavedLoop(loopId: Long)
}

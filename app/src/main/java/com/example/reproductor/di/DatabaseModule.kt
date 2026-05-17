package com.example.reproductor.di

import android.content.Context
import androidx.room.Room
import com.example.reproductor.data.local.database.MusicDatabase
import com.example.reproductor.data.local.database.dao.AlbumDao
import com.example.reproductor.data.local.database.dao.PlaylistDao
import com.example.reproductor.data.local.database.dao.SavedSongLoopDao
import com.example.reproductor.data.local.database.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMusicDatabase(@ApplicationContext context: Context): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_database"
        )
        .addMigrations(
            MusicDatabase.MIGRATION_2_3, 
            MusicDatabase.MIGRATION_3_4, 
            MusicDatabase.MIGRATION_4_5, 
            MusicDatabase.MIGRATION_5_6,
            MusicDatabase.MIGRATION_6_7
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideSongDao(database: MusicDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    fun provideAlbumDao(database: MusicDatabase): AlbumDao {
        return database.albumDao()
    }

    @Provides
    fun providePlaylistDao(database: MusicDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideSavedSongLoopDao(database: MusicDatabase): SavedSongLoopDao {
        return database.savedSongLoopDao()
    }
}

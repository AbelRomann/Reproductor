package com.example.reproductor.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.reproductor.data.local.database.dao.AlbumDao
import com.example.reproductor.data.local.database.dao.PlaylistDao
import com.example.reproductor.data.local.database.dao.SavedSongLoopDao
import com.example.reproductor.data.local.database.dao.SongDao
import com.example.reproductor.data.local.entities.AlbumEntity
import com.example.reproductor.data.local.entities.PlaylistEntity
import com.example.reproductor.data.local.entities.PlaylistSongCrossRef
import com.example.reproductor.data.local.entities.SavedSongLoopEntity
import com.example.reproductor.data.local.entities.SongEntity
import com.example.reproductor.data.local.entities.SongFtsEntity

@Database(
    entities = [
        SongEntity::class,
        SongFtsEntity::class,
        AlbumEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        SavedSongLoopEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun savedSongLoopDao(): SavedSongLoopDao

    companion object {
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `songs_fts` USING FTS4(" +
                    "`title` TEXT NOT NULL, " +
                    "`artist` TEXT NOT NULL, " +
                    "`album` TEXT NOT NULL, " +
                    "content=`songs`)"
                )
                // Usamos content=songs, y para popular la tabla FTS4 inicialmente con content tables, 
                database.execSQL(
                    "INSERT INTO songs_fts(songs_fts) VALUES ('rebuild')"
                )
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_artistId` ON `songs` (`artistId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_albumId` ON `songs` (`albumId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_isFavorite` ON `songs` (`isFavorite`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_dateAdded` ON `songs` (`dateAdded`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_albums_artistId` ON `albums` (`artistId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_cross_ref_songId` ON `playlist_song_cross_ref` (`songId`)")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE songs ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE songs ADD COLUMN lastPlayed INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_song_loops` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `songId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `startMs` INTEGER NOT NULL,
                        `endMs` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_song_loops_songId` ON `saved_song_loops` (`songId`)")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE songs ADD COLUMN customAlbumArt TEXT")
            }
        }
    }
}

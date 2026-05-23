package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Bookmark::class,
        HistoryItem::class,
        TabItem::class,
        TabGroup::class,
        ExtensionItem::class,
        DownloadItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getDatabase(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "lesspecad_browser_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

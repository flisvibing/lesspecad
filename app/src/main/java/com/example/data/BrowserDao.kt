package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {

    // --- Bookmarks ---
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url LIMIT 1)")
    suspend fun isBookmarked(url: String): Boolean

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)


    // --- History ---
    @Query("SELECT * FROM history ORDER BY visitTime DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(historyItem: HistoryItem)

    @Delete
    suspend fun deleteHistoryItem(historyItem: HistoryItem)

    @Query("DELETE FROM history")
    suspend fun clearHistory()


    // --- Tabs ---
    @Query("SELECT * FROM tabs ORDER BY createdAt ASC")
    fun getAllTabs(): Flow<List<TabItem>>

    @Query("SELECT * FROM tabs WHERE id = :id LIMIT 1")
    suspend fun getTabById(id: String): TabItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabItem)

    @Update
    suspend fun updateTab(tab: TabItem)

    @Delete
    suspend fun deleteTab(tab: TabItem)

    @Query("DELETE FROM tabs")
    suspend fun clearAllTabs()


    // --- Tab Groups ---
    @Query("SELECT * FROM tab_groups ORDER BY createdAt ASC")
    fun getAllTabGroups(): Flow<List<TabGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabGroup(tabGroup: TabGroup)

    @Update
    suspend fun updateTabGroup(tabGroup: TabGroup)

    @Delete
    suspend fun deleteTabGroup(tabGroup: TabGroup)


    // --- Extensions ---
    @Query("SELECT * FROM extensions")
    fun getAllExtensions(): Flow<List<ExtensionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(extension: ExtensionItem)

    @Update
    suspend fun updateExtension(extension: ExtensionItem)

    @Delete
    suspend fun deleteExtension(extension: ExtensionItem)


    // --- Downloads ---
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadItem)

    @Delete
    suspend fun deleteDownload(download: DownloadItem)
}

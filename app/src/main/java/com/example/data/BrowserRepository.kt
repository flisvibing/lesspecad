package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class BrowserRepository(
    private val context: Context,
    private val browserDao: BrowserDao
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lesspecad_prefs", Context.MODE_PRIVATE)

    // --- Core Flows ---
    val allBookmarks: Flow<List<Bookmark>> = browserDao.getAllBookmarks()
    val allHistory: Flow<List<HistoryItem>> = browserDao.getAllHistory()
    val allTabs: Flow<List<TabItem>> = browserDao.getAllTabs()
    val allTabGroups: Flow<List<TabGroup>> = browserDao.getAllTabGroups()
    val allExtensions: Flow<List<ExtensionItem>> = browserDao.getAllExtensions()
    val allDownloads: Flow<List<DownloadItem>> = browserDao.getAllDownloads()

    // --- Bookmarks Logic ---
    suspend fun insertBookmark(bookmark: Bookmark) = browserDao.insertBookmark(bookmark)
    suspend fun deleteBookmark(bookmark: Bookmark) = browserDao.deleteBookmark(bookmark)
    suspend fun isBookmarked(url: String): Boolean = browserDao.isBookmarked(url)
    suspend fun deleteBookmarkByUrl(url: String) = browserDao.deleteBookmarkByUrl(url)

    // --- History Logic ---
    suspend fun addHistory(title: String, url: String) {
        val cleanTitle = title.ifBlank { url }
        browserDao.insertHistory(HistoryItem(title = cleanTitle, url = url))
    }
    suspend fun deleteHistoryItem(item: HistoryItem) = browserDao.deleteHistoryItem(item)
    suspend fun clearHistory() = browserDao.clearHistory()

    // --- Tabs Logic ---
    suspend fun insertTab(tab: TabItem) = browserDao.insertTab(tab)
    suspend fun updateTab(tab: TabItem) = browserDao.updateTab(tab)
    suspend fun deleteTab(tab: TabItem) = browserDao.deleteTab(tab)
    suspend fun clearAllTabs() = browserDao.clearAllTabs()
    suspend fun getTabById(id: String) = browserDao.getTabById(id)

    // --- Tab Groups ---
    suspend fun insertTabGroup(group: TabGroup) = browserDao.insertTabGroup(group)
    suspend fun updateTabGroup(group: TabGroup) = browserDao.updateTabGroup(group)
    suspend fun deleteTabGroup(group: TabGroup) = browserDao.deleteTabGroup(group)

    // --- Extensions ---
    suspend fun insertExtension(extension: ExtensionItem) = browserDao.insertExtension(extension)
    suspend fun updateExtension(extension: ExtensionItem) = browserDao.updateExtension(extension)
    suspend fun deleteExtension(extension: ExtensionItem) = browserDao.deleteExtension(extension)

    // --- Downloads ---
    suspend fun insertDownload(download: DownloadItem) = browserDao.insertDownload(download)
    suspend fun deleteDownload(download: DownloadItem) = browserDao.deleteDownload(download)

    // --- Shared Preferences Settings ---
    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    var defaultSearchEngine: String
        get() = prefs.getString("search_engine", "DuckDuckGo") ?: "DuckDuckGo"
        set(value) = prefs.edit().putString("search_engine", value).apply()

    var accentColorName: String
        get() = prefs.getString("accent_color_name", "Natural") ?: "Natural"
        set(value) = prefs.edit().putString("accent_color_name", value).apply()

    var isAdBlockEnabled: Boolean
        get() = prefs.getBoolean("ad_block_enabled", true)
        set(value) = prefs.edit().putBoolean("ad_block_enabled", value).apply()

    var isPrivacyEnabled: Boolean
        get() = prefs.getBoolean("privacy_enabled", false)
        set(value) = prefs.edit().putBoolean("privacy_enabled", value).apply()

    var defaultReaderFontSize: Float
        get() = prefs.getFloat("reader_font_size", 16f)
        set(value) = prefs.edit().putFloat("reader_font_size", value).apply()

    var defaultReaderFontFamily: String
        get() = prefs.getString("reader_font_family", "Serif") ?: "Serif"
        set(value) = prefs.edit().putString("reader_font_family", value).apply()

    // --- Backup & Sync Export/Import Engine ---
    suspend fun exportBackupJson(bookmarks: List<Bookmark>, history: List<HistoryItem>, extensions: List<ExtensionItem>): String {
        val root = JSONObject()
        
        val bookmarksArray = JSONArray()
        for (b in bookmarks) {
            val obj = JSONObject().apply {
                put("title", b.title)
                put("url", b.url)
            }
            bookmarksArray.put(obj)
        }
        root.put("bookmarks", bookmarksArray)

        val historyArray = JSONArray()
        for (h in history) {
            val obj = JSONObject().apply {
                put("title", h.title)
                put("url", h.url)
                put("visitTime", h.visitTime)
            }
            historyArray.put(obj)
        }
        root.put("history", historyArray)

        val extensionsArray = JSONArray()
        for (e in extensions) {
            if (!e.isSystem) {
                val obj = JSONObject().apply {
                    put("id", e.id)
                    put("name", e.name)
                    put("description", e.description)
                    put("jsCode", e.jsCode)
                    put("isEnabled", e.isEnabled)
                }
                extensionsArray.put(obj)
            }
        }
        root.put("extensions", extensionsArray)

        // Preferences Settings Backup
        val settingsObj = JSONObject().apply {
            put("search_engine", defaultSearchEngine)
            put("accent_color_name", accentColorName)
            put("ad_block_enabled", isAdBlockEnabled)
            put("privacy_enabled", isPrivacyEnabled)
        }
        root.put("settings", settingsObj)

        return root.toString(2)
    }

    suspend fun importBackupJson(backupString: String): Boolean {
        return try {
            val root = JSONObject(backupString)

            // Import Bookmarks
            if (root.has("bookmarks")) {
                val array = root.getJSONArray("bookmarks")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val title = obj.getString("title")
                    val url = obj.getString("url")
                    if (!browserDao.isBookmarked(url)) {
                        browserDao.insertBookmark(Bookmark(title = title, url = url))
                    }
                }
            }

            // Import History
            if (root.has("history")) {
                val array = root.getJSONArray("history")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val title = obj.getString("title")
                    val url = obj.getString("url")
                    val visitTime = obj.optLong("visitTime", System.currentTimeMillis())
                    browserDao.insertHistory(HistoryItem(title = title, url = url, visitTime = visitTime))
                }
            }

            // Import Extensions
            if (root.has("extensions")) {
                val array = root.getJSONArray("extensions")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    val description = obj.getString("description")
                    val jsCode = obj.getString("jsCode")
                    val isEnabled = obj.getBoolean("isEnabled")
                    browserDao.insertExtension(
                        ExtensionItem(id = id, name = name, description = description, jsCode = jsCode, isEnabled = isEnabled, isSystem = false)
                    )
                }
            }

            // Import Settings
            if (root.has("settings")) {
                val obj = root.getJSONObject("settings")
                if (obj.has("search_engine")) defaultSearchEngine = obj.getString("search_engine")
                if (obj.has("accent_color_name")) accentColorName = obj.getString("accent_color_name")
                if (obj.has("ad_block_enabled")) isAdBlockEnabled = obj.getBoolean("ad_block_enabled")
                if (obj.has("privacy_enabled")) isPrivacyEnabled = obj.getBoolean("privacy_enabled")
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

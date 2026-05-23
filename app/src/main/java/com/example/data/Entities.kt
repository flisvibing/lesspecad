package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val visitTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "tabs")
data class TabItem(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val groupId: String? = null,
    val isActive: Boolean = false,
    val isIncognito: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tab_groups")
data class TabGroup(
    @PrimaryKey val id: String,
    val name: String,
    val colorIndex: Int = 0, // Used to render a minimalist colored dot/accent
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "extensions")
data class ExtensionItem(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val jsCode: String,
    val isEnabled: Boolean = false,
    val isSystem: Boolean = false
)

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis()
)

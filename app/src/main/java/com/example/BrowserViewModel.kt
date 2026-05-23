package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BrowserDatabase.getDatabase(application)
    val repository = BrowserRepository(application, db.browserDao())

    // --- State Streams ---
    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tabs: StateFlow<List<TabItem>> = repository.allTabs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tabGroups: StateFlow<List<TabGroup>> = repository.allTabGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val extensions: StateFlow<List<ExtensionItem>> = repository.allExtensions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadItem>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Interface State ---
    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(repository.isOnboardingCompleted)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _searchEngine = MutableStateFlow(repository.defaultSearchEngine)
    val searchEngine: StateFlow<String> = _searchEngine.asStateFlow()

    private val _accentColorName = MutableStateFlow(repository.accentColorName)
    val accentColorName: StateFlow<String> = _accentColorName.asStateFlow()

    private val _adBlockEnabled = MutableStateFlow(repository.isAdBlockEnabled)
    val adBlockEnabled: StateFlow<Boolean> = _adBlockEnabled.asStateFlow()

    private val _privacyEnabled = MutableStateFlow(repository.isPrivacyEnabled)
    val privacyEnabled: StateFlow<Boolean> = _privacyEnabled.asStateFlow()

    // Dialog & Reader UI states
    private val _currentReaderContent = MutableStateFlow<String?>(null)
    val currentReaderContent: StateFlow<String?> = _currentReaderContent.asStateFlow()

    private val _currentReaderTitle = MutableStateFlow<String?>(null)
    val currentReaderTitle: StateFlow<String?> = _currentReaderTitle.asStateFlow()

    init {
        // Prepopulate default Extensions and standard Tab Groups if DB is fresh
        viewModelScope.launch {
            repository.allExtensions.first().let { currentExts ->
                if (currentExts.isEmpty()) {
                    setupDefaultExtensions()
                }
            }
            repository.allTabGroups.first().let { currentGroups ->
                if (currentGroups.isEmpty()) {
                    setupDefaultTabGroups()
                }
            }
            // Add initial Tab if tabs are empty
            repository.allTabs.first().let { currentTabs ->
                if (currentTabs.isNotEmpty()) {
                    val activeLast = currentTabs.find { it.isActive } ?: currentTabs.firstOrNull()
                    _activeTabId.value = activeLast?.id
                } else {
                    createNewTab("about:blank", false)
                }
            }
        }
    }

    // --- Actions ---

    fun completeOnboarding(engine: String, accent: String, blockAds: Boolean, incognitoDefault: Boolean) {
        viewModelScope.launch {
            repository.defaultSearchEngine = engine
            repository.accentColorName = accent
            repository.isAdBlockEnabled = blockAds
            repository.isPrivacyEnabled = incognitoDefault
            repository.isOnboardingCompleted = true

            _searchEngine.value = engine
            _accentColorName.value = accent
            _adBlockEnabled.value = blockAds
            _privacyEnabled.value = incognitoDefault
            _isOnboardingCompleted.value = true
        }
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        repository.isAdBlockEnabled = enabled
        _adBlockEnabled.value = enabled
    }

    fun setPrivacyEnabled(enabled: Boolean) {
        repository.isPrivacyEnabled = enabled
        _privacyEnabled.value = enabled
    }

    fun setAccentColor(accent: String) {
        repository.accentColorName = accent
        _accentColorName.value = accent
    }

    fun setSearchEngine(engine: String) {
        repository.defaultSearchEngine = engine
        _searchEngine.value = engine
    }

    // --- Downloads Logger ---
    fun registerDownload(fileName: String, url: String, mimeType: String, filePath: String) {
        viewModelScope.launch {
            repository.insertDownload(
                DownloadItem(
                    fileName = fileName,
                    url = url,
                    mimeType = mimeType,
                    filePath = filePath
                )
            )
        }
    }

    fun removeDownloadRecord(item: DownloadItem) {
        viewModelScope.launch {
            repository.deleteDownload(item)
        }
    }

    // --- Bookmarks Helpers ---
    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch {
            if (repository.isBookmarked(url)) {
                repository.deleteBookmarkByUrl(url)
            } else {
                repository.insertBookmark(Bookmark(title = title, url = url))
            }
        }
    }

    // --- History Helpers ---
    fun registerVisit(title: String, url: String) {
        if (url == "about:blank" || url.startsWith("file://") || _privacyEnabled.value) return
        viewModelScope.launch {
            repository.addHistory(title, url)
        }
    }

    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            repository.deleteHistoryItem(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // --- Tab Management ---
    fun createNewTab(url: String = "about:blank", isIncognito: Boolean = false, groupId: String? = null) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val newTab = TabItem(
                id = id,
                title = if (url == "about:blank") "Yeni Sekme" else "Yükleniyor...",
                url = url,
                groupId = groupId,
                isActive = true,
                isIncognito = isIncognito
            )

            // Deactivate other tabs
            val currentTabs = repository.allTabs.first()
            for (t in currentTabs) {
                if (t.isActive) {
                    repository.updateTab(t.copy(isActive = false))
                }
            }

            repository.insertTab(newTab)
            _activeTabId.value = id
        }
    }

    fun selectTab(tabId: String) {
        viewModelScope.launch {
            val currentTabs = repository.allTabs.first()
            var nextActiveId: String? = null
            for (t in currentTabs) {
                if (t.id == tabId) {
                    repository.updateTab(t.copy(isActive = true))
                    nextActiveId = t.id
                } else if (t.isActive) {
                    repository.updateTab(t.copy(isActive = false))
                }
            }
            if (nextActiveId != null) {
                _activeTabId.value = nextActiveId
            }
        }
    }

    fun closeTab(tabId: String) {
        viewModelScope.launch {
            val currentTabs = repository.allTabs.first()
            val closingTab = currentTabs.find { it.id == tabId } ?: return@launch
            repository.deleteTab(closingTab)

            if (closingTab.isActive) {
                // Activate another tab
                val remaining = currentTabs.filter { it.id != tabId }
                if (remaining.isNotEmpty()) {
                    val fallback = remaining.last()
                    repository.updateTab(fallback.copy(isActive = true))
                    _activeTabId.value = fallback.id
                } else {
                    createNewTab("about:blank", false)
                }
            }
        }
    }

    fun updateTabInfo(tabId: String, title: String, url: String) {
        viewModelScope.launch {
            val tab = repository.getTabById(tabId) ?: return@launch
            val updated = tab.copy(
                title = title.ifBlank { "Yeni Sekme" },
                url = url
            )
            repository.updateTab(updated)
        }
    }

    fun saveTabToGroup(tabId: String, groupId: String?) {
        viewModelScope.launch {
            val tab = repository.getTabById(tabId) ?: return@launch
            repository.updateTab(tab.copy(groupId = groupId))
        }
    }

    // --- Tab Groups ---
    fun createTabGroup(name: String, colorIndex: Int) {
        viewModelScope.launch {
            val group = TabGroup(
                id = UUID.randomUUID().toString(),
                name = name,
                colorIndex = colorIndex
            )
            repository.insertTabGroup(group)
        }
    }

    fun deleteTabGroup(group: TabGroup) {
        viewModelScope.launch {
            // Remove group associations from associated tabs
            val currentTabs = repository.allTabs.first()
            for (t in currentTabs) {
                if (t.groupId == group.id) {
                    repository.updateTab(t.copy(groupId = null))
                }
            }
            repository.deleteTabGroup(group)
        }
    }

    // --- Extensions Management ---
    fun toggleExtension(item: ExtensionItem) {
        viewModelScope.launch {
            repository.updateExtension(item.copy(isEnabled = !item.isEnabled))
        }
    }

    fun addCustomExtension(name: String, desc: String, jsCode: String) {
        viewModelScope.launch {
            val extension = ExtensionItem(
                id = UUID.randomUUID().toString(),
                name = name,
                description = desc,
                jsCode = jsCode,
                isEnabled = true,
                isSystem = false
            )
            repository.insertExtension(extension)
        }
    }

    fun deleteExtension(extension: ExtensionItem) {
        viewModelScope.launch {
            if (!extension.isSystem) {
                repository.deleteExtension(extension)
            }
        }
    }

    // --- Reading Mode Hooks ---
    fun setReaderContent(title: String, content: String) {
        _currentReaderTitle.value = title
        _currentReaderContent.value = content
    }

    fun clearReaderContent() {
        _currentReaderTitle.value = null
        _currentReaderContent.value = null
    }

    // --- Backup Service ---
    fun importBackup(payload: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val result = repository.importBackupJson(payload)
            if (result) {
                // Refresh local setting flows
                _searchEngine.value = repository.defaultSearchEngine
                _accentColorName.value = repository.accentColorName
                _adBlockEnabled.value = repository.isAdBlockEnabled
                _privacyEnabled.value = repository.isPrivacyEnabled
                _isOnboardingCompleted.value = repository.isOnboardingCompleted
                onSuccess()
            } else {
                onError()
            }
        }
    }

    // --- Setup Default Content ---

    private suspend fun setupDefaultExtensions() {
        val darkExt = ExtensionItem(
            id = "ext_dark_mode",
            name = "Gelişmiş Karanlık Mod",
            description = "Tüm web sitelerinin arka planını göz yormayan koyu gri tonlara çevirerek karanlık mod enjekte eder.",
            jsCode = """
                (function() {
                    var style = document.createElement('style');
                    style.id = 'lesspecad-dark-style';
                    style.innerHTML = 'html, body { background-color: #121212 !important; color: #e0e0e0 !important; } p, span, h1, h2, h3, a { color: #e0e0e0 !important; }';
                    document.head.appendChild(style);
                })();
            """.trimIndent(),
            isEnabled = false,
            isSystem = true
        )

        val speedExt = ExtensionItem(
            id = "ext_zoom_font",
            name = "Kolay Metin Büyütme",
            description = "Metin boyutlarını optimize ederek yazıların mobil cihazlarda daha kolay okunmasını sağlar.",
            jsCode = """
                (function() {
                    var style = document.createElement('style');
                    style.innerHTML = 'p, li, article { font-size: 1.15em !important; line-height: 1.6 !important; }';
                    document.head.appendChild(style);
                })();
            """.trimIndent(),
            isEnabled = false,
            isSystem = true
        )

        val imageExt = ExtensionItem(
            id = "ext_block_images",
            name = "Sadece Metin Modu",
            description = "Web sayfalarındaki tüm resimleri gizleyerek ultra hızlı veri ve batarya tasarrufu sağlar.",
            jsCode = """
                (function() {
                    var style = document.createElement('style');
                    style.innerHTML = 'img, video, iframe { display: none !important; }';
                    document.head.appendChild(style);
                })();
            """.trimIndent(),
            isEnabled = false,
            isSystem = true
        )

        repository.insertExtension(darkExt)
        repository.insertExtension(speedExt)
        repository.insertExtension(imageExt)
    }

    private suspend fun setupDefaultTabGroups() {
        repository.insertTabGroup(TabGroup(id = "group_work", name = "İş", colorIndex = 0))
        repository.insertTabGroup(TabGroup(id = "group_social", name = "Sosyal", colorIndex = 1))
        repository.insertTabGroup(TabGroup(id = "group_research", name = "Araştırma", colorIndex = 2))
    }
}

package com.example

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    // Map to pool and preserve WebView instances for tabs (saves memory, preserves history stack & scrolling state)
    private val webViewPool = mutableMapOf<String, WebView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
            val accentColorName by viewModel.accentColorName.collectAsStateWithLifecycle()

            // Resolve modern Color Palette based on current settings
            val themeColors = getThemeColors(accentColorName)

            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = themeColors.primary,
                    background = themeColors.background,
                    surface = themeColors.surface,
                    onBackground = themeColors.onBackground,
                    onSurface = themeColors.onSurface
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isOnboardingCompleted) {
                        OnboardingScreen(
                            onComplete = { engine, accent, ads, incog ->
                                viewModel.completeOnboarding(engine, accent, ads, incog)
                            },
                            colors = themeColors
                        )
                    } else {
                        BrowserMainScreen(
                            viewModel = viewModel,
                            webViewPool = webViewPool,
                            colors = themeColors
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // Correctly clean up pooled webviews on activity destruction to prevent memory leaks
        webViewPool.values.forEach {
            it.stopLoading()
            it.destroy()
        }
        webViewPool.clear()
        super.onDestroy()
    }
}

// --- Dynamic Visual Theme Colors Manager ---
data class LesspecadColorScheme(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val onSurface: Color,
    val accentLight: Color,
    val tintBorder: Color
)

fun getThemeColors(name: String): LesspecadColorScheme {
    return when (name) {
        "Emerald" -> LesspecadColorScheme(
            primary = Color(0xFF155E37),
            background = Color(0xFFF4F8F5),
            surface = Color(0xFFFFFFFF),
            onBackground = Color(0xFF1B241E),
            onSurface = Color(0xFF1B241E),
            accentLight = Color(0xFFE2EFE7),
            tintBorder = Color(0xFFD3E2D7)
        )
        "Teal" -> LesspecadColorScheme(
            primary = Color(0xFF0F5A69),
            background = Color(0xFFF1F6F7),
            surface = Color(0xFFFFFFFF),
            onBackground = Color(0xFF162529),
            onSurface = Color(0xFF162529),
            accentLight = Color(0xFFE0ECEF),
            tintBorder = Color(0xFFCDE1E5)
        )
        "Lavender" -> LesspecadColorScheme(
            primary = Color(0xFF654A8A),
            background = Color(0xFFF6F3F7),
            surface = Color(0xFFFFFFFF),
            onBackground = Color(0xFF231E29),
            onSurface = Color(0xFF231E29),
            accentLight = Color(0xFFEEEAF2),
            tintBorder = Color(0xFFE1D9E7)
        )
        "Amber" -> LesspecadColorScheme(
            primary = Color(0xFF7F4E16),
            background = Color(0xFFFAF7F2),
            surface = Color(0xFFFFFFFF),
            onBackground = Color(0xFF2B251F),
            onSurface = Color(0xFF2B251F),
            accentLight = Color(0xFFF6EFE5),
            tintBorder = Color(0xFFECE1CE)
        )
        "Natural" -> LesspecadColorScheme(
            primary = Color(0xFF6750A4),
            background = Color(0xFFFDF8F6),
            surface = Color(0xFFFFFFFF),
            onBackground = Color(0xFF1D1B1E),
            onSurface = Color(0xFF1D1B1E),
            accentLight = Color(0xFFF3EDF7),
            tintBorder = Color(0xFFE7E0EC)
        )
        else -> LesspecadColorScheme( // "Natural" is also the default fallback to implement "Natural Tones" design theme
            primary = Color(0xFF6750A4),
            background = Color(0xFFFDF8F6),
            surface = Color(0xFFFFFFFF),
            onBackground = Color(0xFF1D1B1E),
            onSurface = Color(0xFF1D1B1E),
            accentLight = Color(0xFFF3EDF7),
            tintBorder = Color(0xFFE7E0EC)
        )
    }
}

// Map color index to beautiful visual dot color for Tab Groups
fun getGroupDotColor(index: Int): Color {
    return when (index) {
        0 -> Color(0xFFC0392B) // Soft Red
        1 -> Color(0xFF2980B9) // Soft Blue
        2 -> Color(0xFF27AE60) // Soft Green
        3 -> Color(0xFFF39C12) // Soft Orange
        else -> Color(0xFF8E44AD) // Purple
    }
}

// --- 1. Onboarding Screen ---
@Composable
fun OnboardingScreen(
    onComplete: (String, String, Boolean, Boolean) -> Unit,
    colors: LesspecadColorScheme
) {
    var searchEngine by remember { mutableStateOf("DuckDuckGo") }
    var accentColor by remember { mutableStateOf("Natural") }
    var blockAds by remember { mutableStateOf(true) }
    var incognitoByDefault by remember { mutableStateOf(false) }

    val currentDemoColors = getThemeColors(accentColor)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentDemoColors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Logo & Header Top Bar style
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentDemoColors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "L",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Lesspecad",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = currentDemoColors.onBackground
                )
            }

            // Welcome & Customization Text Box
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Merhaba.",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = currentDemoColors.onBackground,
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tarayıcınızı sadece ihtiyacınız olanlarla donatın. Ultra sade, tamamen sizin.",
                    fontSize = 14.sp,
                    color = currentDemoColors.onBackground.copy(alpha = 0.7f),
                    lineHeight = 20.sp
                )
            }

            // Options Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = currentDemoColors.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Search Engine Selection
                    Column {
                        Text(
                            text = "Varsayılan Arama Motoru",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = currentDemoColors.onBackground.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val engines = listOf("Google", "DuckDuckGo", "Bing", "Ecosia")
                            engines.forEach { engine ->
                                val selected = searchEngine == engine
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) currentDemoColors.primary else currentDemoColors.background)
                                        .clickable { searchEngine = engine }
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) Color.Transparent else currentDemoColors.tintBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = engine,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else currentDemoColors.onBackground
                                    )
                                }
                            }
                        }
                    }

                    // Theme Accent Selector
                    Column {
                        Text(
                            text = "Renk Paleti",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = currentDemoColors.onBackground.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val palettes = listOf(
                                "Natural" to Color(0xFF6750A4),
                                "Emerald" to Color(0xFF155E37),
                                "Teal" to Color(0xFF0F5A69),
                                "Lavender" to Color(0xFF654A8A),
                                "Amber" to Color(0xFF7F4E16)
                            )
                            palettes.forEach { (name, color) ->
                                val selected = accentColor == name
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { accentColor = name }
                                        .border(
                                            width = 3.dp,
                                            color = if (selected) currentDemoColors.primary.copy(alpha = 0.4f) else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seçildi",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = currentDemoColors.tintBorder, thickness = 0.5.dp)

                    // Switch 1: Ad Block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reklam Engelleyici",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentDemoColors.onBackground
                            )
                            Text(
                                text = "Web sitelerindeki reklamları ve izleyicileri otomatik engeller.",
                                fontSize = 10.sp,
                                color = currentDemoColors.onBackground.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = blockAds,
                            onCheckedChange = { blockAds = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = currentDemoColors.surface,
                                checkedTrackColor = currentDemoColors.primary
                            )
                        )
                    }

                    // Switch 2: Privacy Mode Default
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Her Zaman Gizli Mod",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentDemoColors.onBackground
                            )
                            Text(
                                text = "Uygulama her açıldığında varsayılan olarak gizli modda başlar.",
                                fontSize = 10.sp,
                                color = currentDemoColors.onBackground.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = incognitoByDefault,
                            onCheckedChange = { incognitoByDefault = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = currentDemoColors.surface,
                                checkedTrackColor = currentDemoColors.primary
                            )
                        )
                    }
                }
            }

            // Launch Button
            Button(
                onClick = { onComplete(searchEngine, accentColor, blockAds, incognitoByDefault) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(4.dp, CircleShape),
                colors = ButtonDefaults.buttonColors(containerColor = currentDemoColors.primary),
                shape = CircleShape
            ) {
                Text(
                    text = "Gezinmeye Başla",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// --- 2. Main Browser Screen ---
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrowserMainScreen(
    viewModel: BrowserViewModel,
    webViewPool: MutableMap<String, WebView>,
    colors: LesspecadColorScheme
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val tabGroups by viewModel.tabGroups.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val extensions by viewModel.extensions.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val searchEngine by viewModel.searchEngine.collectAsStateWithLifecycle()
    val adBlockEnabled by viewModel.adBlockEnabled.collectAsStateWithLifecycle()
    val privacyEnabled by viewModel.privacyEnabled.collectAsStateWithLifecycle()

    val currentReaderTitle by viewModel.currentReaderTitle.collectAsStateWithLifecycle()
    val currentReaderContent by viewModel.currentReaderContent.collectAsStateWithLifecycle()

    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

    var inputUrl by remember { mutableStateOf(TextFieldValue("")) }
    var isAddressFocused by remember { mutableStateOf(false) }
    var webProgress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isCurrentBookmarked by remember { mutableStateOf(false) }

    // Active bottom sheets
    var showMenuSheet by remember { mutableStateOf(false) }
    var showTabsSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showDownloadsSheet by remember { mutableStateOf(false) }
    var showExtensionsSheet by remember { mutableStateOf(false) }
    var showSyncSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Synchronize Input Field with Active Tab's Loaded URL
    LaunchedEffect(activeTab?.url) {
        val currentUrl = activeTab?.url ?: "about:blank"
        val text = if (currentUrl == "about:blank") "" else currentUrl
        inputUrl = TextFieldValue(text = text)
    }

    // Refresh Bookmark toggle icon on current URL change
    LaunchedEffect(activeTab?.url, bookmarks) {
        activeTab?.let {
            isCurrentBookmarked = viewModel.repository.isBookmarked(it.url)
        }
    }

    // Retreiving the actual WebView for the active tab context
    val activeWebView = remember(activeTab?.id) {
        val key = activeTab?.id
        if (key != null) {
            webViewPool.getOrPut(key) {
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Lesspecad/1.0"
                    }

                    // Built-in Ad-Block mechanism via WebResourceRequest interceptor
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val reqUrl = request?.url?.toString() ?: return null
                            if (adBlockEnabled && isAdUrl(reqUrl)) {
                                return WebResourceResponse(
                                    "text/plain",
                                    "utf-8",
                                    ByteArrayInputStream(ByteArray(0))
                                )
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            webProgress = 10
                            url?.let {
                                if (it != "about:blank") {
                                    viewModel.updateTabInfo(key, "Yükleniyor...", it)
                                    inputUrl = TextFieldValue(text = it)
                                }
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            webProgress = 100
                            canGoBack = view?.canGoBack() ?: false
                            canGoForward = view?.canGoForward() ?: false

                            val pageTitle = view?.title ?: ""
                            url?.let {
                                val cleanTitle = pageTitle.ifBlank { it }
                                viewModel.updateTabInfo(key, cleanTitle, it)
                                if (it != "about:blank") {
                                    inputUrl = TextFieldValue(text = it)
                                    // Register history visit if not in Custom Incognito tab
                                    val incog = activeTab.isIncognito || privacyEnabled
                                    if (!incog) {
                                        viewModel.registerVisit(cleanTitle, it)
                                    }
                                }

                                // Native Javascript Extensions payload injections
                                extensions.forEach { ext ->
                                    if (ext.isEnabled) {
                                        view?.evaluateJavascript(ext.jsCode, null)
                                    }
                                }
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            webProgress = newProgress
                        }
                    }

                    // Robust System Downloads integrations
                    setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                        try {
                            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                            val request = DownloadManager.Request(Uri.parse(url)).apply {
                                setMimeType(mimetype)
                                addRequestHeader("User-Agent", userAgent)
                                setDescription("Lesspecad Web İndirmesi")
                                setTitle(fileName)
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                            }
                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)

                            val path = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                fileName
                            ).absolutePath

                            viewModel.registerDownload(fileName, url, mimetype, path)
                            Toast.makeText(context, "$fileName indiriliyor...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "İndirme başlatılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Register custom Javascript bridge interface for Reader Engine
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onContentExtracted(title: String, content: String) {
                            Handler(Looper.getMainLooper()).post {
                                viewModel.setReaderContent(title, content)
                            }
                        }
                    }, "LesspecadReader")
                }
            }
        } else {
            null
        }
    }

    // Configure Cookies depending on incognito / privacy tabs settings
    LaunchedEffect(activeTab?.isIncognito, privacyEnabled) {
        val cookieManager = CookieManager.getInstance()
        val incog = (activeTab?.isIncognito == true) || privacyEnabled
        if (incog) {
            cookieManager.setAcceptCookie(false)
            activeWebView?.settings?.cacheMode = WebSettings.LOAD_NO_CACHE
            activeWebView?.settings?.databaseEnabled = false
            activeWebView?.settings?.domStorageEnabled = false
            activeWebView?.clearCache(true)
            activeWebView?.clearHistory()
        } else {
            cookieManager.setAcceptCookie(true)
            activeWebView?.settings?.cacheMode = WebSettings.LOAD_DEFAULT
            activeWebView?.settings?.databaseEnabled = true
            activeWebView?.settings?.domStorageEnabled = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .background(colors.surface)
                    .statusBarsPadding()
            ) {
                // Top control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left back / forward navigation
                    IconButton(
                        onClick = { activeWebView?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = if (canGoBack) colors.primary else colors.onBackground.copy(alpha = 0.25f)
                        )
                    }

                    // Unified minimalist search input
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.background)
                            .border(0.5.dp, colors.tintBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isIncog = privacyEnabled || (activeTab?.isIncognito == true)
                        Icon(
                            imageVector = if (isIncog) Icons.Default.Lock else Icons.Default.Search,
                            contentDescription = "Güvenli Arama",
                            tint = if (isIncog) colors.primary else colors.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )

                        // Clean customized BasicTextField without heavy visual decorations
                        BasicTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("address_bar")
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        if (!isAddressFocused) {
                                            // Gained focus! Select all text
                                            inputUrl = inputUrl.copy(
                                                selection = TextRange(0, inputUrl.text.length)
                                            )
                                            isAddressFocused = true
                                        }
                                    } else {
                                        isAddressFocused = false
                                    }
                                },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                color = colors.onBackground,
                                fontFamily = FontFamily.SansSerif
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (inputUrl.text.isNotBlank()) {
                                        keyboardController?.hide()
                                        val destinationUrl = resolveQueryUrl(inputUrl.text, searchEngine)
                                        activeTab?.let { tab ->
                                            viewModel.updateTabInfo(tab.id, "Yükleniyor...", destinationUrl)
                                        }
                                        activeWebView?.loadUrl(destinationUrl)
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (inputUrl.text.isEmpty()) {
                                        Text(
                                            text = "Arama yapın veya URL girin",
                                            fontSize = 13.sp,
                                            color = colors.onBackground.copy(alpha = 0.35f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Clear or reload button
                        if (inputUrl.text.isNotEmpty()) {
                            IconButton(
                                onClick = { inputUrl = TextFieldValue("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Temizle",
                                    tint = colors.onBackground.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { activeWebView?.reload() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Yenile",
                                    tint = colors.onBackground.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Dynamic Tabs Manager button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, colors.primary, RoundedCornerShape(10.dp))
                            .clickable { showTabsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabs.size.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }

                    // Hamburger Menu Trigger
                    IconButton(
                        onClick = { showMenuSheet = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menü",
                            tint = colors.primary
                        )
                    }
                }

                // Dynamic high-precision loading progress bar
                if (webProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = webProgress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = colors.primary,
                        trackColor = Color.Transparent
                    )
                } else {
                    Spacer(modifier = Modifier.height(2.5.dp))
                }
            }
        },
        bottomBar = {
            // Ultra minimalist Quick Command Bar
            BottomAppBar(
                containerColor = colors.surface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(64.dp)
                    .border(0.5.dp, colors.tintBorder, RectangleShape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            // Go back to absolute default
                            activeTab?.let { tab ->
                                viewModel.updateTabInfo(tab.id, "Yeni Sekme", "about:blank")
                            }
                            activeWebView?.loadUrl("about:blank")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Anasayfa",
                            tint = colors.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            activeTab?.let {
                                viewModel.toggleBookmark(it.title, it.url)
                            }
                        },
                        enabled = activeTab?.url != "about:blank"
                    ) {
                        Icon(
                            imageVector = if (isCurrentBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Yer İmlerine Ekle",
                            tint = if (isCurrentBookmarked) colors.primary else colors.onBackground.copy(alpha = 0.4f)
                        )
                    }

                    // Bottom centered Add Tab quick trigger
                    IconButton(
                        onClick = { viewModel.createNewTab("about:blank", false) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Yeni Sekme",
                            tint = colors.primary
                        )
                    }

                    // Trigger Reading Mode parsing scripts
                    IconButton(
                        onClick = {
                            if (activeTab?.url != "about:blank") {
                                activeWebView?.evaluateJavascript(
                                    """
                                    (function() {
                                        var title = document.title || "";
                                        var paragraphs = Array.from(document.querySelectorAll("p")).map(p => p.innerText.trim()).filter(t => t.length > 20);
                                        var content = paragraphs.join("\n\n");
                                        if (content.length < 50) {
                                            var article = document.querySelector("article");
                                            if (article) content = article.innerText;
                                            else content = document.body.innerText;
                                        }
                                        LesspecadReader.onContentExtracted(title, content);
                                    })();
                                    """.trimIndent(), null
                                )
                            }
                        },
                        enabled = activeTab?.url != "about:blank"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu, // Reading Menu format icon
                            contentDescription = "Okuma Görünümü",
                            tint = colors.primary
                        )
                    }

                    IconButton(
                        onClick = { activeWebView?.goForward() },
                        enabled = canGoForward
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "İleri",
                            tint = if (canGoForward) colors.primary else colors.onBackground.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Render active view container based on url presence
            if (activeTab == null || activeTab.url == "about:blank") {
                LocalDashboard(
                    viewModel = viewModel,
                    colors = colors,
                    onSearchSubmit = { destination ->
                        activeTab?.let { tab ->
                            viewModel.updateTabInfo(tab.id, "Yükleniyor...", destination)
                        }
                        activeWebView?.loadUrl(destination)
                    }
                )
            } else {
                val currentTabId = activeTab?.id
                if (currentTabId != null) {
                    key(currentTabId) {
                        val webView = webViewPool[currentTabId]
                        if (webView != null) {
                            AndroidView(
                                factory = { webView },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Real Reading Mode warm reader text container sheet overlay
            if (currentReaderContent != null) {
                ReadingModeReader(
                    title = currentReaderTitle ?: "Bilinmeyen Başlık",
                    content = currentReaderContent!!,
                    onDismiss = { viewModel.clearReaderContent() },
                    colors = colors
                )
            }
        }
    }

    // --- Bottom Sheets Integrations ---

    // A. Main Hamburger Menu Sheet
    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            containerColor = colors.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Lesspecad",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Double columns list of basic menu options
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        MenuGridButton(
                            label = "Yer İmleri",
                            icon = Icons.Default.Star,
                            onClick = {
                                showMenuSheet = false
                                showBookmarksSheet = true
                            },
                            colors = colors
                        )
                    }
                    item {
                        MenuGridButton(
                            label = "Geçmiş",
                            icon = Icons.Default.Refresh,
                            onClick = {
                                showMenuSheet = false
                                showHistorySheet = true
                            },
                            colors = colors
                        )
                    }
                    item {
                        MenuGridButton(
                            label = "İndirilenler",
                            icon = Icons.Default.ArrowDownward,
                            onClick = {
                                showMenuSheet = false
                                showDownloadsSheet = true
                            },
                            colors = colors
                        )
                    }
                    item {
                        MenuGridButton(
                            label = "Eklentiler",
                            icon = Icons.Default.Extension,
                            onClick = {
                                showMenuSheet = false
                                showExtensionsSheet = true
                            },
                            colors = colors
                        )
                    }
                    item {
                        MenuGridButton(
                            label = "Bulutsuz Eşitleme",
                            icon = Icons.Default.Share,
                            onClick = {
                                showMenuSheet = false
                                showSyncSheet = true
                            },
                            colors = colors
                        )
                    }
                    item {
                        MenuGridButton(
                            label = "Ayarlar",
                            icon = Icons.Default.Settings,
                            onClick = {
                                showMenuSheet = false
                                showSettingsSheet = true
                            },
                            colors = colors
                        )
                    }
                }

                Divider(color = colors.tintBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                // Inline fast-action switches for core blocks
                OptionInlineSwitch(
                    title = "Reklam Engelleyici",
                    checked = adBlockEnabled,
                    onCheckedChange = { viewModel.setAdBlockEnabled(it) },
                    colors = colors
                )

                OptionInlineSwitch(
                    title = "Gizlilik ve Takip Önleme",
                    checked = privacyEnabled,
                    onCheckedChange = { viewModel.setPrivacyEnabled(it) },
                    colors = colors
                )
            }
        }
    }

    // B. Live Tabs & Tab Groups Sheet
    if (showTabsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTabsSheet = false },
            containerColor = colors.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var selectedGroupIdFilter by remember { mutableStateOf<String?>(null) }
                var showGroupCreateDialog by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Açık Sekmeler",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { showGroupCreateDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Grup Ekle",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Grup Ekle", fontSize = 11.sp, color = colors.primary)
                        }

                        IconButton(
                            onClick = {
                                viewModel.createNewTab("about:blank", false)
                                showTabsSheet = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Yeni Sekme Ekle",
                                tint = colors.primary
                            )
                        }
                    }
                }

                // Horizontal list of groups to filter open tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // "All" filter pill
                    TabGroupPill(
                        name = "Tümü",
                        isSelected = selectedGroupIdFilter == null,
                        color = colors.primary,
                        onClick = { selectedGroupIdFilter = null }
                    )

                    tabGroups.forEach { group ->
                        TabGroupPill(
                            name = group.name,
                            isSelected = selectedGroupIdFilter == group.id,
                            color = getGroupDotColor(group.colorIndex),
                            onClick = { selectedGroupIdFilter = group.id }
                        )
                    }
                }

                // Scrollable Grid of open tabs
                val filteredTabs = if (selectedGroupIdFilter == null) tabs else tabs.filter { it.groupId == selectedGroupIdFilter }

                if (filteredTabs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bu grupta açık sekme bulunmuyor.",
                            fontSize = 12.sp,
                            color = colors.onBackground.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTabs, key = { it.id }) { tab ->
                            val isCurrentActive = tab.id == activeTabId
                            var showGroupAssignSheet by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCurrentActive) colors.accentLight else colors.background)
                                    .combinedClickable(
                                        onClick = {
                                            viewModel.selectTab(tab.id)
                                            showTabsSheet = false
                                        },
                                        onLongClick = {
                                            showGroupAssignSheet = true
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrentActive) colors.primary else colors.tintBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 12.dp, horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Group color point if assigned
                                tab.groupId?.let { currentGroupId ->
                                    val matchGroup = tabGroups.find { g -> g.id == currentGroupId }
                                    if (matchGroup != null) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 10.dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(getGroupDotColor(matchGroup.colorIndex))
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tab.title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrentActive) FontWeight.Bold else FontWeight.Normal,
                                        color = colors.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (tab.url == "about:blank") "Boş Sayfa" else tab.url,
                                        fontSize = 10.sp,
                                        color = colors.onBackground.copy(alpha = 0.45f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.closeTab(tab.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Sekmeyi Kapat",
                                        tint = colors.onBackground.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Sub-sheet to assign tab to localized group
                            if (showGroupAssignSheet) {
                                AlertDialog(
                                    onDismissRequest = { showGroupAssignSheet = false },
                                    containerColor = colors.surface,
                                    title = { Text("Gruba Kaydet", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Bu sekmeyi kaydetmek istediğiniz kategoriyi seçin:",
                                                fontSize = 12.sp,
                                                color = colors.onBackground.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            // Assign to no group
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.background)
                                                    .clickable {
                                                        viewModel.saveTabToGroup(tab.id, null)
                                                        showGroupAssignSheet = false
                                                    }
                                                    .padding(12.dp)
                                            ) {
                                                Text("Hiçbiri", fontSize = 12.sp, color = colors.onBackground)
                                            }
                                            tabGroups.forEach { group ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.background)
                                                    .clickable {
                                                        viewModel.saveTabToGroup(tab.id, group.id)
                                                        showGroupAssignSheet = false
                                                    }
                                                    .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(getGroupDotColor(group.colorIndex))
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(group.name, fontSize = 12.sp, color = colors.onBackground)
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showGroupAssignSheet = false }) {
                                            Text("Vazgeç", color = colors.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Modal dialog to compile a new Tab Category Group
                if (showGroupCreateDialog) {
                    var newGroupName by remember { mutableStateOf("") }
                    var selectedColorIndex by remember { mutableStateOf(0) }

                    AlertDialog(
                        onDismissRequest = { showGroupCreateDialog = false },
                        containerColor = colors.surface,
                        title = { Text("Yeni Sekme Grubu", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = newGroupName,
                                    onValueChange = { newGroupName = it },
                                    label = { Text("Grup Adı") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.primary,
                                        focusedLabelColor = colors.primary
                                    )
                                )

                                Column {
                                    Text("Grup Rengi", fontSize = 12.sp, color = colors.onBackground.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (i in 0..4) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(getGroupDotColor(i))
                                                    .clickable { selectedColorIndex = i }
                                                    .border(
                                                        width = 2.dp,
                                                        color = if (selectedColorIndex == i) colors.primary else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newGroupName.isNotBlank()) {
                                        viewModel.createTabGroup(newGroupName, selectedColorIndex)
                                        showGroupCreateDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                            ) {
                                Text("Oluştur", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showGroupCreateDialog = false }) {
                                Text("Vazgeç", color = colors.primary)
                            }
                        }
                    )
                }
            }
        }
    }

    // C. Bookmarks Management Sheet
    if (showBookmarksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarksSheet = false },
            containerColor = colors.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var searchQ by remember { mutableStateOf("") }

                Text(
                    text = "Yer İmlerim",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                // Search field
                OutlinedTextField(
                    value = searchQ,
                    onValueChange = { searchQ = it },
                    placeholder = { Text("Yer imlerinde ara...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, "Ara") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        focusedLabelColor = colors.primary
                    )
                )

                val filteredBookmarks = if (searchQ.isBlank()) bookmarks else bookmarks.filter {
                    it.title.lowercase().contains(searchQ.lowercase()) || it.url.lowercase().contains(searchQ.lowercase())
                }

                if (filteredBookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQ.isEmpty()) "Henüz kaydedilmiş yer imi yok." else "Aramayla eşleşen sonuç bulunamadı.",
                            fontSize = 12.sp,
                            color = colors.onBackground.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredBookmarks) { bookmark ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.background)
                                    .clickable {
                                        activeTab?.let { tab ->
                                            viewModel.updateTabInfo(tab.id, "Yükleniyor...", bookmark.url)
                                        }
                                        activeWebView?.loadUrl(bookmark.url)
                                        showBookmarksSheet = false
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Bookmark",
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bookmark.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = bookmark.url,
                                        fontSize = 10.sp,
                                        color = colors.onBackground.copy(alpha = 0.45f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.toggleBookmark(bookmark.title, bookmark.url) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Sil",
                                        tint = colors.onBackground.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // D. History Logs Sheet
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = colors.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var searchQ by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Geçmiş",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )

                    if (history.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearHistory() }
                        ) {
                            Text("Tümünü Temizle", fontSize = 12.sp, color = colors.primary)
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQ,
                    onValueChange = { searchQ = it },
                    placeholder = { Text("Geçmişte ara...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, "Ara") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        focusedLabelColor = colors.primary
                    )
                )

                val filteredHistory = if (searchQ.isBlank()) history else history.filter {
                    it.title.lowercase().contains(searchQ.lowercase()) || it.url.lowercase().contains(searchQ.lowercase())
                }

                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQ.isEmpty()) "Henüz tarama geçmişi bulunmuyor." else "Aramayla eşleşen geçmiş kaydı yok.",
                            fontSize = 12.sp,
                            color = colors.onBackground.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredHistory) { hist ->
                            val readableTime = remember(hist.visitTime) {
                                val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
                                sdf.format(Date(hist.visitTime))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.background)
                                    .clickable {
                                        activeTab?.let { tab ->
                                            viewModel.updateTabInfo(tab.id, "Yükleniyor...", hist.url)
                                        }
                                        activeWebView?.loadUrl(hist.url)
                                        showHistorySheet = false
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = hist.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = readableTime,
                                            fontSize = 9.sp,
                                            color = colors.primary.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = hist.url,
                                            fontSize = 9.sp,
                                            color = colors.onBackground.copy(alpha = 0.4f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteHistoryItem(hist) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Kaydı Kapat",
                                        tint = colors.onBackground.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // E. Downloads Database Log Sheet
    if (showDownloadsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDownloadsSheet = false },
            containerColor = colors.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "İndirilen Dosyalar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                if (downloads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Henüz indirilmiş bir dosya bulunmuyor.",
                            fontSize = 12.sp,
                            color = colors.onBackground.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(downloads) { dItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.background)
                                    .clickable {
                                        // Attempt file open/shares using Intent Provider
                                        try {
                                            val file = File(dItem.filePath)
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                file
                                            )
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, dItem.mimeType)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Dosyayı Aç"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Dosya açılamadı, taşınmış veya silinmiş olabilir.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Dosya",
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dItem.fileName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = dItem.filePath,
                                        fontSize = 9.sp,
                                        color = colors.onBackground.copy(alpha = 0.45f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeDownloadRecord(dItem) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Kaydı Kaldır",
                                        tint = colors.onBackground.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // F. Plugins / Javascript Extensions Sheet
    if (showExtensionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExtensionsSheet = false },
            containerColor = colors.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var showAddExtensionDialog by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Eklentiler & Eklenti Motoru",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )

                    Button(
                        onClick = { showAddExtensionDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Eklenti Yükle", fontSize = 11.sp, color = Color.White)
                    }
                }

                Text(
                    text = "Lesspecad, sayfa yüklendiğinde otomatik olarak özel JavaScript scriptleri enjekte edebilen yerleşik bir motora sahiptir.",
                    fontSize = 11.sp,
                    color = colors.onBackground.copy(alpha = 0.5f)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(extensions) { ext ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = colors.background),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = "Eklenti",
                                            tint = colors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = ext.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onBackground
                                        )
                                    }

                                    Switch(
                                        checked = ext.isEnabled,
                                        onCheckedChange = { viewModel.toggleExtension(ext) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = colors.surface,
                                            checkedTrackColor = colors.primary
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = ext.description,
                                    fontSize = 11.sp,
                                    color = colors.onBackground.copy(alpha = 0.6f)
                                )

                                if (!ext.isSystem) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { viewModel.deleteExtension(ext) }) {
                                            Text("Sistemden Kaldır", fontSize = 11.sp, color = colors.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAddExtensionDialog) {
                    var extName by remember { mutableStateOf("") }
                    var extDesc by remember { mutableStateOf("") }
                    var extJs by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { showAddExtensionDialog = false },
                        containerColor = colors.surface,
                        title = { Text("Eklenti Oluştur", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Yüklenecek JS kodunu ve eklenti açıklamalarını tanımlayın:",
                                    fontSize = 11.sp,
                                    color = colors.onBackground.copy(alpha = 0.5f)
                                )
                                OutlinedTextField(
                                    value = extName,
                                    onValueChange = { extName = it },
                                    label = { Text("Eklenti Adı", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary)
                                )
                                OutlinedTextField(
                                    value = extDesc,
                                    onValueChange = { extDesc = it },
                                    label = { Text("Eklenti Açıklaması", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary)
                                )
                                OutlinedTextField(
                                    value = extJs,
                                    onValueChange = { extJs = it },
                                    label = { Text("JavaScript Kodu (evaluateJs)", fontSize = 11.sp) },
                                    placeholder = { Text("javascript:(function(){ ... })();") },
                                    maxLines = 5,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (extName.isNotBlank() && extJs.isNotBlank()) {
                                        viewModel.addCustomExtension(extName, extDesc, extJs)
                                        showAddExtensionDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                            ) {
                                Text("Doğrula & Yükle", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddExtensionDialog = false }) {
                                Text("Vazgeç", color = colors.primary)
                            }
                        }
                    )
                }
            }
        }
    }

    // G. Cloud Sync / Eşitleme & Yedekleme
    if (showSyncSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSyncSheet = false },
            containerColor = colors.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.75f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var syncPayload by remember { mutableStateOf("") }

                Text(
                    text = "Lesspecad Bulutsuz Veri Eşitleme",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                Text(
                    text = "Yer imleriniz, arama geçmişiniz, ayarlamalarınız ve yüklü olan tüm eklenti verileriniz tek bir transfer koduna sıkıştırılır. İnternet sunucularına veri yüklemeden, kopyaladığınız kodu başka Lesspecad paneline yapıştırarak anında eşitleme sağlayabilirsiniz.",
                    fontSize = 11.sp,
                    color = colors.onBackground.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val payload = viewModel.repository.exportBackupJson(bookmarks, history, extensions)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Lesspecad Sync Code", payload)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Eşitleme transfer kodu panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, "Kopya", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Yedek Kodu Al", fontSize = 11.sp, color = Color.White)
                    }
                }

                Divider(color = colors.tintBorder, thickness = 0.5.dp)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Transfer Kodu İçe Aktar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    OutlinedTextField(
                        value = syncPayload,
                        onValueChange = { syncPayload = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = { Text("Lesspecad JSON transfer kodunu buraya yapıştırın...", fontSize = 11.sp) },
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary)
                    )
                    Button(
                        onClick = {
                            if (syncPayload.isNotBlank()) {
                                viewModel.importBackup(
                                    syncPayload,
                                    onSuccess = {
                                        Toast.makeText(context, "Eşitleme işlemi tamamlandı! Tüm verileriniz senkronize edildi.", Toast.LENGTH_LONG).show()
                                        showSyncSheet = false
                                    },
                                    onError = {
                                        Toast.makeText(context, "Desteklenmeyen veya hatalı transfer kodu yapıştırıldı.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, "Aktar", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Doğrula ve Eşitle", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }

    // H. Settings Preference Adjusters
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = colors.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.75f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Arayüz Ayarları",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                // Search Engine selector
                Column {
                    Text("Arama Motoru Seçeneği", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val items = listOf("Google", "DuckDuckGo", "Bing", "Ecosia")
                        items.forEach { engine ->
                            val isSel = searchEngine == engine
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) colors.primary else colors.background)
                                    .border(1.dp, if (isSel) Color.Transparent else colors.tintBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setSearchEngine(engine) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(engine, fontSize = 11.sp, color = if (isSel) Color.White else colors.onBackground)
                            }
                        }
                    }
                }

                Divider(color = colors.tintBorder, thickness = 0.5.dp)

                // Accent Theme Selector
                Column {
                    Text("Arayüz Renk Tonu", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val palettes = listOf(
                            "Natural" to Color(0xFF6750A4),
                            "Emerald" to Color(0xFF155E37),
                            "Teal" to Color(0xFF0F5A69),
                            "Lavender" to Color(0xFF654A8A),
                            "Amber" to Color(0xFF7F4E16)
                        )
                        palettes.forEach { (name, color) ->
                            val isSel = name == viewModel.accentColorName.value
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { viewModel.setAccentColor(name) }
                                    .border(
                                        width = 3.dp,
                                        color = if (isSel) colors.primary.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(Icons.Default.Check, "X", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Menu Helper Components ---
@Composable
fun MenuGridButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    colors: LesspecadColorScheme
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.background)
            .border(0.5.dp, colors.tintBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = icon, contentDescription = label, tint = colors.primary, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.onBackground)
        }
    }
}

@Composable
fun OptionInlineSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: LesspecadColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, color = colors.onBackground, fontWeight = FontWeight.SemiBold)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.surface,
                checkedTrackColor = colors.primary
            )
        )
    }
}

@Composable
fun TabGroupPill(
    name: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(if (isSelected) color else color.copy(alpha = 0.08f))
            .clickable { onClick() }
            .border(1.dp, if (isSelected) Color.Transparent else color.copy(alpha = 0.2f), RoundedCornerShape(30.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color.White else color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else color
        )
    }
}

// --- 3. Home Startup Dashboard ---
@Composable
fun LocalDashboard(
    viewModel: BrowserViewModel,
    colors: LesspecadColorScheme,
    onSearchSubmit: (String) -> Unit
) {
    var searchQueryLocal by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val extensions by viewModel.extensions.collectAsStateWithLifecycle()

    // Real-time dynamic clock tracking matching Ankara/Istanbul (UTC/Local)
    var formattedTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            formattedTime = sdf.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Elegant Clock and Brand
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Lesspecad",
                    fontSize = 40.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraLight,
                    color = colors.primary,
                    letterSpacing = 2.sp
                )
                if (formattedTime.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.primary.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Dashboard Fast search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(0.5.dp, colors.tintBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )

                BasicTextField(
                    value = searchQueryLocal,
                    onValueChange = { searchQueryLocal = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = colors.onBackground
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQueryLocal.isNotBlank()) {
                                keyboardController?.hide()
                                val destination = resolveQueryUrl(searchQueryLocal, viewModel.searchEngine.value)
                                onSearchSubmit(destination)
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQueryLocal.isEmpty()) {
                                Text(
                                    text = "${viewModel.searchEngine.value} ile internette ara...",
                                    fontSize = 13.sp,
                                    color = colors.onBackground.copy(alpha = 0.35f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // Quick Bookmarks links (Speed dials)
            if (bookmarks.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Hızlı Erişim",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 2.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bookmarks.take(8).forEach { bookmark ->
                            // Custom Minimal badge button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surface)
                                    .border(0.5.dp, colors.tintBorder, RoundedCornerShape(12.dp))
                                    .clickable { onSearchSubmit(bookmark.url) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Star, "Fav", tint = colors.primary, modifier = Modifier.size(11.dp))
                                    Text(
                                        text = bookmark.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dashboard summary parameters stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    DashboardStatCol("Yer İmi", bookmarks.size.toString(), colors)
                    DashboardStatCol("Geçmiş", history.size.toString(), colors)
                    DashboardStatCol("Eklentiler", extensions.filter { it.isEnabled }.size.toString(), colors)
                }
            }
        }
    }
}

@Composable
fun DashboardStatCol(
    label: String,
    value: String,
    colors: LesspecadColorScheme
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.primary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = colors.onBackground.copy(alpha = 0.45f))
    }
}

// --- 4. Beautiful Book-Like Reading Mode Reader ---
@Composable
fun ReadingModeReader(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    colors: LesspecadColorScheme
) {
    var fontSize by remember { mutableStateOf(16f) }
    var fontSerif by remember { mutableStateOf(true) }
    var sepiaTheme by remember { mutableStateOf(true) }

    // Resolve reader environment colors
    val readerBackground = if (sepiaTheme) Color(0xFFF4ECD8) else Color(0xFFFAF9F6)
    val readerText = if (sepiaTheme) Color(0xFF3C2F2F) else Color(0xFF1E1E1E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Reader command menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = readerText)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // font small button
                    IconButton(
                        onClick = { if (fontSize > 12f) fontSize -= 1.5f },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("A-", fontSize = 12.sp, color = readerText, fontWeight = FontWeight.Bold)
                    }

                    // font large button
                    IconButton(
                        onClick = { if (fontSize < 24f) fontSize += 1.5f },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("A+", fontSize = 15.sp, color = readerText, fontWeight = FontWeight.Bold)
                    }

                    // Serif / Sans Toggle
                    IconButton(
                        onClick = { fontSerif = !fontSerif },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (fontSerif) Icons.Default.FontDownload else Icons.Default.TextFields,
                            contentDescription = "Font Değiştir",
                            tint = readerText
                        )
                    }

                    // Theme sepia converter
                    IconButton(
                        onClick = { sepiaTheme = !sepiaTheme },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (sepiaTheme) Color.White else Color(0xFFE5D3B3))
                                .border(1.dp, readerText.copy(alpha = 0.4f), CircleShape)
                        )
                    }
                }
            }

            // Book content column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = title,
                    fontSize = (fontSize + 6f).sp,
                    fontFamily = if (fontSerif) FontFamily.Serif else FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = readerText,
                    lineHeight = (fontSize + 10f).sp
                )

                Divider(color = readerText.copy(alpha = 0.15f), thickness = 0.5.dp)

                Text(
                    text = content,
                    fontSize = fontSize.sp,
                    fontFamily = if (fontSerif) FontFamily.Serif else FontFamily.SansSerif,
                    color = readerText,
                    lineHeight = (fontSize * 1.6f).sp
                )
            }
        }
    }
}

// --- Dynamic Query Url Resolver ---
fun resolveQueryUrl(query: String, searchEngine: String): String {
    val trimmed = query.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file://")) {
        return trimmed
    }
    if (trimmed.contains(".") && !trimmed.contains(" ") && URLUtil.isValidUrl("https://$trimmed")) {
        return "https://$trimmed"
    }

    // Append to standard search engines depending on config Selection
    val encoded = Uri.encode(trimmed)
    return when (searchEngine) {
        "Google" -> "https://www.google.com/search?q=$encoded"
        "Bing" -> "https://www.bing.com/search?q=$encoded"
        "Ecosia" -> "https://www.ecosia.org/search?q=$encoded"
        else -> "https://duckduckgo.com/?q=$encoded"
    }
}

// Adblock evaluation keywords blocker
private fun isAdUrl(url: String): Boolean {
    val lower = url.lowercase()
    val checkList = arrayOf(
        "doubleclick.net", "googleads", "googlesyndication.com", "adservice.google",
        "adnow.com", "adnxs.com", "adroll", "adform", "adsrvr.org", "adtech",
        "analytics.google.com", "hotjar.com", "taboola.com", "outbrain.com",
        "popads", "popunder", "propellerads", "yandex.ru/clck", "scorecardresearch",
        "exoclick.com", "mgid.com", "addthis.com", "quantserve.com", "ad-delivery",
        "adsystem", "adserver", "pixel.wp.com", "/ads/", "/advert/", "advertising"
    )
    for (keyword in checkList) {
        if (lower.contains(keyword)) {
            return true
        }
    }
    return false
}

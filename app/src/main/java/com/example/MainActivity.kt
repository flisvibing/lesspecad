// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.lifecycle.lifecycleScope
import com.example.data.*
import kotlinx.coroutines.launch
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

        // Asynchronously pre-compile and load the expanded local adblock rule database from assets
        lifecycleScope.launch {
            AdBlocker.loadCustomHosts(applicationContext)
        }

        setContent {
            val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
            val accentColorName by viewModel.accentColorName.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            // Resolve modern Color Palette based on current settings
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }
            val themeColors = getThemeColors(accentColorName, isDark)

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
                            onComplete = { engine, accent, ads, incog, lang, theme ->
                                viewModel.completeOnboarding(engine, accent, ads, incog, lang, theme)
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

    override fun onPause() {
        super.onPause()
        try {
            webViewPool.values.forEach { webView ->
                webView.onPause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            webViewPool.values.forEach { webView ->
                webView.onResume()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        // Correctly clean up pooled webviews on activity destruction to prevent memory leaks and InputDispatcher channel issues
        webViewPool.values.forEach { webView ->
            try {
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView.stopLoading()
                webView.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

fun getThemeColors(name: String, isDark: Boolean = false): LesspecadColorScheme {
    if (isDark) {
        return when (name) {
            "Emerald" -> LesspecadColorScheme(
                primary = Color(0xFF81C784),
                background = Color(0xFF121814),
                surface = Color(0xFF1D2620),
                onBackground = Color(0xFFE2EFE7),
                onSurface = Color(0xFFE2EFE7),
                accentLight = Color(0xFF2E3B32),
                tintBorder = Color(0xFF3C4E41)
            )
            "Teal" -> LesspecadColorScheme(
                primary = Color(0xFF4DB6AC),
                background = Color(0xFF11181A),
                surface = Color(0xFF1A2427),
                onBackground = Color(0xFFE0ECEF),
                onSurface = Color(0xFFE0ECEF),
                accentLight = Color(0xFF223236),
                tintBorder = Color(0xFF32494E)
            )
            "Lavender" -> LesspecadColorScheme(
                primary = Color(0xFFB39DDB),
                background = Color(0xFF17141B),
                surface = Color(0xFF211D27),
                onBackground = Color(0xFFEEEAF2),
                onSurface = Color(0xFFEEEAF2),
                accentLight = Color(0xFF2E2737),
                tintBorder = Color(0xFF41374E)
            )
            "Amber" -> LesspecadColorScheme(
                primary = Color(0xFFFFB74D),
                background = Color(0xFF1C1814),
                surface = Color(0xFF28221B),
                onBackground = Color(0xFFF6EFE5),
                onSurface = Color(0xFFF6EFE5),
                accentLight = Color(0xFF3A3025),
                tintBorder = Color(0xFF524434)
            )
            else -> LesspecadColorScheme( // "Natural" is also the default fallback to implement "Natural Tones" design theme
                primary = Color(0xFFD0BCFF),
                background = Color(0xFF141218),
                surface = Color(0xFF1D1B22),
                onBackground = Color(0xFFE6E1E5),
                onSurface = Color(0xFFE6E1E5),
                accentLight = Color(0xFF2F293A),
                tintBorder = Color(0xFF49454F)
            )
        }
    } else {
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
    onComplete: (String, String, Boolean, Boolean, String, String) -> Unit,
    colors: LesspecadColorScheme
) {
    var currentPage by remember { mutableStateOf(1) }
    var appLanguage by remember { mutableStateOf(if (java.util.Locale.getDefault().language == "tr") "tr" else "en") }
    var searchEngine by remember { mutableStateOf("DuckDuckGo") }
    var accentColor by remember { mutableStateOf("Natural") }
    var themeMode by remember { mutableStateOf("system") }
    var blockAds by remember { mutableStateOf(true) }
    var incognitoByDefault by remember { mutableStateOf(false) }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val currentDemoColors = getThemeColors(accentColor, isDark)

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
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top branding / progress section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentPage > 1) {
                    // Small branding at top
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.lesspecad_logo),
                                contentDescription = "Lesspecad Logo",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Text(
                                text = "Lesspecad",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = currentDemoColors.onBackground
                            )
                        }
                        
                        // Page counter indicator
                        Text(
                            text = "$currentPage / 5",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentDemoColors.primary
                        )
                    }
                }
            }

            // Central content based on current page with smooth sliding/fading transition
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "onboarding_transition"
                ) { page ->
                    when (page) {
                    1 -> {
                        // PAGE 1: Welcome & Logo (perfectly centered)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.lesspecad_logo),
                                contentDescription = "Lesspecad Logo",
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .shadow(4.dp, RoundedCornerShape(20.dp))
                            )
                            
                            Text(
                                text = "Lesspecad",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentDemoColors.onBackground,
                                letterSpacing = (-0.5).sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Welcome.",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                color = currentDemoColors.onBackground
                            )
                            
                            Text(
                                text = "Equip your browser with only what you need. Ultra simple, completely yours.",
                                fontSize = 14.sp,
                                color = currentDemoColors.onBackground.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    
                    2 -> {
                        // PAGE 2: Language / Dil
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Language / Dil",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = currentDemoColors.onBackground
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(1.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = currentDemoColors.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    listOf("en" to "English", "tr" to "Türkçe").forEach { (code, name) ->
                                        val selected = appLanguage == code
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selected) currentDemoColors.primary else currentDemoColors.background)
                                                .clickable { appLanguage = code }
                                                .border(
                                                    width = 1.dp,
                                                    color = if (selected) Color.Transparent else currentDemoColors.tintBorder,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name,
                                                fontSize = 14.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) Color.White else currentDemoColors.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    3 -> {
                        // PAGE 3: Default Search Engine
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = Locales.getText(appLanguage, "default_search_engine"),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = currentDemoColors.onBackground
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(1.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = currentDemoColors.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val engines = listOf("Google", "DuckDuckGo", "Bing", "Ecosia")
                                    engines.forEach { engine ->
                                        val selected = searchEngine == engine
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selected) currentDemoColors.primary else currentDemoColors.background)
                                                .clickable { searchEngine = engine }
                                                .border(
                                                    width = 1.dp,
                                                    color = if (selected) Color.Transparent else currentDemoColors.tintBorder,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(vertical = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = engine,
                                                fontSize = 14.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) Color.White else currentDemoColors.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    4 -> {
                        // PAGE 4: Color Palette
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = Locales.getText(appLanguage, "color_palette"),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = currentDemoColors.onBackground
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(1.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = currentDemoColors.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    val palettes = listOf(
                                        "Natural" to Color(0xFF6750A4),
                                        "Emerald" to Color(0xFF155E37),
                                        "Teal" to Color(0xFF0F5A69),
                                        "Lavender" to Color(0xFF654A8A),
                                        "Amber" to Color(0xFF7F4E16)
                                    )
                                    
                                    // Row of large colored dots
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        palettes.forEach { (name, color) ->
                                            val selected = accentColor == name
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .clickable { accentColor = name }
                                                    .border(
                                                        width = 3.dp,
                                                        color = if (selected) currentDemoColors.primary.copy(alpha = 0.5f) else Color.Transparent,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (selected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                                                    // Accent color label
                                    Text(
                                        text = "${Locales.getText(appLanguage, "selected")}: $accentColor",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = currentDemoColors.onBackground
                                    )

                                    Divider(color = currentDemoColors.tintBorder, thickness = 0.5.dp)

                                    // Dark Mode Selector inside Onboarding Page 4
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = Locales.getText(appLanguage, "dark_mode"),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = currentDemoColors.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                "system" to Locales.getText(appLanguage, "dark_mode_system"),
                                                "light" to Locales.getText(appLanguage, "dark_mode_light"),
                                                "dark" to Locales.getText(appLanguage, "dark_mode_dark")
                                            ).forEach { (mode, label) ->
                                                val isSel = themeMode == mode
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSel) currentDemoColors.primary else currentDemoColors.background)
                                                        .border(1.dp, if (isSel) Color.Transparent else currentDemoColors.tintBorder, RoundedCornerShape(8.dp))
                                                        .clickable { themeMode = mode }
                                                        .padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        color = if (isSel) Color.White else currentDemoColors.onBackground
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    5 -> {
                        // PAGE 5: Options (Ad Blocker & Always Incognito)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = Locales.getText(appLanguage, "interface_settings"),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = currentDemoColors.onBackground
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(1.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = currentDemoColors.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    // Switch 1: Ad Block
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = Locales.getText(appLanguage, "ad_blocker"),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = currentDemoColors.onBackground
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = Locales.getText(appLanguage, "ad_blocker_sub"),
                                                fontSize = 11.sp,
                                                color = currentDemoColors.onBackground.copy(alpha = 0.6f),
                                                lineHeight = 16.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Switch(
                                            checked = blockAds,
                                            onCheckedChange = { blockAds = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = currentDemoColors.surface,
                                                checkedTrackColor = currentDemoColors.primary
                                            )
                                        )
                                    }

                                    Divider(color = currentDemoColors.tintBorder, thickness = 0.5.dp)

                                    // Switch 2: Always Incognito Mode
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = Locales.getText(appLanguage, "always_incognito"),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = currentDemoColors.onBackground
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = Locales.getText(appLanguage, "always_incognito_sub"),
                                                fontSize = 11.sp,
                                                color = currentDemoColors.onBackground.copy(alpha = 0.6f),
                                                lineHeight = 16.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
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
                        }
                    }
                }
            }
            }

            // Bottom Navigation Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentPage < 5) {
                    // Next [circle-arrow-right icon] button
                    Button(
                        onClick = { currentPage++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(4.dp, CircleShape),
                        colors = ButtonDefaults.buttonColors(containerColor = currentDemoColors.primary),
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (appLanguage == "tr") "Sonraki" else "Next",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowCircleRight,
                                contentDescription = "Next Arrow Icon",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Small click navigation for back
                    if (currentPage > 1) {
                        TextButton(
                            onClick = { currentPage-- },
                            colors = ButtonDefaults.textButtonColors(contentColor = currentDemoColors.primary)
                        ) {
                            Text(
                                text = if (appLanguage == "tr") "Geri" else "Back",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // Spacer to maintain exact height consistency for centering
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                } else {
                    // Start Browsing [rocket icon] button on the final page
                    Button(
                        onClick = { onComplete(searchEngine, accentColor, blockAds, incognitoByDefault, appLanguage, themeMode) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(4.dp, CircleShape),
                        colors = ButtonDefaults.buttonColors(containerColor = currentDemoColors.primary),
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = Locales.getText(appLanguage, "start_browsing"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = "Rocket Icon",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Back button on final page
                    TextButton(
                        onClick = { currentPage-- },
                        colors = ButtonDefaults.textButtonColors(contentColor = currentDemoColors.primary)
                    ) {
                        Text(
                            text = if (appLanguage == "tr") "Geri" else "Back",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
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
    val focusManager = LocalFocusManager.current
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
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val currentReaderTitle by viewModel.currentReaderTitle.collectAsStateWithLifecycle()
    val currentReaderContent by viewModel.currentReaderContent.collectAsStateWithLifecycle()

    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

    var inputUrl by remember { mutableStateOf(TextFieldValue("")) }
    var isAddressFocused by remember { mutableStateOf(false) }
    var webProgress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isCurrentBookmarked by remember { mutableStateOf(false) }
    var webViewGeneration by remember { mutableStateOf(0) }

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

    // Clear focus and hide keyboard when switching tabs or opening bottom sheets
    LaunchedEffect(
        activeTabId,
        showMenuSheet,
        showTabsSheet,
        showBookmarksSheet,
        showHistorySheet,
        showDownloadsSheet,
        showExtensionsSheet,
        showSyncSheet,
        showSettingsSheet
    ) {
        try {
            focusManager.clearFocus()
            keyboardController?.hide()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Optimize active/inactive WebViews inside the webViewPool: managed statefully by the AndroidView lifecycle

    // Retreiving the actual WebView for the active tab context
    val activeWebView = remember(activeTab?.id, webViewGeneration) {
        val key = activeTab?.id
        if (key != null) {
            val webView = webViewPool.getOrPut(key) {
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
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }

                    // Enable general and third-party cookies for seamless Google Sign-In
                    CookieManager.getInstance().let { cm ->
                        cm.setAcceptCookie(true)
                        cm.setAcceptThirdPartyCookies(this, true)
                    }

                    // Built-in Ad-Block mechanism via WebResourceRequest interceptor
                    webViewClient = object : WebViewClient() {
                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: RenderProcessGoneDetail?
                        ): Boolean {
                            val webView = view ?: return true
                            // Post to the main thread's message queue to safely dispose of the dead WebView
                            Handler(Looper.getMainLooper()).post {
                                try {
                                    val parentView = webView.parent as? ViewGroup
                                    parentView?.removeView(webView)
                                    if (key != null) {
                                        webViewPool.remove(key)
                                        webViewGeneration++
                                    }
                                    webView.destroy()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            return true // Handled smoothly so the parent app continues running safely
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            try {
                                val reqUrl = request?.url?.toString() ?: return null
                                if (viewModel.adBlockEnabled.value && isAdUrl(reqUrl)) {
                                    return WebResourceResponse(
                                        "text/plain",
                                        "utf-8",
                                        ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            webProgress = 10
                            url?.let {
                                if (it != "about:blank") {
                                    viewModel.updateTabInfo(key, Locales.getText(viewModel.appLanguage.value, "loading"), it)
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
                                    val specificTab = viewModel.tabs.value.find { it.id == key }
                                    val isIncogTab = specificTab?.isIncognito ?: false
                                    val incog = isIncogTab || viewModel.privacyEnabled.value
                                    if (!incog) {
                                        viewModel.registerVisit(cleanTitle, it)
                                    }
                                }

                                // Native Javascript Extensions payload injections
                                viewModel.extensions.value.forEach { ext ->
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
                            Toast.makeText(context, "$fileName ${Locales.getText(viewModel.appLanguage.value, "downloading")}", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "${Locales.getText(viewModel.appLanguage.value, "download_failed")}${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
            webView
        } else {
            null
        }
    }

    // Synchronize GoBack / GoForward states on tab switch
    LaunchedEffect(activeTabId, webViewGeneration) {
        activeWebView?.let { webView ->
            canGoBack = webView.canGoBack()
            canGoForward = webView.canGoForward()
        }
    }

    // Support physical/gesture back button to navigate backwards in the WebView history instead of exiting
    BackHandler(enabled = canGoBack) {
        activeWebView?.goBack()
    }

    // Synchronize webViewPool with the actual list of open tabs to prevent memory leaks and Out-of-Memory app crashes
    LaunchedEffect(tabs) {
        val activeIds = tabs.map { it.id }.toSet()
        val pooledIds = webViewPool.keys.toList()
        for (id in pooledIds) {
            if (id !in activeIds) {
                webViewPool.remove(id)?.let { webView ->
                    try {
                        webView.clearFocus()
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        webView.stopLoading()
                        webView.clearHistory()
                        webView.webViewClient = WebViewClient()
                        webView.webChromeClient = null
                        // Post with a generous 5-second delay to ensure graphics render pipeline and any residual input actions are fully flushed/cleared before destroying
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                if (webView.parent == null) {
                                    webView.destroy()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, 5000)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
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
            if (activeWebView != null) {
                cookieManager.setAcceptThirdPartyCookies(activeWebView, true)
            }
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
                    // Left back / forward / refresh navigation
                    IconButton(
                        onClick = { activeWebView?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("nav_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Locales.getText(appLanguage, "back"),
                            tint = if (canGoBack) colors.primary else colors.onBackground.copy(alpha = 0.25f)
                        )
                    }

                    IconButton(
                        onClick = { activeWebView?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("nav_forward")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = Locales.getText(appLanguage, "forward"),
                            tint = if (canGoForward) colors.primary else colors.onBackground.copy(alpha = 0.25f)
                        )
                    }

                    IconButton(
                        onClick = { activeWebView?.reload() },
                        enabled = activeWebView != null && activeTab?.url != "about:blank",
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("nav_refresh")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = Locales.getText(appLanguage, "refresh"),
                            tint = if (activeWebView != null && activeTab?.url != "about:blank") colors.primary else colors.onBackground.copy(alpha = 0.25f)
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
                            contentDescription = Locales.getText(appLanguage, "secure_search"),
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
                                            viewModel.updateTabInfo(tab.id, Locales.getText(appLanguage, "loading"), destinationUrl)
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
                                            text = Locales.getText(appLanguage, "search_or_type_placeholder"),
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
                                    contentDescription = Locales.getText(appLanguage, "clear"),
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
                                    contentDescription = Locales.getText(appLanguage, "refresh"),
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
                            contentDescription = Locales.getText(appLanguage, "menu"),
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
                                viewModel.updateTabInfo(tab.id, Locales.getText(appLanguage, "new_tab"), "about:blank")
                            }
                            activeWebView?.loadUrl("about:blank")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = Locales.getText(appLanguage, "home"),
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
                            contentDescription = Locales.getText(appLanguage, "add_to_bookmarks"),
                            tint = if (isCurrentBookmarked) colors.primary else colors.onBackground.copy(alpha = 0.4f)
                        )
                    }

                    // Bottom centered Add Tab quick trigger
                    IconButton(
                        onClick = { viewModel.createNewTab("about:blank", false) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = Locales.getText(appLanguage, "new_tab"),
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
                            contentDescription = Locales.getText(appLanguage, "reader_view"),
                            tint = colors.primary
                        )
                    }

                    IconButton(
                        onClick = { activeWebView?.goForward() },
                        enabled = canGoForward
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = Locales.getText(appLanguage, "forward"),
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
            val showDashboard = activeTab == null || activeTab.url == "about:blank"

            if (showDashboard) {
                LocalDashboard(
                    viewModel = viewModel,
                    colors = colors,
                    onSearchSubmit = { destination ->
                        activeTab?.let { tab ->
                            viewModel.updateTabInfo(tab.id, Locales.getText(appLanguage, "loading"), destination)
                        }
                        activeWebView?.loadUrl(destination)
                    }
                )
            }

            // Always render open WebViews inside a single container and swap views programmatically to prevent hardware acceleration black screens and broken input channels
            if (!showDashboard && activeTab != null && activeWebView != null) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { container ->
                        try {
                            val currentChild = if (container.childCount > 0) container.getChildAt(0) else null
                            if (currentChild != activeWebView) {
                                (currentChild as? WebView)?.clearFocus()
                                (currentChild as? WebView)?.onPause()
                                container.removeAllViews()
                                (activeWebView.parent as? ViewGroup)?.removeView(activeWebView)
                                container.addView(activeWebView)
                                activeWebView.onResume()
                                activeWebView.requestFocus()
                            } else {
                                activeWebView.onResume()
                            }
                            // Safe loadUrl after view attachment to prevent cold-start black screens
                            if (activeWebView.url.isNullOrEmpty() && activeTab.url.isNotEmpty() && activeTab.url != "about:blank") {
                                activeWebView.loadUrl(activeTab.url)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { container ->
                        try {
                            container.removeAllViews()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }

            // Real Reading Mode warm reader text container sheet overlay
            if (currentReaderContent != null) {
                ReadingModeReader(
                    title = currentReaderTitle ?: Locales.getText(appLanguage, "blank_page"),
                    content = currentReaderContent!!,
                    onDismiss = { viewModel.clearReaderContent() },
                    colors = colors,
                    appLanguage = appLanguage
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
                            label = Locales.getText(appLanguage, "bookmarks"),
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
                            label = Locales.getText(appLanguage, "history"),
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
                            label = Locales.getText(appLanguage, "downloads"),
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
                            label = Locales.getText(appLanguage, "extensions"),
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
                            label = Locales.getText(appLanguage, "backup_sync"),
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
                            label = Locales.getText(appLanguage, "settings"),
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
                    title = Locales.getText(appLanguage, "ad_blocker"),
                    checked = adBlockEnabled,
                    onCheckedChange = { viewModel.setAdBlockEnabled(it) },
                    colors = colors
                )

                OptionInlineSwitch(
                    title = Locales.getText(appLanguage, "always_incognito"),
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
                        text = Locales.getText(appLanguage, "active_tabs"),
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
                                contentDescription = Locales.getText(appLanguage, "add_group"),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Locales.getText(appLanguage, "add_group"), fontSize = 11.sp, color = colors.primary)
                        }

                        IconButton(
                            onClick = {
                                viewModel.createNewTab("about:blank", false)
                                showTabsSheet = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = Locales.getText(appLanguage, "add_new_tab"),
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
                        name = Locales.getText(appLanguage, "all"),
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
                            text = Locales.getText(appLanguage, "no_tabs_in_group"),
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
                                        text = if (tab.url == "about:blank") Locales.getText(appLanguage, "blank_page") else tab.url,
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
                                        contentDescription = Locales.getText(appLanguage, "close_tab"),
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
                                    title = { Text(Locales.getText(appLanguage, "save_to_group"), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = Locales.getText(appLanguage, "select_group_for_tab"),
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
                                                Text(Locales.getText(appLanguage, "none"), fontSize = 12.sp, color = colors.onBackground)
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
                                            Text(Locales.getText(appLanguage, "cancel"), color = colors.primary)
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
                        title = { Text(Locales.getText(appLanguage, "new_tab_group"), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = newGroupName,
                                    onValueChange = { newGroupName = it },
                                    label = { Text(Locales.getText(appLanguage, "group_name")) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.primary,
                                        focusedLabelColor = colors.primary
                                    )
                                )

                                Column {
                                    Text(Locales.getText(appLanguage, "group_color"), fontSize = 12.sp, color = colors.onBackground.copy(alpha = 0.5f))
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
                                Text(Locales.getText(appLanguage, "create"), color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showGroupCreateDialog = false }) {
                                Text(Locales.getText(appLanguage, "cancel"), color = colors.primary)
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
                    text = Locales.getText(appLanguage, "bookmarks"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                // Search field
                OutlinedTextField(
                    value = searchQ,
                    onValueChange = { searchQ = it },
                    placeholder = { Text(Locales.getText(appLanguage, "search_bookmarks_placeholder"), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, Locales.getText(appLanguage, "search")) },
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
                            text = if (searchQ.isEmpty()) Locales.getText(appLanguage, "no_bookmarks_yet") else Locales.getText(appLanguage, "no_results_matched"),
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
                                            viewModel.updateTabInfo(tab.id, Locales.getText(appLanguage, "loading"), bookmark.url)
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
                                        contentDescription = Locales.getText(appLanguage, "delete"),
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
                        text = Locales.getText(appLanguage, "history"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )

                    if (history.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearHistory() }
                        ) {
                            Text(Locales.getText(appLanguage, "clear_all"), fontSize = 12.sp, color = colors.primary)
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQ,
                    onValueChange = { searchQ = it },
                    placeholder = { Text(Locales.getText(appLanguage, "search_history_placeholder"), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, Locales.getText(appLanguage, "search")) },
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
                            text = if (searchQ.isEmpty()) Locales.getText(appLanguage, "no_history_yet") else Locales.getText(appLanguage, "no_results_matched"),
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
                                            viewModel.updateTabInfo(tab.id, Locales.getText(appLanguage, "loading"), hist.url)
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
                                        contentDescription = Locales.getText(appLanguage, "delete"),
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
                    text = Locales.getText(appLanguage, "downloads"),
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
                            text = Locales.getText(appLanguage, "no_downloads_yet"),
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
                                            context.startActivity(Intent.createChooser(intent, Locales.getText(appLanguage, "open_file")))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, Locales.getText(appLanguage, "open_file_failed"), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = Locales.getText(appLanguage, "file"),
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
                                        contentDescription = Locales.getText(appLanguage, "delete"),
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
                        text = Locales.getText(appLanguage, "extensions"),
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
                        Text(Locales.getText(appLanguage, "install_extension"), fontSize = 11.sp, color = Color.White)
                    }
                }

                Text(
                    text = Locales.getText(appLanguage, "extensions_desc"),
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
                                            contentDescription = Locales.getText(appLanguage, "extension"),
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
                                            Text(Locales.getText(appLanguage, "uninstall"), fontSize = 11.sp, color = colors.primary)
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
                        title = { Text(Locales.getText(appLanguage, "create_extension"), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = Locales.getText(appLanguage, "define_extension_desc"),
                                    fontSize = 11.sp,
                                    color = colors.onBackground.copy(alpha = 0.5f)
                                )
                                OutlinedTextField(
                                    value = extName,
                                    onValueChange = { extName = it },
                                    label = { Text(Locales.getText(appLanguage, "extension_name"), fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary)
                                )
                                OutlinedTextField(
                                    value = extDesc,
                                    onValueChange = { extDesc = it },
                                    label = { Text(Locales.getText(appLanguage, "extension_title"), fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary)
                                )
                                OutlinedTextField(
                                    value = extJs,
                                    onValueChange = { extJs = it },
                                    label = { Text(Locales.getText(appLanguage, "js_code"), fontSize = 11.sp) },
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
                                Text(Locales.getText(appLanguage, "validate_install"), color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddExtensionDialog = false }) {
                                Text(Locales.getText(appLanguage, "cancel"), color = colors.primary)
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
                    text = Locales.getText(appLanguage, "sync_title"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                Text(
                    text = Locales.getText(appLanguage, "sync_desc"),
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
                                Toast.makeText(context, Locales.getText(appLanguage, "sync_copied"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, "Kopya", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Locales.getText(appLanguage, "get_backup_code"), fontSize = 11.sp, color = Color.White)
                    }
                }

                Divider(color = colors.tintBorder, thickness = 0.5.dp)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Locales.getText(appLanguage, "import_backup"),
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
                        placeholder = { Text(Locales.getText(appLanguage, "paste_sync_code"), fontSize = 11.sp) },
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary)
                    )
                    Button(
                        onClick = {
                            if (syncPayload.isNotBlank()) {
                                viewModel.importBackup(
                                    syncPayload,
                                    onSuccess = {
                                        Toast.makeText(context, Locales.getText(appLanguage, "sync_done"), Toast.LENGTH_LONG).show()
                                        showSyncSheet = false
                                    },
                                    onError = {
                                        Toast.makeText(context, Locales.getText(appLanguage, "sync_failed"), Toast.LENGTH_SHORT).show()
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
                        Text(Locales.getText(appLanguage, "validate_sync"), fontSize = 11.sp, color = Color.White)
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
                    text = Locales.getText(appLanguage, "interface_settings"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                // Search Engine selector
                Column {
                    Text(
                        text = Locales.getText(appLanguage, "search_engine_option"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground.copy(alpha = 0.6f)
                    )
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
                    Text(
                        text = Locales.getText(appLanguage, "interface_accent"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground.copy(alpha = 0.6f)
                    )
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

                Divider(color = colors.tintBorder, thickness = 0.5.dp)

                // Language Selector
                Column {
                    Text(
                        text = Locales.getText(appLanguage, "app_language"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("en" to "English", "tr" to "Türkçe").forEach { (code, name) ->
                            val isSel = appLanguage == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) colors.primary else colors.background)
                                    .border(1.dp, if (isSel) Color.Transparent else colors.tintBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setAppLanguage(code) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    color = if (isSel) Color.White else colors.onBackground
                                )
                            }
                        }
                    }
                }

                Divider(color = colors.tintBorder, thickness = 0.5.dp)

                // Dark Mode Selector
                Column {
                    Text(
                        text = Locales.getText(appLanguage, "dark_mode"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "system" to Locales.getText(appLanguage, "dark_mode_system"),
                            "light" to Locales.getText(appLanguage, "dark_mode_light"),
                            "dark" to Locales.getText(appLanguage, "dark_mode_dark")
                        ).forEach { (mode, label) ->
                            val isSel = themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) colors.primary else colors.background)
                                    .border(1.dp, if (isSel) Color.Transparent else colors.tintBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setThemeMode(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (isSel) Color.White else colors.onBackground
                                )
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
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

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
                                val label = if (appLanguage == "en") {
                                    "${Locales.getText(appLanguage, "search_the_web_with")} ${viewModel.searchEngine.value}..."
                                } else {
                                    "${viewModel.searchEngine.value} ${Locales.getText(appLanguage, "search_the_web_with")}"
                                }
                                Text(
                                    text = label,
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
                        text = Locales.getText(appLanguage, "quick_access"),
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
                    DashboardStatCol(Locales.getText(appLanguage, "bookmark"), bookmarks.size.toString(), colors)
                    DashboardStatCol(Locales.getText(appLanguage, "history"), history.size.toString(), colors)
                    DashboardStatCol(Locales.getText(appLanguage, "extensions"), extensions.filter { it.isEnabled }.size.toString(), colors)
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
    colors: LesspecadColorScheme,
    appLanguage: String = "tr"
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
                    Icon(imageVector = Icons.Default.Close, contentDescription = Locales.getText(appLanguage, "close"), tint = readerText)
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
                            contentDescription = Locales.getText(appLanguage, "change_font"),
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
    return AdBlocker.isAdUrl(url)
}

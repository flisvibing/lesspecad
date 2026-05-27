package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class BrowserWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences("lesspecad_prefs", Context.MODE_PRIVATE)
        val appLang = prefs.getString("app_language", "en") ?: "en"

        val isTr = appLang == "tr"
        val placeholderText = if (isTr) "Arama yapın veya URL girin..." else "Search or enter URL..."
        val labelNewTab = if (isTr) "Yeni Sekme" else "New Tab"
        val labelIncognito = if (isTr) "Gizli Mod" else "Incognito"
        val labelBookmarks = if (isTr) "Yer İmleri" else "Bookmarks"
        val labelHistory = if (isTr) "Geçmiş" else "History"

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.browser_widget)

            // Update texts dynamically based on user selected app language
            views.setTextViewText(R.id.search_placeholder_text, placeholderText)
            views.setTextViewText(R.id.btn_new_tab_text, labelNewTab)
            views.setTextViewText(R.id.btn_incognito_text, labelIncognito)
            views.setTextViewText(R.id.btn_bookmarks_text, labelBookmarks)
            views.setTextViewText(R.id.btn_history_text, labelHistory)

            // Setup Click PendingIntents
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            // Search Bar
            val searchIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.example.ACTION_WIDGET_CLICK"
                putExtra("EXTRA_WIDGET_ACTION", "search")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val searchPending = PendingIntent.getActivity(context, 100, searchIntent, pendingFlags)
            views.setOnClickPendingIntent(R.id.search_placeholder_text, searchPending)

            // New Tab button
            val newTabIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.example.ACTION_WIDGET_CLICK"
                putExtra("EXTRA_WIDGET_ACTION", "new_tab")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val newTabPending = PendingIntent.getActivity(context, 101, newTabIntent, pendingFlags)
            views.setOnClickPendingIntent(R.id.btn_new_tab, newTabPending)

            // Incognito mode button
            val incognitoIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.example.ACTION_WIDGET_CLICK"
                putExtra("EXTRA_WIDGET_ACTION", "incognito")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val incognitoPending = PendingIntent.getActivity(context, 102, incognitoIntent, pendingFlags)
            views.setOnClickPendingIntent(R.id.btn_incognito, incognitoPending)

            // Bookmarks button
            val bookmarksIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.example.ACTION_WIDGET_CLICK"
                putExtra("EXTRA_WIDGET_ACTION", "bookmarks")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val bookmarksPending = PendingIntent.getActivity(context, 103, bookmarksIntent, pendingFlags)
            views.setOnClickPendingIntent(R.id.btn_bookmarks, bookmarksPending)

            // History button
            val historyIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.example.ACTION_WIDGET_CLICK"
                putExtra("EXTRA_WIDGET_ACTION", "history")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val historyPending = PendingIntent.getActivity(context, 104, historyIntent, pendingFlags)
            views.setOnClickPendingIntent(R.id.btn_history, historyPending)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

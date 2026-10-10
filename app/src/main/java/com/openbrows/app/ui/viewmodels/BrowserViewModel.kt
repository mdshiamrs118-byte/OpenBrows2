package com.openbrows.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.openbrows.app.data.BookmarkItem
import com.openbrows.app.data.BookmarkRepository
import com.openbrows.app.data.BrowserPreferences
import com.openbrows.app.data.HistoryRepository
import com.openbrows.app.data.TabModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = BrowserPreferences(application)
    private val bookmarks = BookmarkRepository(application)
    private val history = HistoryRepository()

    private val firstTab = newTab()
    private val _tabs = MutableStateFlow(listOf(firstTab))
    val tabs: StateFlow<List<TabModel>> = _tabs.asStateFlow()

    private val _currentTabId = MutableStateFlow(firstTab.id)
    val currentTabId: StateFlow<String> = _currentTabId.asStateFlow()

    val homepageFlow = preferences.homepageFlow
    val urlPositionFlow = preferences.urlPositionFlow
    val tabLayoutFlow = preferences.tabLayoutFlow
    val autohideFlow = preferences.autohideFlow
    val textSizeFlow = preferences.textSizeFlow
    val uiItemSizeFlow = preferences.uiItemSizeFlow
    val layoutSizeFlow = preferences.layoutSizeFlow

    suspend fun currentHomepage(): String = preferences.homepageFlow.first()

    /** A blank url means "load the homepage when the tab's WebView is created". */
    fun addTab(url: String = "") {
        val tab = newTab(url)
        _tabs.value = _tabs.value + tab
        _currentTabId.value = tab.id
    }

    /** Opens [url] in the current tab if it is still blank, otherwise in a new tab. */
    fun openUrl(url: String) {
        val current = _tabs.value.find { it.id == _currentTabId.value }
        if (current != null && current.url.isBlank()) {
            updateTab(current.id, url = url)
        } else {
            addTab(url)
        }
    }

    fun closeTab(tabId: String) {
        val list = _tabs.value
        val index = list.indexOfFirst { it.id == tabId }
        if (index < 0) return
        val remaining = list.filter { it.id != tabId }
        if (remaining.isEmpty()) {
            val blank = newTab()
            _tabs.value = listOf(blank)
            _currentTabId.value = blank.id
            return
        }
        _tabs.value = remaining
        if (_currentTabId.value == tabId) {
            _currentTabId.value = remaining[(index - 1).coerceAtLeast(0)].id
        }
    }

    fun switchTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) _currentTabId.value = tabId
    }

    fun updateTab(tabId: String, url: String? = null, title: String? = null) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(url = url ?: it.url, title = title ?: it.title) else it
        }
    }

    fun addHistory(title: String, url: String) = history.addHistory(title, url)

    fun getBookmarks(): List<BookmarkItem> = bookmarks.getAll()

    fun addBookmark(title: String, url: String): Boolean = bookmarks.add(title, url)

    private fun newTab(url: String = "") = TabModel(UUID.randomUUID().toString(), "", url)
}

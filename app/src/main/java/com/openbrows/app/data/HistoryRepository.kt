package com.openbrows.app.data

data class HistoryItem(val title: String, val url: String, val timestamp: Long)

/** In-memory browsing history for the current session. */
class HistoryRepository {

    private val historyList = mutableListOf<HistoryItem>()

    fun addHistory(title: String, url: String) {
        if (url.isBlank() || url == "about:blank") return
        if (historyList.firstOrNull()?.url == url) return
        historyList.add(0, HistoryItem(title.ifBlank { url }, url, System.currentTimeMillis()))
        if (historyList.size > MAX_ITEMS) historyList.removeAt(historyList.lastIndex)
    }

    fun getHistory(): List<HistoryItem> = historyList.toList()

    fun clearHistory() {
        historyList.clear()
    }

    private companion object {
        const val MAX_ITEMS = 500
    }
}

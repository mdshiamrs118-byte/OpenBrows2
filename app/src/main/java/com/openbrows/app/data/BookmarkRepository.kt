package com.openbrows.app.data

import android.content.Context
import com.openbrows.app.R
import org.json.JSONArray
import org.json.JSONObject

data class BookmarkItem(val title: String, val url: String)

class BookmarkRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)

    fun loadDefaultBookmarks(): List<BookmarkItem> {
        return try {
            val json = context.resources.openRawResource(R.raw.default_bookmarks)
                .bufferedReader()
                .use { it.readText() }
            parse(JSONArray(json))
        } catch (e: Exception) {
            listOf(
                BookmarkItem("Google", "https://www.google.com"),
                BookmarkItem("GitHub", "https://github.com")
            )
        }
    }

    fun loadUserBookmarks(): List<BookmarkItem> {
        return try {
            parse(JSONArray(prefs.getString(KEY_USER, "[]")))
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAll(): List<BookmarkItem> =
        (loadDefaultBookmarks() + loadUserBookmarks()).distinctBy { it.url }

    /** Returns false if the URL was blank or already bookmarked. */
    fun add(title: String, url: String): Boolean {
        if (url.isBlank() || getAll().any { it.url == url }) return false
        val array = JSONArray()
        (loadUserBookmarks() + BookmarkItem(title.ifBlank { url }, url)).forEach {
            array.put(JSONObject().put("title", it.title).put("url", it.url))
        }
        prefs.edit().putString(KEY_USER, array.toString()).apply()
        return true
    }

    private fun parse(array: JSONArray): List<BookmarkItem> =
        (0 until array.length()).map {
            val o = array.getJSONObject(it)
            BookmarkItem(o.getString("title"), o.getString("url"))
        }

    private companion object {
        const val KEY_USER = "user_bookmarks"
    }
}

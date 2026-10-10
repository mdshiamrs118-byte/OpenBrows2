package com.openbrows.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.openbrows.app.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = Constants.PREFS_NAME)

class BrowserPreferences(context: Context) {

    private val store = context.applicationContext.dataStore

    companion object {
        val HOMEPAGE_KEY = stringPreferencesKey("homepage")
        val URL_POSITION_KEY = stringPreferencesKey("url_position")
        val TAB_LAYOUT_KEY = stringPreferencesKey("tab_layout")
        val AUTOHIDE_KEY = stringPreferencesKey("autohide")
        val TEXT_SIZE_KEY = intPreferencesKey("text_size")
        val UI_ITEM_SIZE_KEY = floatPreferencesKey("ui_item_size")
        val LAYOUT_SIZE_KEY = floatPreferencesKey("layout_size")
    }

    val homepageFlow: Flow<String> = store.data.map { it[HOMEPAGE_KEY] ?: Constants.DEFAULT_HOMEPAGE }
    val urlPositionFlow: Flow<String> = store.data.map { it[URL_POSITION_KEY] ?: "bottom" }
    val tabLayoutFlow: Flow<String> = store.data.map { it[TAB_LAYOUT_KEY] ?: "off" }
    val autohideFlow: Flow<String> = store.data.map { it[AUTOHIDE_KEY] ?: "off" }
    val textSizeFlow: Flow<Int> = store.data.map { it[TEXT_SIZE_KEY] ?: 100 }
    val uiItemSizeFlow: Flow<Float> = store.data.map { it[UI_ITEM_SIZE_KEY] ?: 1.0f }
    val layoutSizeFlow: Flow<Float> = store.data.map { it[LAYOUT_SIZE_KEY] ?: 1.0f }

    suspend fun saveHomepage(homepage: String) {
        store.edit { it[HOMEPAGE_KEY] = homepage }
    }

    suspend fun saveUrlPosition(position: String) {
        store.edit { it[URL_POSITION_KEY] = position }
    }

    suspend fun saveTabLayout(layout: String) {
        store.edit { it[TAB_LAYOUT_KEY] = layout }
    }

    suspend fun saveAutohide(autohide: String) {
        store.edit { it[AUTOHIDE_KEY] = autohide }
    }

    suspend fun saveTextSize(percent: Int) {
        store.edit { it[TEXT_SIZE_KEY] = percent }
    }

    suspend fun saveUiItemSize(scale: Float) {
        store.edit { it[UI_ITEM_SIZE_KEY] = scale }
    }

    suspend fun saveLayoutSize(scale: Float) {
        store.edit { it[LAYOUT_SIZE_KEY] = scale }
    }
}

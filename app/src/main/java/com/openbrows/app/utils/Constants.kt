package com.openbrows.app.utils

import android.net.Uri

object Constants {
    const val PREFS_NAME = "openbrows_preferences"
    const val DEFAULT_HOMEPAGE = "https://www.google.com"
    const val DEFAULT_SEARCH_ENGINE = "https://www.google.com/search?q="

    private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")

    /** Turns whatever the user typed in the URL bar into a loadable URL or a search query URL. */
    fun resolveInput(raw: String): String {
        val input = raw.trim()
        return when {
            input.isEmpty() -> DEFAULT_HOMEPAGE
            input.startsWith("about:") || input.startsWith("javascript:") -> input
            SCHEME_REGEX.matches(input) && input.contains("://") -> input
            input.startsWith("localhost") -> "http://$input"
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> DEFAULT_SEARCH_ENGINE + Uri.encode(input)
        }
    }

    /** Homepage setting: blank falls back to the default, missing scheme gets https://. */
    fun normalizeHomepage(raw: String): String {
        val input = raw.trim()
        return when {
            input.isEmpty() -> DEFAULT_HOMEPAGE
            input.contains("://") -> input
            else -> "https://$input"
        }
    }
}

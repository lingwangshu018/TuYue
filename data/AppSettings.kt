package com.example.malbrowser.data

import android.content.Context
import com.example.malbrowser.util.SearchEngine

class AppSettings(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "mal_settings",
            Context.MODE_PRIVATE
        )

    var searchEngine: SearchEngine
        get() {
            val name = prefs.getString(
                "search_engine",
                SearchEngine.GOOGLE.name
            )

            return runCatching {
                SearchEngine.valueOf(name!!)
            }.getOrDefault(SearchEngine.GOOGLE)
        }

        set(value) {
            prefs.edit()
                .putString("search_engine", value.name)
                .apply()
        }

    var desktopMode: Boolean
        get() = prefs.getBoolean(
            "desktop_mode",
            false
        )

        set(value) {
            prefs.edit()
                .putBoolean("desktop_mode", value)
                .apply()
        }
}

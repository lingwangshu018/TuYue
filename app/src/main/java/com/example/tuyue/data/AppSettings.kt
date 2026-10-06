package com.example.tuyue.data

import android.content.Context
import com.example.tuyue.util.SearchEngine

class AppSettings(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "tuyue_settings",
            Context.MODE_PRIVATE
        )

    var searchEngine: SearchEngine

        get() {

            val stored =
                preferences.getString(
                    "search_engine",
                    SearchEngine.GOOGLE.name
                )

            return runCatching {

                SearchEngine.valueOf(
                    stored
                        ?: SearchEngine
                            .GOOGLE
                            .name
                )

            }.getOrDefault(
                SearchEngine.GOOGLE
            )
        }

        set(value) {

            preferences
                .edit()
                .putString(
                    "search_engine",
                    value.name
                )
                .apply()
        }
        var desktopMode: Boolean
    get() {
        return preferences.getBoolean(
            "desktop_mode",
            false
        )
    }

    set(value) {
        preferences
            .edit()
            .putBoolean(
                "desktop_mode",
                value
            )
            .apply()
    }
}

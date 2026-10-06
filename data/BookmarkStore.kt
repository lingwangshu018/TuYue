package com.example.malbrowser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BookmarkStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "mal_bookmarks",
            Context.MODE_PRIVATE
        )

    fun getAll(): List<Bookmark> {

        val raw = prefs.getString(
            "bookmarks",
            "[]"
        ) ?: "[]"

        val array = JSONArray(raw)

        return buildList {

            for (i in 0 until array.length()) {

                val item = array.getJSONObject(i)

                add(
                    Bookmark(
                        name = item.getString("name"),
                        url = item.getString("url")
                    )
                )
            }
        }
    }

    fun add(bookmark: Bookmark) {

        val list = getAll().toMutableList()

        if (list.any { it.url == bookmark.url }) {
            return
        }

        list.add(bookmark)

        save(list)
    }

    fun delete(bookmark: Bookmark) {

        val list = getAll()
            .filterNot {
                it.url == bookmark.url
            }

        save(list)
    }

    private fun save(
        bookmarks: List<Bookmark>
    ) {

        val array = JSONArray()

        bookmarks.forEach {

            array.put(
                JSONObject().apply {
                    put("name", it.name)
                    put("url", it.url)
                }
            )
        }

        prefs.edit()
            .putString(
                "bookmarks",
                array.toString()
            )
            .apply()
    }
}

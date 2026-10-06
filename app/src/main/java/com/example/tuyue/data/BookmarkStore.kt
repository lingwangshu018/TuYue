package com.example.tuyue.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BookmarkStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "tuyue_bookmarks",
            Context.MODE_PRIVATE
        )

    fun getAll(): List<Bookmark> {

        val raw =
            preferences.getString(
                "bookmarks",
                "[]"
            ) ?: "[]"

        return runCatching {

            val array =
                JSONArray(raw)

            buildList {

                for (
                    index in
                    0 until array.length()
                ) {

                    val item =
                        array.getJSONObject(index)

                    add(
                        Bookmark(
                            name =
                                item.getString(
                                    "name"
                                ),

                            url =
                                item.getString(
                                    "url"
                                )
                        )
                    )
                }
            }

        }.getOrDefault(
            emptyList()
        )
    }

    fun add(
        bookmark: Bookmark
    ) {

        val bookmarks =
            getAll()
                .toMutableList()

        /*
         * 相同网址不重复添加。
         */
        if (
            bookmarks.any {
                it.url == bookmark.url
            }
        ) {

            return
        }

        bookmarks.add(
            bookmark
        )

        save(bookmarks)
    }

    fun delete(
        bookmark: Bookmark
    ) {

        val bookmarks =
            getAll()
                .filterNot {
                    it.url ==
                        bookmark.url
                }

        save(bookmarks)
    }

    private fun save(
        bookmarks: List<Bookmark>
    ) {

        val array =
            JSONArray()

        bookmarks.forEach {
            bookmark ->

            array.put(
                JSONObject().apply {

                    put(
                        "name",
                        bookmark.name
                    )

                    put(
                        "url",
                        bookmark.url
                    )
                }
            )
        }

        preferences
            .edit()
            .putString(
                "bookmarks",
                array.toString()
            )
            .apply()
    }
}

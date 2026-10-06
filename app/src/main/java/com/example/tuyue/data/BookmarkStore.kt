package com.example.tuyue.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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

                    val iconUrl =
                        item.optString(
                            "iconUrl",
                            ""
                        ).takeIf {
                            it.isNotBlank()
                        }

                    val folderId =
                        item.optString(
                            "folderId",
                            ""
                        ).takeIf {
                            it.isNotBlank()
                        }

                    add(
                        Bookmark(
                            name =
                                item.optString(
                                    "name",
                                    "网页"
                                ),

                            url =
                                item.optString(
                                    "url",
                                    ""
                                ),

                            iconUrl =
                                iconUrl,

                            folderId =
                                folderId
                        )
                    )
                }
            }

        }.getOrDefault(
            emptyList()
        )
    }

    fun getFolders(): List<BookmarkFolder> {

        val raw =
            preferences.getString(
                "folders",
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
                        BookmarkFolder(
                            id =
                                item.getString(
                                    "id"
                                ),

                            name =
                                item.getString(
                                    "name"
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

        val existingIndex =
            bookmarks.indexOfFirst {
                it.url == bookmark.url
            }

        if (
            existingIndex >= 0
        ) {

            bookmarks[existingIndex] =
                bookmark

        } else {

            bookmarks.add(
                bookmark
            )
        }

        saveBookmarks(
            bookmarks
        )
    }

    fun delete(
        bookmark: Bookmark
    ) {

        val bookmarks =
            getAll()
                .filterNot {
                    it.url == bookmark.url
                }

        saveBookmarks(
            bookmarks
        )
    }

    fun createFolder(
        name: String
    ): BookmarkFolder {

        val folder =
            BookmarkFolder(
                id =
                    UUID.randomUUID()
                        .toString(),

                name =
                    name.trim()
                        .ifBlank {
                            "新收藏夹"
                        }
            )

        val folders =
            getFolders()
                .toMutableList()

        folders.add(
            folder
        )

        saveFolders(
            folders
        )

        return folder
    }

    fun renameFolder(
        folderId: String,
        newName: String
    ) {

        val folders =
            getFolders()
                .map {

                    if (
                        it.id == folderId
                    ) {

                        it.copy(
                            name =
                                newName.trim()
                                    .ifBlank {
                                        it.name
                                    }
                        )

                    } else {

                        it
                    }
                }

        saveFolders(
            folders
        )
    }

    fun deleteFolder(
        folderId: String
    ) {

        /*
         * 删除收藏夹时，
         * 里面的网站不会一起删除，
         * 而是自动移动回首页。
         */
        val bookmarks =
            getAll()
                .map {

                    if (
                        it.folderId ==
                        folderId
                    ) {

                        it.copy(
                            folderId = null
                        )

                    } else {

                        it
                    }
                }

        val folders =
            getFolders()
                .filterNot {
                    it.id == folderId
                }

        saveBookmarks(
            bookmarks
        )

        saveFolders(
            folders
        )
    }

    fun moveBookmark(
        bookmark: Bookmark,
        folderId: String?
    ) {

        val bookmarks =
            getAll()
                .map {

                    if (
                        it.url ==
                        bookmark.url
                    ) {

                        it.copy(
                            folderId =
                                folderId
                        )

                    } else {

                        it
                    }
                }

        saveBookmarks(
            bookmarks
        )
    }

    private fun saveBookmarks(
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

                    put(
                        "iconUrl",
                        bookmark.iconUrl ?: ""
                    )

                    put(
                        "folderId",
                        bookmark.folderId ?: ""
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

    private fun saveFolders(
        folders: List<BookmarkFolder>
    ) {

        val array =
            JSONArray()

        folders.forEach {
            folder ->

            array.put(
                JSONObject().apply {

                    put(
                        "id",
                        folder.id
                    )

                    put(
                        "name",
                        folder.name
                    )
                }
            )
        }

        preferences
            .edit()
            .putString(
                "folders",
                array.toString()
            )
            .apply()
    }
}

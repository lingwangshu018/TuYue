package com.example.tuyue.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class SearchEngine(
    val displayName: String
) {

    GOOGLE("Google"),

    BING("Bing"),

    BAIDU("百度"),

    OFF("关闭")
}

fun resolveInput(
    input: String,
    searchEngine: SearchEngine
): String? {

    val text =
        input.trim()

    /*
     * 什么都没输入。
     */
    if (text.isBlank()) {

        return null
    }

    /*
     * 已经是完整网址。
     */
    if (
        text.startsWith(
            "https://",
            ignoreCase = true
        ) ||
        text.startsWith(
            "http://",
            ignoreCase = true
        )
    ) {

        return text
    }

    /*
     * 看起来像域名。
     *
     * 例如：
     *
     * github.com
     * bilibili.com
     */
    val looksLikeDomain =

        !text.contains(" ") &&

        text.contains(".") &&

        !text.startsWith(".")

    if (looksLikeDomain) {

        return "https://$text"
    }

    /*
     * 如果用户关闭了搜索，
     * 普通文字就不处理。
     */
    if (
        searchEngine ==
        SearchEngine.OFF
    ) {

        return null
    }

    val encoded =
        URLEncoder.encode(
            text,
            StandardCharsets
                .UTF_8
                .toString()
        )

    return when (
        searchEngine
    ) {

        SearchEngine.GOOGLE ->

            "https://www.google.com/search?q=$encoded"

        SearchEngine.BING ->

            "https://www.bing.com/search?q=$encoded"

        SearchEngine.BAIDU ->

            "https://www.baidu.com/s?wd=$encoded"

        SearchEngine.OFF ->

            null
    }
}

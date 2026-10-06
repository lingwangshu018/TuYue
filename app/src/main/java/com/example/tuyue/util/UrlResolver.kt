package com.example.tuyue.util

import java.net.URLEncoder

fun resolveInput(
    input: String
): String? {

    val text = input.trim()

    if (text.isBlank()) {
        return null
    }

    /*
     * 已经是完整 HTTP / HTTPS 地址
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
     * github.com
     * bilibili.com
     * www.example.com
     */
    val looksLikeWebsite =
        !text.contains(" ") &&
        text.contains(".") &&
        !text.startsWith(".")

    if (looksLikeWebsite) {

        return "https://$text"
    }

    /*
     * 剩下的内容当成搜索关键词。
     */

    val encoded =
        URLEncoder.encode(
            text,
            Charsets.UTF_8.name()
        )

    return "https://www.google.com/search?q=$encoded"
}

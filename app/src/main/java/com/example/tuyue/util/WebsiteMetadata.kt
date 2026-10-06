package com.example.tuyue.util

import android.text.Html
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class WebsiteMetadata(
    val title: String,
    val iconUrl: String?
)

suspend fun fetchWebsiteMetadata(
    url: String
): WebsiteMetadata {

    return kotlinx.coroutines.withContext(
        kotlinx.coroutines.Dispatchers.IO
    ) {

        var connection: HttpURLConnection? = null

        try {

            val targetUrl =
                URL(url)

            connection =
                targetUrl.openConnection()
                    as HttpURLConnection

            connection.instanceFollowRedirects =
                true

            connection.connectTimeout =
                8000

            connection.readTimeout =
                8000

            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
            )

            connection.setRequestProperty(
                "Accept",
                "text/html,application/xhtml+xml"
            )

            connection.connect()

            val finalUrl =
                connection.url
                    ?.toString()
                    ?: url

            val html =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        reader ->

                        reader.readText()
                    }

            val title =
                findTitle(
                    html = html,
                    url = finalUrl
                )

            val iconUrl =
                findIcon(
                    html = html,
                    pageUrl = finalUrl
                )

            WebsiteMetadata(
                title = title,
                iconUrl = iconUrl
            )

        } catch (
            exception: Exception
        ) {

            WebsiteMetadata(
                title =
                    hostName(url),

                iconUrl =
                    defaultFavicon(url)
            )

        } finally {

            connection?.disconnect()
        }
    }
}

private fun findTitle(
    html: String,
    url: String
): String {

    val regex =
        Regex(
            pattern =
                """<title[^>]*>(.*?)</title>""",
            options =
                setOf(
                    RegexOption.IGNORE_CASE,
                    RegexOption.DOT_MATCHES_ALL
                )
        )

    val rawTitle =
        regex
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(
                Regex("\\s+"),
                " "
            )
            ?.trim()

    if (
        rawTitle.isNullOrBlank()
    ) {
        return hostName(url)
    }

    return Html
        .fromHtml(
            rawTitle,
            Html.FROM_HTML_MODE_LEGACY
        )
        .toString()
        .trim()
        .ifBlank {
            hostName(url)
        }
}

private fun findIcon(
    html: String,
    pageUrl: String
): String? {

    val linkRegex =
        Regex(
            pattern =
                """<link\b[^>]*>""",
            option =
                RegexOption.IGNORE_CASE
        )

    val relRegex =
        Regex(
            pattern =
                """rel\s*=\s*["']([^"']+)["']""",
            option =
                RegexOption.IGNORE_CASE
        )

    val hrefRegex =
        Regex(
            pattern =
                """href\s*=\s*["']([^"']+)["']""",
            option =
                RegexOption.IGNORE_CASE
        )

    val links =
        linkRegex.findAll(html)

    links.forEach {
        match ->

        val tag =
            match.value

        val rel =
            relRegex
                .find(tag)
                ?.groupValues
                ?.getOrNull(1)
                ?.lowercase()

        if (
            rel != null &&
            rel.contains("icon")
        ) {

            val href =
                hrefRegex
                    .find(tag)
                    ?.groupValues
                    ?.getOrNull(1)

            if (
                !href.isNullOrBlank()
            ) {

                return resolveUrl(
                    baseUrl = pageUrl,
                    value = href
                )
            }
        }
    }

    return defaultFavicon(
        pageUrl
    )
}

private fun resolveUrl(
    baseUrl: String,
    value: String
): String? {

    return runCatching {

        URL(
            URL(baseUrl),
            value
        ).toString()

    }.getOrNull()
}

private fun defaultFavicon(
    url: String
): String? {

    return runCatching {

        val uri =
            URI(url)

        val scheme =
            uri.scheme ?: "https"

        val host =
            uri.host
                ?: return null

        "$scheme://$host/favicon.ico"

    }.getOrNull()
}

private fun hostName(
    url: String
): String {

    return runCatching {

        URI(url)
            .host
            ?.removePrefix("www.")
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "网页"

    }.getOrDefault(
        "网页"
    )
}

package com.example.tuyue.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.tuyue.data.Bookmark
import com.example.tuyue.data.BookmarkStore
import com.example.tuyue.util.SearchEngine
import com.example.tuyue.util.fetchWebsiteMetadata
import com.example.tuyue.util.resolveInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun AddWebPageScreen(
    bookmarkStore: BookmarkStore,
    searchEngine: SearchEngine,
    onOpenUrl: (String) -> Unit,
    onSaved: () -> Unit
) {

    var name by remember {
        mutableStateOf("")
    }

    var input by remember {
        mutableStateOf("")
    }

    var iconUrl by remember {
        mutableStateOf<String?>(null)
    }

    var detectedUrl by remember {
        mutableStateOf<String?>(null)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var isDetecting by remember {
        mutableStateOf(false)
    }

    /*
     * 用户停止输入一小会儿以后，
     * 自动识别网页标题和 favicon。
     */
    LaunchedEffect(input) {

        detectedUrl = null
        iconUrl = null

        if (
            input.isBlank()
        ) {

            isDetecting = false
            return@LaunchedEffect
        }

        delay(700)

        val url =
            resolveInput(
                input,
                searchEngine
            )

        if (
            url == null
        ) {

            isDetecting = false
            return@LaunchedEffect
        }

        isDetecting = true

        val metadata =
            fetchWebsiteMetadata(
                url
            )

        detectedUrl = url
        iconUrl = metadata.iconUrl

        if (
            name.isBlank()
        ) {
            name = metadata.title
        }

        isDetecting = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "添加网页",
            style =
                MaterialTheme
                    .typography
                    .headlineLarge
        )

        Text(
            text = "输入网址，兔跃会自动识别网页名称和图标。",
            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )

        OutlinedTextField(
            value = input,

            onValueChange = {
                input = it
                message = null
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("网址")
            },

            placeholder = {
                Text("例如：bilibili.com")
            },

            singleLine = true
        )

        if (
            isDetecting
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp),

                    strokeWidth = 2.dp
                )

                Text(
                    text = "正在识别网页信息…",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }

        if (
            detectedUrl != null
        ) {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(18.dp)
            ) {

                Row(
                    modifier =
                        Modifier.padding(16.dp),

                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {

                    WebsiteIcon(
                        iconUrl = iconUrl,
                        name = name,
                        modifier =
                            Modifier.size(52.dp)
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                name.ifBlank {
                                    "网页"
                                },

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                detectedUrl ?: "",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = name,

            onValueChange = {
                name = it
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("网页名称")
            },

            supportingText = {
                Text(
                    "自动识别后仍然可以自己修改"
                )
            },

            singleLine = true
        )

        OutlinedButton(
            onClick = {

                val url =
                    resolveInput(
                        input,
                        searchEngine
                    )

                if (
                    url == null
                ) {

                    message =
                        "请输入有效的网址"

                    return@OutlinedButton
                }

                onOpenUrl(url)
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text("先打开看看")
        }

        Button(
            onClick = {

                val url =
                    detectedUrl
                        ?: resolveInput(
                            input,
                            searchEngine
                        )

                if (
                    url == null
                ) {

                    message =
                        "请输入有效的网址"

                    return@Button
                }

                val bookmarkName =
                    name
                        .trim()
                        .ifBlank {
                            input.trim()
                                .removePrefix(
                                    "https://"
                                )
                                .removePrefix(
                                    "http://"
                                )
                                .removePrefix(
                                    "www."
                                )
                                .substringBefore("/")
                        }

                bookmarkStore.add(
                    Bookmark(
                        name = bookmarkName,
                        url = url,
                        iconUrl = iconUrl
                    )
                )

                message =
                    "已添加到兔跃首页"

                onSaved()
            },

            enabled =
                input.isNotBlank() &&
                !isDetecting,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text("添加到首页")
        }

        message?.let {

            Text(
                text = it,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )
        }

        HorizontalDivider()

        Text(
            text =
                "当前搜索：${searchEngine.displayName}",

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )
    }
}

@Composable
fun WebsiteIcon(
    iconUrl: String?,
    name: String,
    modifier: Modifier = Modifier
) {

    var bitmap by remember(
        iconUrl
    ) {
        mutableStateOf<android.graphics.Bitmap?>(
            null
        )
    }

    LaunchedEffect(
        iconUrl
    ) {

        bitmap = null

        if (
            iconUrl.isNullOrBlank()
        ) {
            return@LaunchedEffect
        }

        bitmap =
            withContext(
                Dispatchers.IO
            ) {

                runCatching {

                    val connection =
                        URL(iconUrl)
                            .openConnection()

                    connection.connectTimeout =
                        6000

                    connection.readTimeout =
                        6000

                    connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 Android"
                    )

                    connection
                        .getInputStream()
                        .use {
                            stream ->

                            android.graphics.BitmapFactory
                                .decodeStream(
                                    stream
                                )
                        }

                }.getOrNull()
            }
    }

    Surface(
        modifier = modifier,

        shape =
            RoundedCornerShape(12.dp),

        tonalElevation = 2.dp
    ) {

        Box(
            modifier =
                Modifier.fillMaxSize(),

            contentAlignment =
                Alignment.Center
        ) {

            val image =
                bitmap

            if (
                image != null
            ) {

                Image(
                    bitmap =
                        image.asImageBitmap(),

                    contentDescription =
                        name,

                    modifier =
                        Modifier.fillMaxSize(),

                    contentScale =
                        ContentScale.Fit
                )

            } else {

                Text(
                    text =
                        name
                            .trim()
                            .firstOrNull()
                            ?.uppercase()
                            ?: "🌐",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )
            }
        }
    }
}

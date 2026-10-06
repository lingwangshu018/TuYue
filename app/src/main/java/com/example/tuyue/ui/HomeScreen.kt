package com.example.tuyue.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tuyue.data.Bookmark
import com.example.tuyue.data.BookmarkStore
import com.example.tuyue.util.SearchEngine
import com.example.tuyue.util.resolveInput

@Composable
fun HomeScreen(
    bookmarkStore: BookmarkStore,
    searchEngine: SearchEngine,
    onOpenUrl: (String) -> Unit
) {

    var input by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var bookmarks by remember {
        mutableStateOf(
            bookmarkStore.getAll()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        verticalArrangement =
            Arrangement.spacedBy(20.dp)
    ) {

        Text(
            text = "兔跃",

            style =
                MaterialTheme
                    .typography
                    .headlineLarge
        )

        Text(
            text = "网页，从这里出发",

            style =
                MaterialTheme
                    .typography
                    .bodyLarge
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(20.dp),

                verticalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text =
                        "快速打开 / 添加网页",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )

                OutlinedTextField(
                    value = input,

                    onValueChange = {
                        input = it
                        message = null
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true,

                    label = {
                        Text(
                            "搜索或输入网址"
                        )
                    }
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    OutlinedButton(
                        modifier =
                            Modifier.weight(1f),

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
                                    "请输入网址，或者开启搜索"

                            } else {

                                onOpenUrl(url)
                            }
                        }
                    ) {

                        Text("直接打开")
                    }

                    Button(
                        modifier =
                            Modifier.weight(1f),

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
                                    "无法识别这个地址"

                                return@Button
                            }

                            val host =
                                runCatching {

                                    Uri.parse(url)
                                        .host
                                        ?.removePrefix(
                                            "www."
                                        )

                                }.getOrNull()

                            val name =
                                if (
                                    host.isNullOrBlank()
                                ) {
                                    "网页"
                                } else {
                                    host
                                }

                            bookmarkStore.add(
                                Bookmark(
                                    name = name,
                                    url = url
                                )
                            )

                            bookmarks =
                                bookmarkStore.getAll()

                            message =
                                "已添加到首页"
                        }
                    ) {

                        Text("快速添加")
                    }
                }

                Text(
                    text =
                        "需要自动识别标题和网站图标时，请使用底部“添加”页面。",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                Text(
                    text =
                        "当前搜索：${searchEngine.displayName}",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                message?.let {

                    Text(
                        text = it,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "我的网页",

                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            Text(
                text =
                    "${bookmarks.size} 个网页"
            )
        }

        if (
            bookmarks.isEmpty()
        ) {

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(24.dp)
                ) {

                    Text(
                        text =
                            "还没有保存网页",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "可以使用底部“添加”，让兔跃自动识别网页名称和图标。"
                    )
                }
            }

        } else {

            bookmarks.forEach {
                bookmark ->

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
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
                            iconUrl =
                                bookmark.iconUrl,

                            name =
                                bookmark.name,

                            modifier =
                                Modifier.size(52.dp)
                        )

                        Column(
                            modifier =
                                Modifier.weight(1f),

                            verticalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {

                            Text(
                                text =
                                    bookmark.name,

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )

                            Text(
                                text =
                                    bookmark.url,

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )

                            Row {

                                TextButton(
                                    onClick = {

                                        onOpenUrl(
                                            bookmark.url
                                        )
                                    }
                                ) {

                                    Text("打开")
                                }

                                TextButton(
                                    onClick = {

                                        bookmarkStore
                                            .delete(
                                                bookmark
                                            )

                                        bookmarks =
                                            bookmarkStore
                                                .getAll()
                                    }
                                ) {

                                    Text("删除")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

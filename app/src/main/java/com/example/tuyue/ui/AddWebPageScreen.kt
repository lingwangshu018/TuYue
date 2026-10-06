package com.example.tuyue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tuyue.data.Bookmark
import com.example.tuyue.data.BookmarkStore
import com.example.tuyue.util.SearchEngine
import com.example.tuyue.util.resolveInput

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

    var message by remember {
        mutableStateOf<String?>(null)
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
            text = "把常用网页保存到兔跃首页",
            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        OutlinedTextField(
            value = name,

            onValueChange = {
                name = it
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("网页名称（可选）")
            },

            singleLine = true
        )

        OutlinedTextField(
            value = input,

            onValueChange = {
                input = it
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

        Button(
            onClick = {

                val url =
                    resolveInput(
                        input,
                        searchEngine
                    )

                if (url == null) {

                    message =
                        "请输入有效的网址"

                    return@Button
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
                    resolveInput(
                        input,
                        searchEngine
                    )

                if (url == null) {

                    message =
                        "请输入有效的网址"

                    return@Button
                }

                val bookmarkName =
                    if (name.isBlank()) {
                        input.trim()
                            .removePrefix("https://")
                            .removePrefix("http://")
                            .removePrefix("www.")
                            .substringBefore("/")
                    } else {
                        name.trim()
                    }

                bookmarkStore.add(
                    Bookmark(
                        name = bookmarkName,
                        url = url
                    )
                )

                message =
                    "已添加到兔跃首页"

                onSaved()
            },

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

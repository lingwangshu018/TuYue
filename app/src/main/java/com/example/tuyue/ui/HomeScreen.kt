package com.example.tuyue.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tuyue.util.resolveInput

@Composable
fun HomeScreen(
    onOpenUrl: (String) -> Unit
) {

    var input by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        verticalArrangement =
            Arrangement.spacedBy(24.dp)
    ) {

        Column {

            Text(
                text = "兔跃",
                style =
                    MaterialTheme.typography.headlineLarge
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "网页，从这里出发",
                style =
                    MaterialTheme.typography.bodyLarge
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(20.dp),

                verticalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "快速打开",
                    style =
                        MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "输入网址，也可以直接搜索内容。"
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
                        Text("搜索或输入网址")
                    },

                    placeholder = {
                        Text("例如 github.com")
                    }
                )

                Button(
                    modifier =
                        Modifier.fillMaxWidth(),

                    onClick = {

                        val url =
                            resolveInput(
                                input,
                                searchEngine
                            )

                        if (url == null) {

                            message =
                                "先输入一个网址或搜索内容"

                        } else {

                            onOpenUrl(url)
                        }
                    }
                ) {

                    Text("打开网页")
                }

                message?.let {

                    Text(
                        text = it,
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Text(
            text = "现在支持网址和 Google 搜索。",
            style =
                MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "收藏、搜索引擎切换和设置会在下一步加入。",
            style =
                MaterialTheme.typography.bodySmall
        )
    }
}

package com.example.tuyue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tuyue.data.AppSettings
import com.example.tuyue.util.SearchEngine

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit
) {

    var searchEngine by remember {
        mutableStateOf(
            settings.searchEngine
        )
    }

    var desktopMode by remember {
        mutableStateOf(
            settings.desktopMode
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),

        verticalArrangement =
            Arrangement.spacedBy(20.dp)
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            OutlinedButton(
                onClick = onBack
            ) {
                Text("返回")
            }

            Column {

                Text(
                    text = "设置",
                    style =
                        MaterialTheme
                            .typography
                            .headlineLarge
                )

                Text(
                    text = "兔跃的浏览和搜索设置",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )
            }
        }

        HorizontalDivider()

        Text(
            text = "地址栏搜索",
            style =
                MaterialTheme
                    .typography
                    .titleLarge
        )

        SearchEngine.values().forEach { engine ->

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                RadioButton(
                    selected =
                        searchEngine == engine,

                    onClick = {

                        searchEngine =
                            engine

                        settings.searchEngine =
                            engine
                    }
                )

                Text(
                    text =
                        engine.displayName
                )
            }
        }

        HorizontalDivider()

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        "默认桌面版网页",

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
                        "打开网页时默认使用电脑浏览器模式",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            Switch(
                checked = desktopMode,

                onCheckedChange = { enabled ->

                    desktopMode =
                        enabled

                    settings.desktopMode =
                        enabled
                }
            )
        }

        HorizontalDivider()

        Text(
            text = "下载文件",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Text(
            text =
                "普通网页文件将保存到 Download/兔跃。",

            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )
    }
}

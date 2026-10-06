package com.example.tuyue.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.tuyue.data.Bookmark
import com.example.tuyue.data.BookmarkFolder
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
        mutableStateOf<String?>(
            null
        )
    }

    var detectedUrl by remember {
        mutableStateOf<String?>(
            null
        )
    }

    var message by remember {
        mutableStateOf<String?>(
            null
        )
    }

    var isDetecting by remember {
        mutableStateOf(false)
    }

    var folders by remember {
        mutableStateOf(
            bookmarkStore.getFolders()
        )
    }

    var selectedFolderId by remember {
        mutableStateOf<String?>(
            null
        )
    }

    var folderMenuExpanded by remember {
        mutableStateOf(false)
    }

    var showNewFolderDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(
        input
    ) {

        detectedUrl = null
        iconUrl = null

        if (
            input.isBlank()
        ) {

            isDetecting = false
            return@LaunchedEffect
        }

        delay(
            700
        )

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

        detectedUrl =
            url

        iconUrl =
            metadata.iconUrl

        if (
            name.isBlank()
        ) {

            name =
                metadata.title
        }

        isDetecting =
            false
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    20.dp
                ),

        verticalArrangement =
            Arrangement.spacedBy(
                16.dp
            )
    ) {

        Text(
            text = "添加网页",

            style =
                MaterialTheme
                    .typography
                    .headlineLarge
        )

        Text(
            text =
                "把网页放到首页，或者整理进收藏夹。",

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
                Text(
                    "例如：bilibili.com"
                )
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
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(
                            20.dp
                        ),

                    strokeWidth =
                        2.dp
                )

                Text(
                    "正在识别网页信息…"
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
                    RoundedCornerShape(
                        18.dp
                    )
            ) {

                Row(
                    modifier =
                        Modifier.padding(
                            16.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            14.dp
                        )
                ) {

                    WebsiteIcon(
                        iconUrl =
                            iconUrl,

                        name =
                            name,

                        modifier =
                            Modifier.size(
                                52.dp
                            )
                    )

                    Column(
                        modifier =
                            Modifier.weight(
                                1f
                            )
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
                                Modifier.height(
                                    4.dp
                                )
                        )

                        Text(
                            text =
                                detectedUrl
                                    ?: "",

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

            singleLine = true
        )

        Text(
            text = "保存位置",

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Box(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            OutlinedButton(
                onClick = {

                    folderMenuExpanded =
                        true
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                val selectedFolder =
                    folders.firstOrNull {
                        it.id ==
                            selectedFolderId
                    }

                Text(
                    text =
                        if (
                            selectedFolder ==
                            null
                        ) {

                            "🏠 首页"

                        } else {

                            "📁 ${selectedFolder.name}"
                        }
                )
            }

            DropdownMenu(
                expanded =
                    folderMenuExpanded,

                onDismissRequest = {

                    folderMenuExpanded =
                        false
                }
            ) {

                DropdownMenuItem(
                    text = {
                        Text(
                            "🏠 首页"
                        )
                    },

                    onClick = {

                        selectedFolderId =
                            null

                        folderMenuExpanded =
                            false
                    }
                )

                folders.forEach {
                    folder ->

                    DropdownMenuItem(
                        text = {

                            Text(
                                "📁 ${folder.name}"
                            )
                        },

                        onClick = {

                            selectedFolderId =
                                folder.id

                            folderMenuExpanded =
                                false
                        }
                    )
                }

                HorizontalDivider()

                DropdownMenuItem(
                    text = {
                        Text(
                            "＋ 新建收藏夹"
                        )
                    },

                    onClick = {

                        folderMenuExpanded =
                            false

                        showNewFolderDialog =
                            true
                    }
                )
            }
        }

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

                onOpenUrl(
                    url
                )
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

                            input
                                .trim()
                                .removePrefix(
                                    "https://"
                                )
                                .removePrefix(
                                    "http://"
                                )
                                .removePrefix(
                                    "www."
                                )
                                .substringBefore(
                                    "/"
                                )
                        }

                bookmarkStore.add(
                    Bookmark(
                        name =
                            bookmarkName,

                        url =
                            url,

                        iconUrl =
                            iconUrl,

                        folderId =
                            selectedFolderId
                    )
                )

                onSaved()
            },

            enabled =
                input.isNotBlank() &&
                    !isDetecting,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text("添加")
        }

        message?.let {

            Text(
                text = it,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall
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

    if (
        showNewFolderDialog
    ) {

        NewFolderDialog(
            onDismiss = {

                showNewFolderDialog =
                    false
            },

            onCreate = {
                folderName ->

                val folder =
                    bookmarkStore
                        .createFolder(
                            folderName
                        )

                folders =
                    bookmarkStore
                        .getFolders()

                selectedFolderId =
                    folder.id

                showNewFolderDialog =
                    false
            }
        )
    }
}

@Composable
private fun NewFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {

    var name by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,

        title = {
            Text(
                "新建收藏夹"
            )
        },

        text = {

            OutlinedTextField(
                value = name,

                onValueChange = {
                    name = it
                },

                label = {
                    Text(
                        "收藏夹名称"
                    )
                },

                placeholder = {
                    Text(
                        "例如：学习"
                    )
                },

                singleLine = true
            )
        },

        confirmButton = {

            TextButton(
                onClick = {

                    if (
                        name.isNotBlank()
                    ) {

                        onCreate(
                            name.trim()
                        )
                    }
                }
            ) {

                Text("创建")
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text("取消")
            }
        }
    )
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

        mutableStateOf<
            android.graphics.Bitmap?
        >(
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
                        URL(
                            iconUrl
                        ).openConnection()

                    connection
                        .connectTimeout =
                        6000

                    connection
                        .readTimeout =
                        6000

                    connection
                        .setRequestProperty(
                            "User-Agent",
                            "Mozilla/5.0 Android"
                        )

                    connection
                        .getInputStream()
                        .use {
                            stream ->

                            android
                                .graphics
                                .BitmapFactory
                                .decodeStream(
                                    stream
                                )
                        }

                }.getOrNull()
            }
    }

    Surface(
        modifier =
            modifier,

        shape =
            RoundedCornerShape(
                14.dp
            ),

        tonalElevation =
            2.dp
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

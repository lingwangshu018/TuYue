package com.example.tuyue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tuyue.data.Bookmark
import com.example.tuyue.data.BookmarkFolder
import com.example.tuyue.data.BookmarkStore
import com.example.tuyue.util.SearchEngine
import com.example.tuyue.util.resolveInput

private sealed class DesktopItem {

    data class Website(
        val bookmark: Bookmark
    ) : DesktopItem()

    data class Folder(
        val folder: BookmarkFolder
    ) : DesktopItem()
}

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

    var folders by remember {
        mutableStateOf(
            bookmarkStore.getFolders()
        )
    }

    var openedFolder by remember {
        mutableStateOf<BookmarkFolder?>(
            null
        )
    }

    var selectedBookmark by remember {
        mutableStateOf<Bookmark?>(
            null
        )
    }

    var selectedFolder by remember {
        mutableStateOf<BookmarkFolder?>(
            null
        )
    }

    fun refresh() {

        bookmarks =
            bookmarkStore.getAll()

        folders =
            bookmarkStore.getFolders()
    }

    val desktopItems =
        buildList {

            bookmarks
                .filter {
                    it.folderId == null
                }
                .forEach {

                    add(
                        DesktopItem.Website(
                            it
                        )
                    )
                }

            folders.forEach {

                add(
                    DesktopItem.Folder(
                        it
                    )
                )
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 20.dp
            )
    ) {

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        Text(
            text = "兔跃",

            style =
                MaterialTheme
                    .typography
                    .headlineLarge
        )

        Text(
            text =
                "网页，从这里出发",

            style =
                MaterialTheme
                    .typography
                    .bodyMedium,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(18.dp)
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

            shape =
                RoundedCornerShape(
                    22.dp
                ),

            label = {
                Text(
                    "搜索或输入网址"
                )
            }
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

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
                            "请输入网址或搜索内容"

                    } else {

                        onOpenUrl(
                            url
                        )
                    }
                },

                modifier =
                    Modifier.weight(1f)
            ) {

                Text("打开")
            }

            Button(
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

                    val name =
                        runCatching {

                            android.net.Uri
                                .parse(url)
                                .host
                                ?.removePrefix(
                                    "www."
                                )

                        }.getOrNull()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "网页"

                    bookmarkStore.add(
                        Bookmark(
                            name = name,
                            url = url
                        )
                    )

                    refresh()

                    message =
                        "已添加到首页"
                },

                modifier =
                    Modifier.weight(1f)
            ) {

                Text("快速添加")
            }
        }

        message?.let {

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text = it,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

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
                    "${bookmarks.size} 个",

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        if (
            desktopItems.isEmpty()
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),

                contentAlignment =
                    Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "🌐",

                        style =
                            MaterialTheme
                                .typography
                                .displaySmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            "这里还是空的",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "从底部“添加”保存你的第一个网页",

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }

        } else {

            LazyVerticalGrid(
                columns =
                    GridCells.Fixed(4),

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        20.dp
                    )
            ) {

                items(
                    desktopItems
                ) {
                    item ->

                    when (item) {

                        is DesktopItem.Website -> {

                            WebsiteDesktopItem(
                                bookmark =
                                    item.bookmark,

                                onClick = {

                                    onOpenUrl(
                                        item
                                            .bookmark
                                            .url
                                    )
                                },

                                onLongClickMenu = {

                                    selectedBookmark =
                                        item.bookmark
                                }
                            )
                        }

                        is DesktopItem.Folder -> {

                            val folderBookmarks =
                                bookmarks.filter {

                                    it.folderId ==
                                        item.folder.id
                                }

                            FolderDesktopItem(
                                folder =
                                    item.folder,

                                bookmarks =
                                    folderBookmarks,

                                onClick = {

                                    openedFolder =
                                        item.folder
                                },

                                onLongClickMenu = {

                                    selectedFolder =
                                        item.folder
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    openedFolder?.let {
        folder ->

        FolderDialog(
            folder = folder,

            bookmarks =
                bookmarks.filter {
                    it.folderId ==
                        folder.id
                },

            onDismiss = {

                openedFolder =
                    null
            },

            onOpenUrl = {
                url ->

                openedFolder =
                    null

                onOpenUrl(
                    url
                )
            },

            onBookmarkMenu = {
                bookmark ->

                selectedBookmark =
                    bookmark
            }
        )
    }

    selectedBookmark?.let {
        bookmark ->

        BookmarkMenuDialog(
            bookmark = bookmark,
            folders = folders,

            onDismiss = {

                selectedBookmark =
                    null
            },

            onMove = {
                folderId ->

                bookmarkStore
                    .moveBookmark(
                        bookmark,
                        folderId
                    )

                selectedBookmark =
                    null

                openedFolder =
                    null

                refresh()
            },

            onDelete = {

                bookmarkStore
                    .delete(
                        bookmark
                    )

                selectedBookmark =
                    null

                openedFolder =
                    null

                refresh()
            }
        )
    }

    selectedFolder?.let {
        folder ->

        FolderMenuDialog(
            folder = folder,

            onDismiss = {

                selectedFolder =
                    null
            },

            onDelete = {

                bookmarkStore
                    .deleteFolder(
                        folder.id
                    )

                selectedFolder =
                    null

                refresh()
            }
        )
    }
}

@Composable
private fun WebsiteDesktopItem(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onLongClickMenu: () -> Unit
) {

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally,

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Box {

            Surface(
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(
                            RoundedCornerShape(
                                17.dp
                            )
                        )
                        .clickable {
                            onClick()
                        },

                shape =
                    RoundedCornerShape(
                        17.dp
                    ),

                tonalElevation =
                    2.dp
            ) {

                WebsiteIcon(
                    iconUrl =
                        bookmark.iconUrl,

                    name =
                        bookmark.name,

                    modifier =
                        Modifier.fillMaxSize()
                )
            }

            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .clickable {
                            menuExpanded =
                                true
                        }
            )

            DropdownMenu(
                expanded =
                    menuExpanded,

                onDismissRequest = {

                    menuExpanded =
                        false
                }
            ) {

                DropdownMenuItem(
                    text = {
                        Text("打开")
                    },

                    onClick = {

                        menuExpanded =
                            false

                        onClick()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("管理")
                    },

                    onClick = {

                        menuExpanded =
                            false

                        onLongClickMenu()
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(7.dp)
        )

        Text(
            text =
                bookmark.name,

            style =
                MaterialTheme
                    .typography
                    .bodySmall,

            maxLines = 1,

            overflow =
                TextOverflow.Ellipsis,

            textAlign =
                TextAlign.Center,

            modifier =
                Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FolderDesktopItem(
    folder: BookmarkFolder,
    bookmarks: List<Bookmark>,
    onClick: () -> Unit,
    onLongClickMenu: () -> Unit
) {

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally,

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Box {

            Surface(
                modifier =
                    Modifier
                        .size(64.dp)
                        .clickable {
                            onClick()
                        },

                shape =
                    RoundedCornerShape(
                        18.dp
                    ),

                color =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                        .copy(
                            alpha = 0.55f
                        )
            ) {

                Box(
                    modifier =
                        Modifier.padding(
                            8.dp
                        )
                ) {

                    val previews =
                        bookmarks.take(4)

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(
                                4.dp
                            )
                    ) {

                        for (
                            row in 0..1
                        ) {

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        4.dp
                                    )
                            ) {

                                for (
                                    column in 0..1
                                ) {

                                    val index =
                                        row * 2 +
                                            column

                                    val bookmark =
                                        previews
                                            .getOrNull(
                                                index
                                            )

                                    if (
                                        bookmark != null
                                    ) {

                                        WebsiteIcon(
                                            iconUrl =
                                                bookmark
                                                    .iconUrl,

                                            name =
                                                bookmark
                                                    .name,

                                            modifier =
                                                Modifier
                                                    .size(
                                                        22.dp
                                                    )
                                        )

                                    } else {

                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(
                                                        22.dp
                                                    )
                                                    .background(
                                                        Color.Transparent
                                                    )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .clickable {

                            menuExpanded =
                                true
                        }
            )

            DropdownMenu(
                expanded =
                    menuExpanded,

                onDismissRequest = {

                    menuExpanded =
                        false
                }
            ) {

                DropdownMenuItem(
                    text = {
                        Text("打开收藏夹")
                    },

                    onClick = {

                        menuExpanded =
                            false

                        onClick()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("管理")
                    },

                    onClick = {

                        menuExpanded =
                            false

                        onLongClickMenu()
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(7.dp)
        )

        Text(
            text =
                folder.name,

            style =
                MaterialTheme
                    .typography
                    .bodySmall,

            maxLines = 1,

            overflow =
                TextOverflow.Ellipsis,

            textAlign =
                TextAlign.Center,

            modifier =
                Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FolderDialog(
    folder: BookmarkFolder,
    bookmarks: List<Bookmark>,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onBookmarkMenu: (Bookmark) -> Unit
) {

    AlertDialog(
        onDismissRequest =
            onDismiss,

        title = {

            Text(
                folder.name
            )
        },

        text = {

            if (
                bookmarks.isEmpty()
            ) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                120.dp
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        "这个收藏夹还是空的"
                    )
                }

            } else {

                LazyVerticalGrid(
                    columns =
                        GridCells.Fixed(3),

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                300.dp
                            ),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            18.dp
                        )
                ) {

                    items(
                        bookmarks
                    ) {
                        bookmark ->

                        WebsiteDesktopItem(
                            bookmark =
                                bookmark,

                            onClick = {

                                onOpenUrl(
                                    bookmark.url
                                )
                            },

                            onLongClickMenu = {

                                onBookmarkMenu(
                                    bookmark
                                )
                            }
                        )
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text("关闭")
            }
        }
    )
}

@Composable
private fun BookmarkMenuDialog(
    bookmark: Bookmark,
    folders: List<BookmarkFolder>,
    onDismiss: () -> Unit,
    onMove: (String?) -> Unit,
    onDelete: () -> Unit
) {

    AlertDialog(
        onDismissRequest =
            onDismiss,

        title = {

            Text(
                bookmark.name
            )
        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {

                Text(
                    text =
                        "移动到",

                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )

                TextButton(
                    onClick = {

                        onMove(
                            null
                        )
                    }
                ) {

                    Text("首页")
                }

                folders.forEach {
                    folder ->

                    TextButton(
                        onClick = {

                            onMove(
                                folder.id
                            )
                        }
                    ) {

                        Text(
                            folder.name
                        )
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text("取消")
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDelete
            ) {

                Text("删除网页")
            }
        }
    )
}

@Composable
private fun FolderMenuDialog(
    folder: BookmarkFolder,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {

    AlertDialog(
        onDismissRequest =
            onDismiss,

        title = {

            Text(
                folder.name
            )
        },

        text = {

            Text(
                "删除收藏夹后，里面的网站会自动移回首页。"
            )
        },

        confirmButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text("取消")
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDelete
            ) {

                Text("删除收藏夹")
            }
        }
    )
}

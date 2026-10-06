package com.example.tuyue

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tuyue.data.AppSettings
import com.example.tuyue.data.Bookmark
import com.example.tuyue.data.BookmarkStore
import com.example.tuyue.ui.AddWebPageScreen
import com.example.tuyue.ui.BrowserScreen
import com.example.tuyue.ui.HomeScreen
import com.example.tuyue.ui.SettingsScreen

private sealed class TuYuePage {

    data object Home : TuYuePage()

    data object AddWebPage : TuYuePage()

    data class Browser(
        val url: String
    ) : TuYuePage()

    data object Settings : TuYuePage()
}

@Composable
fun TuYueApp() {

    val context = LocalContext.current

    val settings = remember {
        AppSettings(context)
    }

    val bookmarkStore = remember {
        BookmarkStore(context)
    }

    var page by remember {
        mutableStateOf<TuYuePage>(
            TuYuePage.Home
        )
    }

    var previousPage by remember {
        mutableStateOf<TuYuePage>(
            TuYuePage.Home
        )
    }

    val openSettings = {

        if (page != TuYuePage.Settings) {
            previousPage = page
        }

        page = TuYuePage.Settings
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            when (val current = page) {

                TuYuePage.Home -> {

                    HomeScreen(
                        bookmarkStore = bookmarkStore,

                        searchEngine =
                            settings.searchEngine,

                        onOpenUrl = { url ->

                            page =
                                TuYuePage.Browser(
                                    url = url
                                )
                        }
                    )
                }

                TuYuePage.AddWebPage -> {

                    AddWebPageScreen(
                        bookmarkStore = bookmarkStore,

                        searchEngine =
                            settings.searchEngine,

                        onOpenUrl = { url ->

                            page =
                                TuYuePage.Browser(
                                    url = url
                                )
                        },

                        onSaved = {

                            page =
                                TuYuePage.Home
                        }
                    )
                }

                is TuYuePage.Browser -> {

                    BrowserScreen(
                        initialUrl =
                            current.url,

                        initialDesktopMode =
                            settings.desktopMode,

                        onDesktopModeChanged = {

                            settings.desktopMode =
                                it
                        },

                        onHome = {

                            page =
                                TuYuePage.Home
                        },

                        onAddWebPage = {

                            page =
                                TuYuePage.AddWebPage
                        },

                        onSettings = {

                            openSettings()
                        },

                        onSaveToHome = { name, url ->

                            bookmarkStore.add(
                                Bookmark(
                                    name = name,
                                    url = url
                                )
                            )

                            Toast.makeText(
                                context,
                                "已保存到兔跃首页",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                TuYuePage.Settings -> {

                    BackHandler {

                        page =
                            previousPage
                    }

                    SettingsScreen(
                        settings = settings,

                        onBack = {

                            page =
                                previousPage
                        }
                    )
                }
            }
        }

        CompactBottomBar(
            currentPage = page,

            onHome = {

                page =
                    TuYuePage.Home
            },

            onAdd = {

                page =
                    TuYuePage.AddWebPage
            },

            onSettings = {

                openSettings()
            }
        )
    }
}

@Composable
private fun CompactBottomBar(
    currentPage: TuYuePage,
    onHome: () -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit
) {

    Surface(
        tonalElevation = 3.dp
    ) {

        Column {

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(58.dp)
                    .padding(
                        horizontal = 8.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.SpaceEvenly
            ) {

                TextButton(
                    onClick = onHome,

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "首页",

                        fontWeight =
                            if (
                                currentPage ==
                                TuYuePage.Home
                            ) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                    )
                }

                TextButton(
                    onClick = onAdd,

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "添加",

                        fontWeight =
                            if (
                                currentPage ==
                                TuYuePage.AddWebPage
                            ) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                    )
                }

                TextButton(
                    onClick = onSettings,

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "设置",

                        fontWeight =
                            if (
                                currentPage ==
                                TuYuePage.Settings
                            ) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                    )
                }
            }
        }
    }
}

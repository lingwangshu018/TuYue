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
fun TuYueApp(
    browserDisplayMode: BrowserDisplayMode,
    onBrowserDisplayModeChanged:
        (BrowserDisplayMode) -> Unit
) {

    val context =
        LocalContext.current

    val settings =
        remember {

            AppSettings(
                context
            )
        }

    val bookmarkStore =
        remember {

            BookmarkStore(
                context
            )
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

        if (
            page !=
            TuYuePage.Settings
        ) {

            previousPage =
                page
        }

        page =
            TuYuePage.Settings
    }

    /*
     * 只有浏览器页面允许全屏。
     *
     * 离开浏览器以后，
     * 自动恢复普通系统栏。
     */
    LaunchedEffect(
        page
    ) {

        if (
            page !is
            TuYuePage.Browser
        ) {

            onBrowserDisplayModeChanged(
                BrowserDisplayMode.NORMAL
            )
        }
    }

    val isBrowser =
        page is
        TuYuePage.Browser

    val isFullscreen =
        isBrowser &&
        browserDisplayMode !=
        BrowserDisplayMode.NORMAL

    /*
     * ---------------------------
     * 最外层布局
     * ---------------------------
     *
     * NORMAL：
     * 给顶部状态栏留安全区域。
     *
     * FULLSCREEN：
     * 状态栏仍显示，所以顶部仍然留区域；
     * 但是底部完全不留 navigation bar inset。
     *
     * IMMERSIVE：
     * 上下都不留系统栏区域。
     *
     * 注意：
     * 这里故意没有给整个 Column 使用
     * navigationBarsPadding()。
     *
     * 底部安全区域只由普通模式下的
     * CompactBottomBar 自己处理。
     */
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .then(

                    if (
                        isBrowser &&
                        browserDisplayMode ==
                        BrowserDisplayMode.IMMERSIVE
                    ) {

                        /*
                         * 沉浸式：
                         * 不留顶部状态栏空间。
                         */
                        Modifier

                    } else {

                        /*
                         * 普通页面 / 普通全屏：
                         * 顶部状态栏仍存在。
                         */
                        Modifier
                            .statusBarsPadding()
                    }
                )
    ) {

        /*
         * 页面内容区域
         *
         * 全屏时不加任何底部 padding，让 WebView 真正铺满到底部。
         */
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
        ) {

            when (
                val current =
                    page
            ) {

                /*
                 * 首页
                 */
                TuYuePage.Home -> {

                    HomeScreen(
                        bookmarkStore =
                            bookmarkStore,

                        searchEngine =
                            settings
                                .searchEngine,

                        onOpenUrl = {
                            url ->

                            page =
                                TuYuePage.Browser(
                                    url =
                                        url
                                )
                        }
                    )
                }

                /*
                 * 添加网页
                 */
                TuYuePage.AddWebPage -> {

                    AddWebPageScreen(
                        bookmarkStore =
                            bookmarkStore,

                        searchEngine =
                            settings
                                .searchEngine,

                        onOpenUrl = {
                            url ->

                            page =
                                TuYuePage.Browser(
                                    url =
                                        url
                                )
                        },

                        onSaved = {

                            page =
                                TuYuePage.Home
                        }
                    )
                }

                /*
                 * 浏览器
                 */
                is TuYuePage.Browser -> {

                    BrowserScreen(
                        initialUrl =
                            current.url,

                        initialDesktopMode =
                            settings
                                .desktopMode,

                        displayMode =
                            browserDisplayMode,

                        onDisplayModeChanged = {

                            onBrowserDisplayModeChanged(
                                it
                            )
                        },

                        onDesktopModeChanged = {

                            settings
                                .desktopMode =
                                it
                        },

                        onHome = {

                            onBrowserDisplayModeChanged(
                                BrowserDisplayMode.NORMAL
                            )

                            page =
                                TuYuePage.Home
                        },

                        onAddWebPage = {

                            onBrowserDisplayModeChanged(
                                BrowserDisplayMode.NORMAL
                            )

                            page =
                                TuYuePage.AddWebPage
                        },

                        onSettings = {

                            onBrowserDisplayModeChanged(
                                BrowserDisplayMode.NORMAL
                            )

                            openSettings()
                        },

                        onSaveToHome = {
                            name,
                            url ->

                            bookmarkStore.add(
                                Bookmark(
                                    name =
                                        name,

                                    url =
                                        url
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

                /*
                 * 设置
                 */
                TuYuePage.Settings -> {

                    BackHandler {

                        page =
                            previousPage
                    }

                    SettingsScreen(
                        settings =
                            settings,

                        onBack = {

                            page =
                                previousPage
                        }
                    )
                }
            }
        }

        /*
         * ---------------------------
         * App 底部导航
         * ---------------------------
         *
         * 只有普通模式显示。
         *
         * 一旦浏览器进入：
         *
         * FULLSCREEN
         * 或
         * IMMERSIVE
         *
         * 整个底栏从 Compose 布局中删除。
         *
         * 这样它不会继续占高度，
         * WebView 会真正获得底部空间。
         */
        if (
            !isFullscreen
        ) {

            CompactBottomBar(
                currentPage =
                    page,

                onHome = {

                    onBrowserDisplayModeChanged(
                        BrowserDisplayMode.NORMAL
                    )

                    page =
                        TuYuePage.Home
                },

                onAdd = {

                    onBrowserDisplayModeChanged(
                        BrowserDisplayMode.NORMAL
                    )

                    page =
                        TuYuePage.AddWebPage
                },

                onSettings = {

                    onBrowserDisplayModeChanged(
                        BrowserDisplayMode.NORMAL
                    )

                    openSettings()
                }
            )
        }
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
        tonalElevation =
            3.dp
    ) {

        Column {

            HorizontalDivider()

            /*
             * navigationBarsPadding()
             *
             * 只存在于普通模式的底栏。
             *
             * 全屏时 CompactBottomBar
             * 整个都不会进入 Composition，
             * 所以 navigation bar inset
             * 不会留下任何灰色占位。
             */
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(
                            58.dp
                        )
                        .padding(
                            horizontal =
                                8.dp
                        ),

                verticalAlignment =
                    Alignment
                        .CenterVertically,

                horizontalArrangement =
                    Arrangement
                        .SpaceEvenly
            ) {

                /*
                 * 首页
                 */
                TextButton(
                    onClick =
                        onHome,

                    modifier =
                        Modifier
                            .weight(1f)
                ) {

                    Text(
                        text =
                            "首页",

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

                /*
                 * 添加
                 */
                TextButton(
                    onClick =
                        onAdd,

                    modifier =
                        Modifier
                            .weight(1f)
                ) {

                    Text(
                        text =
                            "添加",

                        fontWeight =
                            if (
                                currentPage ==
                                TuYuePage
                                    .AddWebPage
                            ) {

                                FontWeight.Bold

                            } else {

                                FontWeight.Normal
                            }
                    )
                }

                /*
                 * 设置
                 */
                TextButton(
                    onClick =
                        onSettings,

                    modifier =
                        Modifier
                            .weight(1f)
                ) {

                    Text(
                        text =
                            "设置",

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

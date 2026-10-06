package com.example.tuyue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.tuyue.data.AppSettings
import com.example.tuyue.data.BookmarkStore
import com.example.tuyue.ui.BrowserScreen
import com.example.tuyue.ui.HomeScreen

private sealed class TuYuePage {

    data object Home : TuYuePage()

    data class Browser(
        val url: String
    ) : TuYuePage()
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

    when (val currentPage = page) {

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

        is TuYuePage.Browser -> {

            BrowserScreen(
                initialUrl =
                    currentPage.url,

                initialDesktopMode =
                    settings.desktopMode,

                onDesktopModeChanged = { enabled ->

                    settings.desktopMode =
                        enabled
                },

                onHome = {

                    page =
                        TuYuePage.Home
                }
            )
        }
    }
}

package com.example.tuyue

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var page by remember {
        mutableStateOf<TuYuePage>(
            TuYuePage.Home
        )
    }

    when (val currentPage = page) {

        TuYuePage.Home -> {

            HomeScreen(
                onOpenUrl = { url ->

                    page =
                        TuYuePage.Browser(url)
                }
            )
        }

        is TuYuePage.Browser -> {

            BackHandler {
                page = TuYuePage.Home
            }

            BrowserScreen(
                initialUrl = currentPage.url,

                onHome = {
                    page = TuYuePage.Home
                }
            )
        }
    }
}

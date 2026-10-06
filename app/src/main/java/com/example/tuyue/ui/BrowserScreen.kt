package com.example.tuyue.ui

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/140.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    initialUrl: String,
    initialDesktopMode: Boolean,
    onDesktopModeChanged: (Boolean) -> Unit,
    onHome: () -> Unit
) {

    var webView by remember {
        mutableStateOf<WebView?>(null)
    }

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    var desktopMode by remember {
        mutableStateOf(initialDesktopMode)
    }

    var progress by remember {
        mutableIntStateOf(0)
    }

    BackHandler {

        val view = webView

        if (view?.canGoBack() == true) {

            view.goBack()

        } else {

            onHome()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),

            factory = { context ->

                WebView(context).apply {

                    webView = this

                    settings.apply {

                        javaScriptEnabled = true

                        domStorageEnabled = true

                        databaseEnabled = true

                        javaScriptCanOpenWindowsAutomatically = true

                        loadsImagesAutomatically = true

                        useWideViewPort = true

                        loadWithOverviewMode = true

                        builtInZoomControls = true

                        displayZoomControls = false

                        userAgentString =
                            if (desktopMode) {
                                DESKTOP_USER_AGENT
                            } else {
                                null
                            }
                    }

                    webViewClient =
                        WebViewClient()

                    webChromeClient =
                        object : WebChromeClient() {

                            override fun onProgressChanged(
                                view: WebView?,
                                newProgress: Int
                            ) {

                                progress = newProgress
                            }
                        }

                    loadUrl(initialUrl)
                }
            },

            onRelease = { view ->

                view.stopLoading()

                view.loadUrl("about:blank")

                view.removeAllViews()

                view.destroy()

                webView = null
            }
        )

        if (progress in 1..99) {

            LinearProgressIndicator(
                progress = {
                    progress / 100f
                }
            )
        }

        Column(
            modifier =
                Modifier
                    .align(
                        Alignment.CenterEnd
                    )
                    .padding(
                        end = 12.dp
                    ),

            horizontalAlignment =
                Alignment.End
        ) {

            if (menuExpanded) {

                Card(
                    modifier =
                        Modifier
                            .width(210.dp)
                            .padding(
                                bottom = 10.dp
                            ),

                    shape =
                        RoundedCornerShape(
                            20.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                vertical = 8.dp
                            )
                    ) {

                        BrowserMenuItem(
                            text = "⌂  回到主页"
                        ) {

                            menuExpanded = false

                            onHome()
                        }

                        BrowserMenuItem(
                            text = "→  前进"
                        ) {

                            menuExpanded = false

                            val view = webView

                            if (
                                view?.canGoForward() ==
                                true
                            ) {

                                view.goForward()
                            }
                        }

                        BrowserMenuItem(
                            text = "↻  刷新"
                        ) {

                            menuExpanded = false

                            webView?.reload()
                        }

                        BrowserMenuItem(
                            text =
                                if (desktopMode) {
                                    "▣  切换手机版"
                                } else {
                                    "▣  桌面版网页"
                                }
                        ) {

                            menuExpanded = false

                            desktopMode =
                                !desktopMode

                            onDesktopModeChanged(
                                desktopMode
                            )

                            webView?.let { view ->

                                view.settings
                                    .userAgentString =
                                    if (desktopMode) {

                                        DESKTOP_USER_AGENT

                                    } else {

                                        null
                                    }

                                view.reload()
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {

                    menuExpanded =
                        !menuExpanded
                },

                shape =
                    RoundedCornerShape(
                        18.dp
                    )
            ) {

                Text(
                    text =
                        if (menuExpanded) {
                            "×"
                        } else {
                            "⋮"
                        },

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )
            }
        }
    }
}

@Composable
private fun BrowserMenuItem(
    text: String,
    onClick: () -> Unit
) {

    TextButton(
        onClick = onClick
    ) {

        Row(
            modifier =
                Modifier
                    .width(180.dp)
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
        ) {

            Text(
                text = text
            )
        }
    }
}

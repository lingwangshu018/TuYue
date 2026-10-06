package com.example.tuyue.ui

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onHome: () -> Unit
) {

    var webView by remember {
        mutableStateOf<WebView?>(null)
    }

    var pageTitle by remember {
        mutableStateOf("网页")
    }

    var progress by remember {
        mutableIntStateOf(0)
    }

    var desktopMode by remember {
        mutableStateOf(false)
    }

    /*
     * Android 系统返回键。
     *
     * 网页有历史记录：
     * 返回上一页。
     *
     * 没有历史记录：
     * 回兔跃首页。
     */
    BackHandler {

        val view = webView

        if (
            view != null &&
            view.canGoBack()
        ) {

            view.goBack()

        } else {

            onHome()
        }
    }

    DisposableEffect(Unit) {

        onDispose {

            webView?.apply {

                stopLoading()

                webChromeClient = null

                webViewClient =
                    WebViewClient()

                loadUrl("about:blank")

                clearHistory()

                removeAllViews()

                destroy()
            }

            webView = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Surface(
            tonalElevation = 3.dp
        ) {

            Column {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 6.dp,
                            vertical = 4.dp
                        ),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    TextButton(
                        onClick = {

                            val view = webView

                            if (
                                view != null &&
                                view.canGoBack()
                            ) {

                                view.goBack()

                            } else {

                                onHome()
                            }
                        }
                    ) {

                        Text("返回")
                    }

                    TextButton(
                        onClick = {

                            webView?.reload()
                        }
                    ) {

                        Text("刷新")
                    }

                    TextButton(
                        onClick = onHome
                    ) {

                        Text("首页")
                    }

                    TextButton(
                        onClick = {

                            desktopMode =
                                !desktopMode

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
                    ) {

                        Text(
                            if (desktopMode) {
                                "手机版"
                            } else {
                                "桌面版"
                            }
                        )
                    }
                }

                Text(
                    text = pageTitle,

                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 4.dp
                        ),

                    maxLines = 1,

                    style =
                        MaterialTheme.typography.bodyMedium
                )

                if (progress in 1..99) {

                    LinearProgressIndicator(
                        progress = {
                            progress / 100f
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
            }
        }

        AndroidView(
            modifier =
                Modifier.fillMaxSize(),

            factory = { context ->

                WebView(context).apply {

                    webView = this

                    settings.apply {

                        javaScriptEnabled = true

                        domStorageEnabled = true

                        databaseEnabled = true

                        loadsImagesAutomatically = true

                        useWideViewPort = true

                        loadWithOverviewMode = true

                        builtInZoomControls = true

                        displayZoomControls = false
                    }

                    webViewClient =
                        WebViewClient()

                    webChromeClient =
                        object :
                            WebChromeClient() {

                            override fun onReceivedTitle(
                                view: WebView?,
                                title: String?
                            ) {

                                pageTitle =
                                    title ?: "网页"
                            }

                            override fun onProgressChanged(
                                view: WebView?,
                                newProgress: Int
                            ) {

                                progress =
                                    newProgress
                            }
                        }

                    loadUrl(initialUrl)
                }
            }
        )
    }
}

package com.example.malbrowser.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val DESKTOP_UA =
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

    var desktopMode by remember {
        mutableStateOf(initialDesktopMode)
    }

    var pageTitle by remember {
        mutableStateOf("网页")
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Surface(
            tonalElevation = 2.dp
        ) {

            Column {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    TextButton(
                        onClick = {
                            val view = webView

                            if (view?.canGoBack() == true) {
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

                            onDesktopModeChanged(
                                desktopMode
                            )

                            webView?.let { view ->

                                view.settings.userAgentString =
                                    if (desktopMode) {
                                        DESKTOP_UA
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
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 4.dp
                    ),
                    maxLines = 1
                )

                if (progress in 1..99) {

                    LinearProgressIndicator(
                        progress = {
                            progress / 100f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),

            factory = { context ->

                WebView(context).apply {

                    webView = this

                    settings.apply {

                        javaScriptEnabled = true

                        domStorageEnabled = true

                        databaseEnabled = true

                        javaScriptCanOpenWindowsAutomatically =
                            true

                        loadsImagesAutomatically = true

                        useWideViewPort = true

                        loadWithOverviewMode = true

                        builtInZoomControls = true

                        displayZoomControls = false

                        userAgentString =
                            if (desktopMode) {
                                DESKTOP_UA
                            } else {
                                null
                            }
                    }

                    webViewClient =
                        WebViewClient()

                    webChromeClient =
                        object : WebChromeClient() {

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

                    setDownloadListener {
                            url,
                            userAgent,
                            contentDisposition,
                            mimeType,
                            _ ->

                        downloadFile(
                            context = context,
                            url = url,
                            userAgent = userAgent,
                            contentDisposition =
                                contentDisposition,
                            mimeType = mimeType
                        )
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
    }
}

private fun downloadFile(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?
) {

    if (
        !url.startsWith("http://") &&
        !url.startsWith("https://")
    ) {

        Toast.makeText(
            context,
            "暂不支持这种下载类型",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    val fileName =
        URLUtil.guessFileName(
            url,
            contentDisposition,
            mimeType
        )

    val request =
        DownloadManager.Request(
            Uri.parse(url)
        ).apply {

            setTitle(fileName)

            setNotificationVisibility(
                DownloadManager.Request
                    .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )

            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "MAL/$fileName"
            )

            userAgent?.let {
                addRequestHeader(
                    "User-Agent",
                    it
                )
            }

            setAllowedOverMetered(true)

            setAllowedOverRoaming(true)
        }

    val manager =
        context.getSystemService(
            Context.DOWNLOAD_SERVICE
        ) as DownloadManager

    manager.enqueue(request)

    Toast.makeText(
        context,
        "开始下载：$fileName",
        Toast.LENGTH_SHORT
    ).show()
}

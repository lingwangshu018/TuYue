
package com.example.tuyue.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onHome: () -> Unit,
    onAddWebPage: () -> Unit,
    onSettings: () -> Unit,
    onSaveToHome: (String, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var webView by remember {
        mutableStateOf<WebView?>(null)
    }

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    var desktopMode by remember {
        mutableStateOf(initialDesktopMode)
    }

    var pageTitle by remember {
        mutableStateOf("网页")
    }

    var currentUrl by remember {
        mutableStateOf(initialUrl)
    }

    var canGoForward by remember {
        mutableStateOf(false)
    }

    var progress by remember {
        mutableIntStateOf(0)
    }

    BackHandler {
        if (menuExpanded) {
            menuExpanded = false
        } else {
            val view = webView
            if (view?.canGoBack() == true) {
                view.goBack()
            } else {
                onHome()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // 顶部固定工具栏
        Surface(
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
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
                    Text(
                        text = "‹",
                        fontSize = 30.sp
                    )
                }

                Text(
                    text = pageTitle,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )

                // 顶栏最右边的下拉菜单
                Box {

                    IconButton(
                        onClick = {
                            menuExpanded = true
                        }
                    ) {
                        Text(
                            text = "⋮",
                            fontSize = 26.sp
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = {
                            menuExpanded = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = { Text("回到主页") },
                            onClick = {
                                menuExpanded = false
                                onHome()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("前进") },
                            enabled = canGoForward,
                            onClick = {
                                menuExpanded = false
                                webView?.goForward()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("刷新") },
                            onClick = {
                                menuExpanded = false
                                webView?.reload()
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("保存到首页") },
                            onClick = {
                                menuExpanded = false

                                val url =
                                    webView?.url ?: currentUrl

                                val name =
                                    pageTitle.ifBlank { "网页" }

                                if (
                                    url.startsWith("http://") ||
                                    url.startsWith("https://")
                                ) {
                                    onSaveToHome(name, url)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "当前页面无法收藏",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("添加网页") },
                            onClick = {
                                menuExpanded = false
                                onAddWebPage()
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (desktopMode) {
                                        "切换手机版网页"
                                    } else {
                                        "切换桌面版网页"
                                    }
                                )
                            },
                            onClick = {
                                menuExpanded = false

                                desktopMode = !desktopMode

                                onDesktopModeChanged(
                                    desktopMode
                                )

                                webView?.let { view ->
                                    view.settings.userAgentString =
                                        if (desktopMode) {
                                            DESKTOP_USER_AGENT
                                        } else {
                                            null
                                        }

                                    view.reload()
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("浏览器设置") },
                            onClick = {
                                menuExpanded = false
                                onSettings()
                            }
                        )
                    }
                }
            }
        }

        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = {
                    progress / 100f
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 网页自动占满剩余空间
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),

            factory = { ctx ->
                WebView(ctx).apply {

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

                        userAgentString =
                            if (desktopMode) {
                                DESKTOP_USER_AGENT
                            } else {
                                null
                            }
                    }

                    CookieManager.getInstance()
                        .setAcceptCookie(true)

                    webViewClient =
                        object : WebViewClient() {

                            override fun onPageFinished(
                                view: WebView?,
                                url: String?
                            ) {
                                currentUrl =
                                    url ?: currentUrl

                                canGoForward =
                                    view?.canGoForward() == true

                                val title = view?.title
                                if (!title.isNullOrBlank()) {
                                    pageTitle = title
                                }
                            }
                        }

                    webChromeClient =
                        object : WebChromeClient() {

                            override fun onReceivedTitle(
                                view: WebView?,
                                title: String?
                            ) {
                                if (!title.isNullOrBlank()) {
                                    pageTitle = title
                                }
                            }

                            override fun onProgressChanged(
                                view: WebView?,
                                newProgress: Int
                            ) {
                                progress = newProgress
                            }
                        }

                    setDownloadListener {
                        url,
                        userAgent,
                        contentDisposition,
                        mimeType,
                        _ ->

                        downloadFile(
                            context = ctx,
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
        !url.startsWith("https://", true) &&
        !url.startsWith("http://", true)
    ) {
        Toast.makeText(
            context,
            "暂不支持 blob/data 下载",
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    try {
        val fileName = URLUtil.guessFileName(
            url,
            contentDisposition,
            mimeType
        ).replace("/", "_")
            .replace("\\", "_")

        val request = DownloadManager.Request(
            Uri.parse(url)
        ).apply {

            setTitle(fileName)

            setMimeType(
                mimeType ?: "application/octet-stream"
            )

            setNotificationVisibility(
                DownloadManager.Request
                    .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )

            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "兔跃/$fileName"
            )

            userAgent?.let {
                addRequestHeader("User-Agent", it)
            }

            val cookies =
                CookieManager.getInstance()
                    .getCookie(url)

            if (!cookies.isNullOrBlank()) {
                addRequestHeader("Cookie", cookies)
            }
        }

        val manager = context.getSystemService(
            Context.DOWNLOAD_SERVICE
        ) as DownloadManager

        manager.enqueue(request)

        Toast.makeText(
            context,
            "开始下载：$fileName",
            Toast.LENGTH_SHORT
        ).show()

    } catch (e: Exception) {
        Toast.makeText(
            context,
            "下载失败：${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}

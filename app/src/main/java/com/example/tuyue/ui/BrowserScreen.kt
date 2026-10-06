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
import com.example.tuyue.BrowserDisplayMode

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
    "AppleWebKit/537.36 (KHTML, like Gecko) " +
    "Chrome/140.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    initialUrl: String,
    initialDesktopMode: Boolean,

    displayMode: BrowserDisplayMode,

    onDisplayModeChanged:
        (BrowserDisplayMode) -> Unit,

    onDesktopModeChanged:
        (Boolean) -> Unit,

    onHome: () -> Unit,

    onAddWebPage: () -> Unit,

    onSettings: () -> Unit,

    onSaveToHome:
        (String, String) -> Unit
) {

    val context =
        androidx.compose.ui.platform
            .LocalContext.current

    var webView by remember {
        mutableStateOf<WebView?>(null)
    }

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    var desktopMode by remember {
        mutableStateOf(
            initialDesktopMode
        )
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

    val isFullscreen =
        displayMode !=
        BrowserDisplayMode.NORMAL

    /*
     * 返回键优先级：
     *
     * 1. 关闭菜单
     * 2. 退出全屏
     * 3. 网页后退
     * 4. 返回兔跃首页
     */
    BackHandler {

        when {

            menuExpanded -> {

                menuExpanded = false
            }

            isFullscreen -> {

                onDisplayModeChanged(
                    BrowserDisplayMode.NORMAL
                )
            }

            webView?.canGoBack() == true -> {

                webView?.goBack()
            }

            else -> {

                onHome()
            }
        }
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {

        /*
         * 普通模式显示网页工具栏。
         *
         * 全屏和沉浸式全屏：
         * 整个工具栏直接从布局中移除。
         */
        if (!isFullscreen) {

            Surface(
                tonalElevation = 2.dp
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(
                            horizontal = 8.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    /*
                     * 返回
                     */
                    TextButton(
                        onClick = {

                            val view =
                                webView

                            if (
                                view?.canGoBack() ==
                                true
                            ) {

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

                    /*
                     * 网页标题
                     */
                    Text(
                        text = pageTitle,

                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                horizontal = 4.dp
                            ),

                        maxLines = 1,

                        overflow =
                            TextOverflow.Ellipsis,

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    /*
                     * 右上角菜单
                     */
                    Box {

                        IconButton(
                            onClick = {

                                menuExpanded =
                                    true
                            }
                        ) {

                            Text(
                                text = "⋮",
                                fontSize = 26.sp
                            )
                        }

                        DropdownMenu(
                            expanded =
                                menuExpanded,

                            onDismissRequest = {

                                menuExpanded =
                                    false
                            }
                        ) {

                            /*
                             * 回到主页
                             */
                            DropdownMenuItem(
                                text = {

                                    Text(
                                        "回到主页"
                                    )
                                },

                                onClick = {

                                    menuExpanded =
                                        false

                                    onHome()
                                }
                            )

                            /*
                             * 前进
                             */
                            DropdownMenuItem(
                                text = {

                                    Text(
                                        "前进"
                                    )
                                },

                                enabled =
                                    canGoForward,

                                onClick = {

                                    menuExpanded =
                                        false

                                    webView
                                        ?.goForward()
                                }
                            )

                            /*
                             * 刷新
                             */
                            DropdownMenuItem(
                                text = {

                                    Text(
                                        "刷新"
                                    )
                                },

                                onClick = {

                                    menuExpanded =
                                        false

                                    webView
                                        ?.reload()
                                }
                            )

                            HorizontalDivider()

                            /*
                             * 全屏
                             *
                             * 保留顶部系统时间栏
                             * 隐藏底部系统导航栏
                             */
                            DropdownMenuItem(
                                text = {

                                    Text(
                                        "全屏"
                                    )
                                },

                                onClick = {

                                    menuExpanded =
                                        false

                                    onDisplayModeChanged(
                                        BrowserDisplayMode
                                            .FULLSCREEN
                                    )
                                }
                            )

                            /*
                             * 沉浸式全屏
                             *
                             * 状态栏和导航栏
                             * 全部隐藏
                             */
                            DropdownMenuItem(
                                text = {

                                    Text(
                                        "沉浸式全屏"
                                    )
                                },

                                onClick = {

                                    menuExpanded =
                                        false

                                    onDisplayModeChanged(
                                        BrowserDisplayMode
                                            .IMMERSIVE
                                    )
                                }
                            )

                            HorizontalDivider()

                            /*
                             * 保存到首页
                             */
                            DropdownMenuItem(
                                text = {

                                    Text(
                                        "保存到首页"
                                    )
                                },

                                onClick = {

                                    menuExpanded =
                                        false

                                    val url =
                                        webView?.url
                                            ?: currentUrl

                                    val name =
                                        pageTitle
                                            .ifBlank {
                                                "网页"
                                            }

                                    if (
                                        url.startsWith(
                                            "http://"
                                        ) ||
                                        url.startsWith(
                                            "https://"
                                        )
                                    ) {

                                        onSaveToHome(
                                            name,
                                            url
                                        )

                                    } else {

                                        Toast
                                            .makeText(
                                                context,
                                                "当前页面无法收藏",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                }
                            )

                            /*
                             * 添加网页
                             */
                            DropdownMenuItem(
                                text = {

                                    Text(
                                        "添加网页"
                                    )
                                },

                                onClick = {

                                    menuExpanded =
                                        false

                                    onAddWebPage()
                                }
                            )

                            HorizontalDivider()

                            /*
                             * 桌面版 / 手机版
                             */
                            DropdownMenuItem(
                                text = {

                                    Text(
                                        if (
                                            desktopMode
                                        ) {

                                            "切换手机版网页"

                                        } else {

                                            "切换桌面版网页"
                                        }
                                    )
                                },

                                onClick = {

                                    menuExpanded =
                                        false

                                    desktopMode =
                                        !desktopMode

                                    onDesktopModeChanged(
                                        desktopMode
                                    )

                                    webView?.let {
                                        view ->

                                        view.settings
                                            .userAgentString =
                                            if (
                                                desktopMode
                                            ) {

                                                DESKTOP_USER_AGENT

                                            } else {

                                                null
                                            }

                                        view.reload()
                                    }
                                }
                            )

                            /*
                             * 浏览器设置
                             */
                            DropdownMenuItem(
                                text = {

                                    Text(
                                        "浏览器设置"
                                    )
                                },

                                onClick = {

                                    menuExpanded =
                                        false

                                    onSettings()
                                }
                            )
                        }
                    }
                }
            }
        }

        /*
         * 网页加载进度
         */
        if (
            progress in 1..99
        ) {

            LinearProgressIndicator(
                progress = {

                    progress / 100f
                },

                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        /*
         * WebView
         *
         * 普通模式：
         * 占工具栏和底栏之间的区域。
         *
         * 全屏：
         * 工具栏和底栏消失，
         * WebView 自动扩大。
         *
         * 沉浸式：
         * WebView 真正铺满屏幕。
         */
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),

            factory = { ctx ->

                WebView(ctx).apply {

                    webView = this

                    settings.apply {

                        javaScriptEnabled =
                            true

                        domStorageEnabled =
                            true

                        databaseEnabled =
                            true

                        loadsImagesAutomatically =
                            true

                        useWideViewPort =
                            true

                        loadWithOverviewMode =
                            true

                        builtInZoomControls =
                            true

                        displayZoomControls =
                            false

                        userAgentString =
                            if (
                                desktopMode
                            ) {

                                DESKTOP_USER_AGENT

                            } else {

                                null
                            }
                    }

                    /*
                     * Cookie
                     */
                    CookieManager
                        .getInstance()
                        .setAcceptCookie(
                            true
                        )

                    /*
                     * WebViewClient
                     */
                    webViewClient =
                        object :
                            WebViewClient() {

                            override fun onPageFinished(
                                view:
                                    WebView?,
                                url:
                                    String?
                            ) {

                                currentUrl =
                                    url
                                        ?: currentUrl

                                canGoForward =
                                    view
                                        ?.canGoForward() ==
                                    true

                                val title =
                                    view?.title

                                if (
                                    !title
                                        .isNullOrBlank()
                                ) {

                                    pageTitle =
                                        title
                                }
                            }
                        }

                    /*
                     * WebChromeClient
                     */
                    webChromeClient =
                        object :
                            WebChromeClient() {

                            override fun onReceivedTitle(
                                view:
                                    WebView?,
                                title:
                                    String?
                            ) {

                                if (
                                    !title
                                        .isNullOrBlank()
                                ) {

                                    pageTitle =
                                        title
                                }
                            }

                            override fun onProgressChanged(
                                view:
                                    WebView?,
                                newProgress:
                                    Int
                            ) {

                                progress =
                                    newProgress
                            }
                        }

                    /*
                     * 下载
                     */
                    setDownloadListener {
                        url,
                        userAgent,
                        contentDisposition,
                        mimeType,
                        _ ->

                        downloadFile(
                            context =
                                ctx,

                            url =
                                url,

                            userAgent =
                                userAgent,

                            contentDisposition =
                                contentDisposition,

                            mimeType =
                                mimeType
                        )
                    }

                    /*
                     * 打开网页
                     */
                    loadUrl(
                        initialUrl
                    )
                }
            },

            onRelease = {
                view ->

                view.stopLoading()

                view.loadUrl(
                    "about:blank"
                )

                view.removeAllViews()

                view.destroy()

                webView =
                    null
            }
        )
    }
}

/*
 * 下载文件
 */
private fun downloadFile(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?
) {

    /*
     * 暂时只处理 http / https。
     */
    if (
        !url.startsWith(
            "https://",
            true
        ) &&
        !url.startsWith(
            "http://",
            true
        )
    ) {

        Toast.makeText(
            context,
            "暂不支持 blob/data 下载",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    try {

        /*
         * 获取文件名
         */
        val fileName =
            URLUtil.guessFileName(
                url,
                contentDisposition,
                mimeType
            )
                .replace(
                    "/",
                    "_"
                )
                .replace(
                    "\\",
                    "_"
                )

        /*
         * 创建系统下载任务
         */
        val request =
            DownloadManager.Request(
                Uri.parse(url)
            ).apply {

                setTitle(
                    fileName
                )

                setMimeType(
                    mimeType
                        ?: "application/octet-stream"
                )

                setNotificationVisibility(
                    DownloadManager
                        .Request
                        .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )

                /*
                 * 下载位置：
                 *
                 * Downloads/兔跃/
                 */
                setDestinationInExternalPublicDir(
                    Environment
                        .DIRECTORY_DOWNLOADS,

                    "兔跃/$fileName"
                )

                /*
                 * User-Agent
                 */
                userAgent?.let {

                    addRequestHeader(
                        "User-Agent",
                        it
                    )
                }

                /*
                 * Cookie
                 *
                 * 一些登录网站下载文件时需要。
                 */
                val cookies =
                    CookieManager
                        .getInstance()
                        .getCookie(
                            url
                        )

                if (
                    !cookies.isNullOrBlank()
                ) {

                    addRequestHeader(
                        "Cookie",
                        cookies
                    )
                }
            }

        /*
         * 开始下载
         */
        val manager =
            context.getSystemService(
                Context.DOWNLOAD_SERVICE
            ) as DownloadManager

        manager.enqueue(
            request
        )

        Toast.makeText(
            context,
            "开始下载：$fileName",
            Toast.LENGTH_SHORT
        ).show()

    } catch (
        e: Exception
    ) {

        Toast.makeText(
            context,
            "下载失败：${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}

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
import kotlinx.coroutines.delay

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/140.0.0.0 Safari/537.36"

private const val SHORT_PAGE_FIX_SCRIPT = """
(function () {
    try {
        const html = document.documentElement;
        const body = document.body;

        if (!html || !body) return;

        const viewportHeight = window.innerHeight;

        const bodyHeight =
            body.getBoundingClientRect().height;

        /*
         * 只处理“网页本身比视口矮”的情况。
         * 长网页完全不碰。
         */
        if (bodyHeight + 1 < viewportHeight) {

            html.style.minHeight = '100%';
            body.style.minHeight = '100vh';

            /*
             * 常见 SPA 根节点。
             * 只有它本身接近整个 body 高度时才补高，
             * 避免随便修改网页内部组件。
             */
            const candidates = [
                document.getElementById('app'),
                document.getElementById('root'),
                document.getElementById('__next')
            ].filter(Boolean);

            candidates.forEach(function (element) {

                const rect =
                    element.getBoundingClientRect();

                if (
                    rect.top <= 1 &&
                    rect.height <= viewportHeight
                ) {
                    element.style.minHeight = '100vh';
                }
            });
        }

        window.dispatchEvent(
            new Event('resize')
        );

    } catch (e) {
    }
})();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    initialUrl: String,
    initialDesktopMode: Boolean,
    displayMode: BrowserDisplayMode,
    onDisplayModeChanged: (BrowserDisplayMode) -> Unit,
    onDesktopModeChanged: (Boolean) -> Unit,
    onHome: () -> Unit,
    onAddWebPage: () -> Unit,
    onSettings: () -> Unit,
    onSaveToHome: (String, String) -> Unit
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

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

    val isFullscreen =
        displayMode != BrowserDisplayMode.NORMAL

    /*
     * 全屏/沉浸式切换后，等待系统栏和 Compose 布局稳定，
     * 再让网页按新的 viewport 重新计算。
     *
     * SHORT_PAGE_FIX_SCRIPT 只会给“内容本身短于视口”的页面补最小高度，
     * 长页面不会被修改。
     */
    LaunchedEffect(
        displayMode,
        webView
    ) {
        val view =
            webView ?: return@LaunchedEffect

        view.post {
            view.requestLayout()
            view.invalidate()
        }

        delay(100)

        view.post {
            view.requestLayout()
            view.invalidate()
            view.evaluateJavascript(
                SHORT_PAGE_FIX_SCRIPT,
                null
            )
        }

        delay(250)

        view.post {
            view.requestLayout()
            view.invalidate()
            view.evaluateJavascript(
                SHORT_PAGE_FIX_SCRIPT,
                null
            )
        }

        delay(500)

        view.post {
            view.requestLayout()
            view.invalidate()
            view.evaluateJavascript(
                SHORT_PAGE_FIX_SCRIPT,
                null
            )
        }
    }

    /*
     * ----------------------------------------------------
     * 返回键
     * ----------------------------------------------------
     */
    BackHandler {

        when {

            menuExpanded -> {

                menuExpanded =
                    false
            }

            isFullscreen -> {

                onDisplayModeChanged(
                    BrowserDisplayMode.NORMAL
                )
            }

            webView?.canGoBack() ==
                true -> {

                webView?.goBack()
            }

            else -> {

                onHome()
            }
        }
    }

    /*
     * 浏览器主体保持 fillMaxSize。
     * 全屏时只隐藏顶部栏，不额外占用 WebView 高度。
     */
    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        /*
         * ------------------------------------------------
         * 原来的浏览器主体
         * ------------------------------------------------
         */
        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            /*
             * 普通模式显示顶部浏览器栏。
             */
            if (!isFullscreen) {

                Surface(
                    tonalElevation =
                        2.dp
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(
                                    horizontal =
                                        8.dp
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
                                text =
                                    "‹",

                                fontSize =
                                    30.sp
                            )
                        }

                        /*
                         * 网页标题
                         */
                        Text(
                            text =
                                pageTitle,

                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(
                                        horizontal =
                                            4.dp
                                    ),

                            maxLines =
                                1,

                            overflow =
                                TextOverflow.Ellipsis,

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )

                        /*
                         * 菜单
                         */
                        Box {

                            IconButton(
                                onClick = {

                                    menuExpanded =
                                        true
                                }
                            ) {

                                Text(
                                    text =
                                        "⋮",

                                    fontSize =
                                        26.sp
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

                                            Toast.makeText(
                                                context,
                                                "当前页面无法收藏",
                                                Toast.LENGTH_SHORT
                                            ).show()
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
                                 * 手机版 / 桌面版
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
                                 * 设置
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
             * 加载进度。
             */
            if (
                progress in 1..99
            ) {

                LinearProgressIndicator(
                    progress = {

                        progress /
                            100f
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                )
            }

            /*
             * ------------------------------------------------
             * WebView
             * ------------------------------------------------
             */
            AndroidView(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),

                factory = {
                    ctx ->

                    WebView(ctx).apply {

                        webView =
                            this

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

                        CookieManager
                            .getInstance()
                            .setAcceptCookie(
                                true
                            )

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

                                    view?.post {

                                        view.requestLayout()

                                        view.invalidate()

                                        view.evaluateJavascript(
                                            SHORT_PAGE_FIX_SCRIPT,
                                            null
                                        )
                                    }
                                }
                            }

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
}

/*
 * ----------------------------------------------------
 * 下载
 * ----------------------------------------------------
 */
private fun downloadFile(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?
) {

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

        val request =
            DownloadManager.Request(
                Uri.parse(
                    url
                )
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

                setDestinationInExternalPublicDir(
                    Environment
                        .DIRECTORY_DOWNLOADS,

                    "兔跃/$fileName"
                )

                userAgent?.let {

                    addRequestHeader(
                        "User-Agent",
                        it
                    )
                }

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

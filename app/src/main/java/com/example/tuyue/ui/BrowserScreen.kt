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
     * ----------------------------------------------------
     * 全屏切换后的 WebView 刷新 + 尺寸诊断
     * ----------------------------------------------------
     *
     * 现在我们不再猜底部空白来自哪里。
     *
     * 这里会同时取得：
     *
     * Android 层：
     * Root 宽高
     * WebView 宽高
     * WebView 在屏幕中的 Y
     * WebView bottom
     *
     * JavaScript 层：
     * window.innerHeight
     * documentElement.clientHeight
     * body 高度
     * scrollHeight
     *
     * 这样就能判断：
     *
     * 1. Compose 是否少给了 WebView 高度
     * 2. WebView 是否已经真正铺满
     * 3. HTML viewport 是否少了一截
     * 4. 是否只是网页自己的 body 没铺满
     */
    LaunchedEffect(
        displayMode,
        webView
    ) {

        val view =
            webView ?: return@LaunchedEffect

        /*
         * 第一次要求 Android 重新布局。
         */
        view.post {

            view.requestLayout()
            view.invalidate()
        }

        /*
         * 等待 Compose / Window Insets
         * 完成第一轮尺寸变化。
         */
        delay(100)

        view.post {

            view.requestLayout()
            view.invalidate()

            /*
             * 通知网页 viewport 已发生变化。
             */
            view.evaluateJavascript(
                """
                (function() {
                    try {
                        window.dispatchEvent(
                            new Event('resize')
                        );
                    } catch (e) {
                    }
                })();
                """.trimIndent(),
                null
            )
        }

        /*
         * 某些手机隐藏系统栏有动画，
         * 再等一轮。
         */
        delay(250)

        view.post {

            view.requestLayout()
            view.invalidate()

            view.evaluateJavascript(
                """
                (function() {
                    try {
                        window.dispatchEvent(
                            new Event('resize')
                        );
                    } catch (e) {
                    }
                })();
                """.trimIndent(),
                null
            )
        }

        /*
         * NORMAL 模式不用弹诊断。
         *
         * 只有：
         * FULLSCREEN
         * IMMERSIVE
         *
         * 才测量。
         */
        if (
            displayMode !=
            BrowserDisplayMode.NORMAL
        ) {

            /*
             * 等系统栏动画和 Compose
             * 最终稳定下来。
             */
            delay(500)

            view.post {

                /*
                 * WebView 左上角在真实屏幕中的位置。
                 */
                val location =
                    IntArray(2)

                view.getLocationOnScreen(
                    location
                )

                /*
                 * Activity 根 View。
                 */
                val rootView =
                    view.rootView

                /*
                 * 同时读取网页自己的 viewport。
                 */
                view.evaluateJavascript(
                    """
                    (function() {
                        try {
                            var body = document.body;
                            var html = document.documentElement;

                            return JSON.stringify({
                                innerWidth:
                                    window.innerWidth,

                                innerHeight:
                                    window.innerHeight,

                                clientWidth:
                                    html
                                        ? html.clientWidth
                                        : 0,

                                clientHeight:
                                    html
                                        ? html.clientHeight
                                        : 0,

                                bodyHeight:
                                    body
                                        ? Math.round(
                                            body.getBoundingClientRect().height
                                          )
                                        : 0,

                                bodyScrollHeight:
                                    body
                                        ? body.scrollHeight
                                        : 0,

                                htmlScrollHeight:
                                    html
                                        ? html.scrollHeight
                                        : 0,

                                devicePixelRatio:
                                    window.devicePixelRatio
                            });
                        } catch (e) {

                            return JSON.stringify({
                                error:
                                    String(e)
                            });
                        }
                    })();
                    """.trimIndent()
                ) { result ->

                    /*
                     * 这里显示的就是我们真正需要的
                     * 诊断结果。
                     */
                    val message =
                        """
                        MODE: $displayMode
                        Root: ${rootView.width} x ${rootView.height}
                        WebView: ${view.width} x ${view.height}
                        Y: ${location[1]}
                        Bottom: ${location[1] + view.height}
                        JS: $result
                        """.trimIndent()

                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /*
     * ----------------------------------------------------
     * 返回键
     * ----------------------------------------------------
     *
     * 1. 菜单打开 -> 关闭菜单
     * 2. 正在全屏 -> 退出全屏
     * 3. WebView 可以后退 -> 网页后退
     * 4. 否则 -> 回首页
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

            webView?.canGoBack() == true -> {

                webView?.goBack()
            }

            else -> {

                onHome()
            }
        }
    }

    /*
     * ----------------------------------------------------
     * 浏览器主体
     * ----------------------------------------------------
     */
    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {

        /*
         * 普通模式显示浏览器顶部栏。
         *
         * 全屏 / 沉浸式全屏时，
         * 顶部栏完全退出布局。
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
                     * 当前网页标题
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
                             * 全屏：
                             *
                             * 保留顶部系统状态栏，
                             * 隐藏底部系统导航栏。
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
                             * 沉浸式全屏：
                             *
                             * 状态栏、导航栏全部隐藏。
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
                             * 手机版 / 桌面版切换
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
         * 网页加载进度。
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

                    /*
                     * 保存 WebView 引用。
                     */
                    webView =
                        this

                    /*
                     * WebView 基础设置。
                     */
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
                     * 页面导航
                     */
                    webViewClient =
                        object :
                            WebViewClient() {

                            override fun onPageFinished(
                                view: WebView?,
                                url: String?
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

                                /*
                                 * 页面加载完成以后，
                                 * 主动触发一次 viewport resize。
                                 */
                                view?.post {

                                    view.requestLayout()

                                    view.invalidate()

                                    view.evaluateJavascript(
                                        """
                                        (function() {
                                            try {
                                                window.dispatchEvent(
                                                    new Event('resize')
                                                );
                                            } catch (e) {
                                            }
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                }
                            }
                        }

                    /*
                     * 标题与加载进度。
                     */
                    webChromeClient =
                        object :
                            WebChromeClient() {

                            override fun onReceivedTitle(
                                view: WebView?,
                                title: String?
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
                                view: WebView?,
                                newProgress: Int
                            ) {

                                progress =
                                    newProgress
                            }
                        }

                    /*
                     * 下载监听。
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
                     * 打开初始网址。
                     */
                    loadUrl(
                        initialUrl
                    )
                }
            },

            /*
             * BrowserScreen 真正销毁以后，
             * 释放 WebView。
             */
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

    /*
     * 当前只支持 HTTP / HTTPS。
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
         * 自动生成文件名。
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
         * 系统 DownloadManager。
         */
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

                /*
                 * 保存到：
                 *
                 * Download/兔跃/
                 */
                setDestinationInExternalPublicDir(
                    Environment
                        .DIRECTORY_DOWNLOADS,

                    "兔跃/$fileName"
                )

                /*
                 * 保留 User-Agent。
                 */
                userAgent?.let {

                    addRequestHeader(
                        "User-Agent",
                        it
                    )
                }

                /*
                 * 保留当前网页 Cookie。
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
         * 开始下载。
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

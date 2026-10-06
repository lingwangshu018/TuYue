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
     * 全屏模式发生变化以后，
     * WebView 的实际可用高度也会变化。
     *
     * Compose 会重新布局 AndroidView，
     * 但网页内部不一定马上重新计算 viewport。
     *
     * 因此这里主动：
     *
     * 1. requestLayout()
     * 2. invalidate()
     * 3. 等布局完成
     * 4. 向网页发送 resize 事件
     */
    LaunchedEffect(
        displayMode,
        webView
    ) {

        val view =
            webView ?: return@LaunchedEffect

        /*
         * 第一轮：
         * 让 Android View 重新测量。
         */
        view.post {

            view.requestLayout()

            view.invalidate()
        }

        /*
         * 等 Compose / Window Insets
         * 完成第一轮布局。
         */
        delay(100)

        view.post {

            view.requestLayout()

            view.invalidate()

            /*
             * 通知网页：
             * viewport 尺寸发生变化。
             *
             * 对使用：
             *
             * 100vh
             * 100dvh
             * window.innerHeight
             *
             * 的网页尤其重要。
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
         * 某些 Android ROM 的系统栏动画
         * 会比 Compose 布局慢一点。
         *
         * 再做一次最终同步。
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
    }

    /*
     * 返回键处理。
     *
     * 优先级：
     *
     * 1. 关闭菜单
     * 2. 退出全屏
     * 3. 网页后退
     * 4. 返回首页
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
     * 整个 BrowserScreen。
     *
     * 普通模式：
     *
     * ┌───────────────┐
     * │ 浏览器工具栏    │
     * ├───────────────┤
     * │               │
     * │    WebView    │
     * │               │
     * └───────────────┘
     *
     * 全屏：
     *
     * ┌───────────────┐
     * │               │
     * │               │
     * │    WebView    │
     * │               │
     * │               │
     * └───────────────┘
     */
    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {

        /*
         * -------------------------
         * 浏览器顶部工具栏
         * -------------------------
         *
         * 只有 NORMAL 模式显示。
         *
         * FULLSCREEN /
         * IMMERSIVE 时整个 Composable
         * 从布局中删除。
         */
        if (!isFullscreen) {

            Surface(
                tonalElevation = 2.dp
            ) {

                Row(
                    modifier =
                        Modifier
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

                        modifier =
                            Modifier
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
                             * 沉浸式全屏
                             *
                             * 顶部状态栏和
                             * 底部导航栏全部隐藏。
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
         * 加载进度条
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
         * -------------------------
         * WebView
         * -------------------------
         *
         * weight(1f) 会始终获得 Column
         * 剩余的全部高度。
         *
         * 当顶部工具栏消失后，
         * WebView 会重新测量并扩大。
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
                     * WebView 设置。
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
                     * 页面导航。
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
                                 * 页面第一次加载完成以后，
                                 * 再主动通知一次网页尺寸。
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
                     * 标题和加载进度。
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
                     * 打开初始网页。
                     */
                    loadUrl(
                        initialUrl
                    )
                }
            },

            /*
             * AndroidView 从 Composition
             * 中真正移除时销毁 WebView。
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
 * -----------------------------
 * 下载
 * -----------------------------
 */
private fun downloadFile(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?
) {

    /*
     * 当前只支持标准 HTTP / HTTPS 下载。
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
         * 自动识别文件名。
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
         * 创建系统 DownloadManager
         * 下载任务。
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
                 * 保留网页 User-Agent。
                 */
                userAgent?.let {

                    addRequestHeader(
                        "User-Agent",
                        it
                    )
                }

                /*
                 * 保留 Cookie。
                 *
                 * 登录后的文件下载
                 * 通常需要 Cookie。
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
         * 交给系统下载器。
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

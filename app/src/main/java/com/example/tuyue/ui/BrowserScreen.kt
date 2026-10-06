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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.tuyue.BrowserDisplayMode
import kotlinx.coroutines.delay
import org.json.JSONObject

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

    /*
     * 调试信息。
     *
     * 这个文字会覆盖在 WebView 上方，
     * 不参与页面高度计算。
     */
    var debugInfo by remember {
        mutableStateOf("等待测量...")
    }

    val isFullscreen =
        displayMode != BrowserDisplayMode.NORMAL

    /*
     * ----------------------------------------------------
     * 全屏切换后的 WebView 刷新 + 精确尺寸诊断
     * ----------------------------------------------------
     */
    LaunchedEffect(
        displayMode,
        webView
    ) {

        val view =
            webView ?: return@LaunchedEffect

        /*
         * 普通模式清掉调试信息。
         */
        if (
            displayMode ==
            BrowserDisplayMode.NORMAL
        ) {

            debugInfo =
                "等待进入全屏..."

            return@LaunchedEffect
        }

        /*
         * 第一次重新测量。
         */
        view.post {

            view.requestLayout()
            view.invalidate()
        }

        delay(100)

        /*
         * 通知网页 viewport 改变。
         */
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
         * 等待系统栏动画。
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
         * 再等待布局完全稳定。
         */
        delay(500)

        view.post {

            /*
             * WebView 在屏幕上的真实坐标。
             */
            val location =
                IntArray(2)

            view.getLocationOnScreen(
                location
            )

            val rootView =
                view.rootView

            /*
             * Android 层的数据先保存下来。
             */
            val rootWidth =
                rootView.width

            val rootHeight =
                rootView.height

            val webWidth =
                view.width

            val webHeight =
                view.height

            val webY =
                location[1]

            val webBottom =
                webY + webHeight

            /*
             * 再读取网页内部 viewport。
             */
            view.evaluateJavascript(
                """
                (function() {
                    try {
                        var body =
                            document.body;

                        var html =
                            document.documentElement;

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
                                        body
                                            .getBoundingClientRect()
                                            .height
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
            ) { rawResult ->

                /*
                 * evaluateJavascript 返回的是
                 * 被 JSON 转义过的字符串。
                 *
                 * 先解包，再解析。
                 */
                try {

                    val decoded =
                        if (
                            rawResult.startsWith("\"")
                        ) {

                            JSONObject(
                                """{"value":$rawResult}"""
                            ).getString(
                                "value"
                            )

                        } else {

                            rawResult
                        }

                    val json =
                        JSONObject(
                            decoded
                        )

                    val innerWidth =
                        json.optInt(
                            "innerWidth"
                        )

                    val innerHeight =
                        json.optInt(
                            "innerHeight"
                        )

                    val clientWidth =
                        json.optInt(
                            "clientWidth"
                        )

                    val clientHeight =
                        json.optInt(
                            "clientHeight"
                        )

                    val bodyHeight =
                        json.optInt(
                            "bodyHeight"
                        )

                    val bodyScrollHeight =
                        json.optInt(
                            "bodyScrollHeight"
                        )

                    val htmlScrollHeight =
                        json.optInt(
                            "htmlScrollHeight"
                        )

                    val dpr =
                        json.optDouble(
                            "devicePixelRatio"
                        )

                    debugInfo =
                        """
                        MODE: $displayMode

                        Root:
                        $rootWidth × $rootHeight

                        WebView:
                        $webWidth × $webHeight

                        WebView Y:
                        $webY

                        WebView Bottom:
                        $webBottom

                        inner:
                        $innerWidth × $innerHeight

                        client:
                        $clientWidth × $clientHeight

                        bodyHeight:
                        $bodyHeight

                        bodyScroll:
                        $bodyScrollHeight

                        htmlScroll:
                        $htmlScrollHeight

                        DPR:
                        $dpr
                        """.trimIndent()

                } catch (
                    e: Exception
                ) {

                    /*
                     * 即使 JS JSON 解析失败，
                     * Android 层的关键尺寸
                     * 仍然显示出来。
                     */
                    debugInfo =
                        """
                        MODE: $displayMode

                        Root:
                        $rootWidth × $rootHeight

                        WebView:
                        $webWidth × $webHeight

                        WebView Y:
                        $webY

                        WebView Bottom:
                        $webBottom

                        JS parse error:
                        ${e.message}

                        Raw:
                        $rawResult
                        """.trimIndent()
                }
            }
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
     * ----------------------------------------------------
     * 最外层改成 Box
     * ----------------------------------------------------
     *
     * 浏览器原来的 Column 仍然 fillMaxSize。
     *
     * 调试面板只是覆盖在它上面，
     * 所以绝对不会吃掉 WebView 的高度。
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

        /*
         * ------------------------------------------------
         * 悬浮诊断面板
         * ------------------------------------------------
         *
         * 重点：
         *
         * 它属于外层 Box 的 overlay，
         * 不会占据 Column / WebView 的任何高度。
         */
        if (isFullscreen) {

            Box(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopStart
                        )
                        .padding(
                            12.dp
                        )
                        .zIndex(
                            100f
                        )
                        .background(
                            color =
                                Color.Black.copy(
                                    alpha =
                                        0.78f
                                ),

                            shape =
                                RoundedCornerShape(
                                    12.dp
                                )
                        )
                        .padding(
                            horizontal =
                                12.dp,

                            vertical =
                                10.dp
                        )
            ) {

                Text(
                    text =
                        debugInfo,

                    color =
                        Color.White,

                    fontSize =
                        11.sp,

                    lineHeight =
                        14.sp
                )
            }
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

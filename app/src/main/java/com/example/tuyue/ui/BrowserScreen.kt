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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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

        if (!document.getElementById('tuyue-viewport-fix')) {
            const style = document.createElement('style');
            style.id = 'tuyue-viewport-fix';
            style.textContent = `
                html {
                    min-height: 100% !important;
                    height: 100% !important;
                }
                body {
                    min-height: 100% !important;
                }
            `;
            (document.head || html).appendChild(style);
        }

        window.dispatchEvent(new Event('resize'));
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
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val findFocusRequester = remember { FocusRequester() }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var desktopMode by remember { mutableStateOf(initialDesktopMode) }
    var pageTitle by remember { mutableStateOf("网页") }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var canGoForward by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }

    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findActiveIndex by remember { mutableIntStateOf(0) }
    var findMatchCount by remember { mutableIntStateOf(0) }
    var pageLoadGeneration by remember { mutableIntStateOf(0) }

    val isFullscreen = displayMode != BrowserDisplayMode.NORMAL

    fun closeFind() {
        showFindBar = false
        findQuery = ""
        findActiveIndex = 0
        findMatchCount = 0
        keyboardController?.hide()
        webView?.clearMatches()
    }

    LaunchedEffect(showFindBar) {
        if (showFindBar) {
            delay(80)
            findFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(
        showFindBar,
        findQuery,
        pageLoadGeneration,
        webView
    ) {
        val view = webView ?: return@LaunchedEffect

        if (!showFindBar || findQuery.isBlank()) {
            view.clearMatches()
            findActiveIndex = 0
            findMatchCount = 0
            return@LaunchedEffect
        }

        view.findAllAsync(findQuery)
    }

    LaunchedEffect(displayMode, webView) {
        val view = webView ?: return@LaunchedEffect

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

    BackHandler {
        when {
            menuExpanded -> {
                menuExpanded = false
            }

            showFindBar -> {
                closeFind()
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
        modifier = Modifier.fillMaxSize()
    ) {
        if (showFindBar) {
            FindInPageBar(
                query = findQuery,
                onQueryChange = {
                    findQuery = it
                },
                activeIndex = findActiveIndex,
                matchCount = findMatchCount,
                focusRequester = findFocusRequester,
                onPrevious = {
                    webView?.findNext(false)
                },
                onNext = {
                    webView?.findNext(true)
                },
                onClose = {
                    closeFind()
                }
            )

        } else if (!isFullscreen) {
            BrowserTopBar(
                pageTitle = pageTitle,
                canGoForward = canGoForward,
                desktopMode = desktopMode,
                menuExpanded = menuExpanded,

                onMenuExpandedChange = {
                    menuExpanded = it
                },

                onBack = {
                    val view = webView

                    if (view?.canGoBack() == true) {
                        view.goBack()
                    } else {
                        onHome()
                    }
                },

                onHome = onHome,

                onForward = {
                    webView?.goForward()
                },

                onReload = {
                    webView?.reload()
                },

                onFind = {
                    showFindBar = true
                },

                onFullscreen = {
                    onDisplayModeChanged(
                        BrowserDisplayMode.FULLSCREEN
                    )
                },

                onImmersive = {
                    onDisplayModeChanged(
                        BrowserDisplayMode.IMMERSIVE
                    )
                },

                onSaveToHome = {
                    val url =
                        webView?.url
                            ?: currentUrl

                    val name =
                        pageTitle.ifBlank {
                            "网页"
                        }

                    if (
                        url.startsWith("http://") ||
                        url.startsWith("https://")
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
                },

                onAddWebPage = onAddWebPage,

                onToggleDesktop = {
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
                },

                onSettings = onSettings
            )
        }

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

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),

            factory = { ctx ->

                WebView(ctx).apply {

                    webView =
                        this

                    setFindListener {
                            activeMatchOrdinal,
                            numberOfMatches,
                            isDoneCounting ->

                        if (
                            isDoneCounting
                        ) {
                            findActiveIndex =
                                if (
                                    numberOfMatches > 0
                                ) {
                                    activeMatchOrdinal
                                        .coerceAtLeast(0)
                                } else {
                                    0
                                }

                            findMatchCount =
                                numberOfMatches
                                    .coerceAtLeast(0)
                        }
                    }

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
                                view: WebView?,
                                url: String?
                            ) {

                                currentUrl =
                                    url
                                        ?: currentUrl

                                pageLoadGeneration++

                                canGoForward =
                                    view?.canGoForward()
                                        == true

                                view?.title
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let {
                                        pageTitle = it
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
                                view: WebView?,
                                title: String?
                            ) {

                                if (
                                    !title.isNullOrBlank()
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
                            mimeType =
                                mimeType
                        )
                    }

                    loadUrl(
                        initialUrl
                    )
                }
            },

            onRelease = { view ->

                view.clearMatches()

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

@Composable
private fun FindInPageBar(
    query: String,
    onQueryChange: (String) -> Unit,
    activeIndex: Int,
    matchCount: Int,
    focusRequester: FocusRequester,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {

    Surface(
        tonalElevation = 2.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(
                    horizontal = 8.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.spacedBy(
                    4.dp
                )
        ) {

            OutlinedTextField(
                value =
                    query,

                onValueChange =
                    onQueryChange,

                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(
                            focusRequester
                        ),

                placeholder = {
                    Text(
                        "在网页中查找"
                    )
                },

                singleLine =
                    true
            )

            Text(
                text =
                    if (
                        matchCount > 0
                    ) {
                        "${activeIndex + 1}/$matchCount"
                    } else {
                        "0/0"
                    },

                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            TextButton(
                onClick =
                    onPrevious,

                enabled =
                    matchCount > 0
            ) {

                Text(
                    "↑"
                )
            }

            TextButton(
                onClick =
                    onNext,

                enabled =
                    matchCount > 0
            ) {

                Text(
                    "↓"
                )
            }

            TextButton(
                onClick =
                    onClose
            ) {

                Text(
                    "×"
                )
            }
        }
    }
}

@Composable
private fun BrowserTopBar(
    pageTitle: String,
    canGoForward: Boolean,
    desktopMode: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onFind: () -> Unit,
    onFullscreen: () -> Unit,
    onImmersive: () -> Unit,
    onSaveToHome: () -> Unit,
    onAddWebPage: () -> Unit,
    onToggleDesktop: () -> Unit,
    onSettings: () -> Unit
) {

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

            TextButton(
                onClick =
                    onBack
            ) {

                Text(
                    text =
                        "‹",

                    fontSize =
                        30.sp
                )
            }

            Text(
                text =
                    pageTitle,

                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            horizontal = 4.dp
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

            Box {

                IconButton(
                    onClick = {
                        onMenuExpandedChange(
                            true
                        )
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
                        onMenuExpandedChange(
                            false
                        )
                    }
                ) {

                    BrowserMenuItem(
                        text =
                            "回到主页",

                        closeMenu =
                            onMenuExpandedChange,

                        onClick =
                            onHome
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

                            onMenuExpandedChange(
                                false
                            )

                            onForward()
                        }
                    )

                    BrowserMenuItem(
                        text =
                            "刷新",

                        closeMenu =
                            onMenuExpandedChange,

                        onClick =
                            onReload
                    )

                    BrowserMenuItem(
                        text =
                            "在网页中查找",

                        closeMenu =
                            onMenuExpandedChange,

                        onClick =
                            onFind
                    )

                    HorizontalDivider()

                    BrowserMenuItem(
                        text =
                            "全屏",

                        closeMenu =
                            onMenuExpandedChange,

                        onClick =
                            onFullscreen
                    )

                    BrowserMenuItem(
                        text =
                            "沉浸式全屏",

                        closeMenu =
                            onMenuExpandedChange,

                        onClick =
                            onImmersive
                    )

                    HorizontalDivider()

                    BrowserMenuItem(
                        text =
                            "保存到首页",

                        closeMenu =
                            onMenuExpandedChange,

                        onClick =
                            onSaveToHome
                    )

                    BrowserMenuItem(
                        text =
                            "添加网页",

                        closeMenu =
                            onMenuExpandedChange,

                        onClick =
                            onAddWebPage
                    )

                    HorizontalDivider()

                    BrowserMenuItem(
                        text =
                            if (
                                desktopMode
                            ) {
                                "切换手机版网页"
                            } else {
                                "切换桌面版网页"
                            },

                        closeMenu =
                            onMenuExpandedChange,

                        onClick =
                            onToggleDesktop
                    )

                    BrowserMenuItem(
                        text =
                            "浏览器设置",

                        closeMenu =
                            onMenuExpandedChange,

                        onClick =
                            onSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserMenuItem(
    text: String,
    closeMenu: (Boolean) -> Unit,
    onClick: () -> Unit
) {

    DropdownMenuItem(
        text = {
            Text(
                text
            )
        },

        onClick = {

            closeMenu(
                false
            )

            onClick()
        }
    )
}

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

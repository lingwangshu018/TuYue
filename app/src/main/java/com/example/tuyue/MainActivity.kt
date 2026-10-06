package com.example.tuyue

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.tuyue.ui.SplashScreen
import kotlinx.coroutines.delay

enum class BrowserDisplayMode {
    NORMAL,
    FULLSCREEN,
    IMMERSIVE
}

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        /*
         * 整个 App 使用 edge-to-edge。
         *
         * 系统状态栏 / 导航栏不再负责
         * 给我们的 Compose 内容预留空间。
         *
         * 哪些页面需要安全区域，
         * 由 Compose 自己决定。
         */
        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        /*
         * 系统栏背景透明。
         *
         * 尤其是底部导航区域，
         * 防止隐藏导航栏以后仍然露出
         * Window 默认的灰色背景。
         */
        window.statusBarColor =
            Color.TRANSPARENT

        window.navigationBarColor =
            Color.TRANSPARENT

        /*
         * Android 10+：
         * 禁止系统自动给导航栏增加
         * 对比度背景层。
         *
         * 否则某些手机即使导航栏透明，
         * 底部仍可能出现半透明灰色区域。
         */
        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q
        ) {
            window.isNavigationBarContrastEnforced =
                false
        }

        setContent {

            MaterialTheme {

                var showSplash by remember {
                    mutableStateOf(true)
                }

                var browserDisplayMode by remember {
                    mutableStateOf(
                        BrowserDisplayMode.NORMAL
                    )
                }

                LaunchedEffect(Unit) {

                    delay(1500)

                    showSplash = false
                }

                /*
                 * 浏览器显示模式变化以后，
                 * 同步修改真正的 Android 系统栏。
                 */
                LaunchedEffect(
                    browserDisplayMode
                ) {

                    applyBrowserDisplayMode(
                        browserDisplayMode
                    )
                }

                if (showSplash) {

                    SplashScreen()

                } else {

                    Surface(
                        modifier =
                            Modifier.fillMaxSize(),

                        /*
                         * 给整个 Window 一个确定的背景，
                         * 避免 edge-to-edge 区域露出
                         * Activity 默认 Window 背景。
                         */
                        color =
                            MaterialTheme
                                .colorScheme
                                .background
                    ) {

                        TuYueApp(
                            browserDisplayMode =
                                browserDisplayMode,

                            onBrowserDisplayModeChanged = {
                                browserDisplayMode = it
                            }
                        )
                    }
                }
            }
        }
    }

    private fun applyBrowserDisplayMode(
        mode: BrowserDisplayMode
    ) {

        val controller =
            WindowCompat.getInsetsController(
                window,
                window.decorView
            )

        /*
         * 系统栏隐藏以后，
         * 用户仍然可以从屏幕边缘滑动，
         * 临时把系统栏叫出来。
         *
         * 临时系统栏覆盖在网页上，
         * 不应该重新挤压 WebView。
         */
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        when (mode) {

            BrowserDisplayMode.NORMAL -> {

                /*
                 * 普通模式：
                 *
                 * 顶部状态栏显示
                 * 底部导航栏显示
                 */
                controller.show(
                    WindowInsetsCompat.Type.systemBars()
                )
            }

            BrowserDisplayMode.FULLSCREEN -> {

                /*
                 * 普通全屏：
                 *
                 * 顶部状态栏保留
                 * 底部导航栏隐藏
                 */
                controller.show(
                    WindowInsetsCompat.Type.statusBars()
                )

                controller.hide(
                    WindowInsetsCompat.Type.navigationBars()
                )
            }

            BrowserDisplayMode.IMMERSIVE -> {

                /*
                 * 沉浸式全屏：
                 *
                 * 顶部状态栏隐藏
                 * 底部导航栏隐藏
                 */
                controller.hide(
                    WindowInsetsCompat.Type.systemBars()
                )
            }
        }
    }

    override fun onDestroy() {

        /*
         * Activity 销毁以前恢复系统栏。
         */
        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).show(
            WindowInsetsCompat.Type.systemBars()
        )

        super.onDestroy()
    }
}

package com.example.tuyue

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

        // 让 Compose 自己处理系统栏区域。
        // 这样进入沉浸式全屏后，网页才能真正铺满屏幕。
        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

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

                LaunchedEffect(browserDisplayMode) {
                    applyBrowserDisplayMode(
                        browserDisplayMode
                    )
                }

                if (showSplash) {

                    SplashScreen()

                } else {

                    Surface(
                        modifier = Modifier.fillMaxSize()
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

        // 从屏幕边缘滑动时，
        // 可以临时唤出被隐藏的系统栏。
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        when (mode) {

            BrowserDisplayMode.NORMAL -> {

                // 普通模式：
                // 状态栏 + 导航栏全部显示
                controller.show(
                    WindowInsetsCompat.Type.systemBars()
                )
            }

            BrowserDisplayMode.FULLSCREEN -> {

                // 全屏：
                // 保留顶部时间/状态栏
                controller.show(
                    WindowInsetsCompat.Type.statusBars()
                )

                // 隐藏底部导航栏
                controller.hide(
                    WindowInsetsCompat.Type.navigationBars()
                )
            }

            BrowserDisplayMode.IMMERSIVE -> {

                // 沉浸式全屏：
                // 状态栏 + 导航栏全部隐藏
                controller.hide(
                    WindowInsetsCompat.Type.systemBars()
                )
            }
        }
    }

    override fun onDestroy() {

        // Activity 销毁时恢复系统栏，
        // 避免系统栏状态残留。
        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).show(
            WindowInsetsCompat.Type.systemBars()
        )

        super.onDestroy()
    }
}

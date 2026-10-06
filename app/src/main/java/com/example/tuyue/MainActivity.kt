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
import com.example.tuyue.ui.SplashScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {

            MaterialTheme {

                var showSplash by remember {
                    mutableStateOf(true)
                }

                LaunchedEffect(Unit) {

                    delay(1500)

                    showSplash = false
                }

                if (showSplash) {

                    SplashScreen()

                } else {

                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        TuYueApp()
                    }
                }
            }
        }
    }
}

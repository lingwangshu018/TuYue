package com.example.tuyue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {

            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {

                    TuYueApp()
                }
            }
        }
    }
}

@Composable
fun TuYueApp() {

    Column(
        modifier = Modifier.fillMaxSize(),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "兔跃",
            style =
                MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "网页，从这里出发"
        )
    }
}

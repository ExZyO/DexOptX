package com.antigravity.dexoptx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.antigravity.dexoptx.theme.DarkMidnight
import com.antigravity.dexoptx.theme.DexOptXTheme
import com.antigravity.dexoptx.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DexOptXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkMidnight
                ) {
                    MainScreen()
                }
            }
        }
    }
}

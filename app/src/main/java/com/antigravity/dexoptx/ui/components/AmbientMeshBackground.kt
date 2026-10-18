package com.antigravity.dexoptx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.antigravity.dexoptx.theme.DarkMidnight

@Composable
fun AmbientMeshBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Base dark background
        drawRect(color = DarkMidnight)

        // Top-left ambient indigo glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x334F46E5), Color(0x104F46E5), Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.15f),
                radius = size.width * 0.65f
            ),
            center = Offset(size.width * 0.15f, size.height * 0.15f),
            radius = size.width * 0.65f
        )

        // Bottom-right ambient fuchsia glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x2BDB2777), Color(0x0CDB2777), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.85f),
                radius = size.width * 0.65f
            ),
            center = Offset(size.width * 0.85f, size.height * 0.85f),
            radius = size.width * 0.65f
        )
    }
}

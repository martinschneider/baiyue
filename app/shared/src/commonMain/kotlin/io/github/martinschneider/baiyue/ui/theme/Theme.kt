package io.github.martinschneider.baiyue.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors from the web app
val BaiyuePrimary = Color(0xFF0066A2)
val XiaobaiyuePrimary = Color(0xFF728224)
val AppAccent = Color(0xFF3298DC)

// Marker colors
val BaiyueUnvisited = Color(0xFF003B6F)   // darkblue
val BaiyueVisited = Color(0xFF2A81CB)      // blue
val XiaobaiyueUnvisited = Color(0xFF436436) // darkgreen
val XiaobaiyueVisited = Color(0xFF728224)   // green

val BaiyueColorScheme = lightColorScheme(
    primary = AppAccent,
    secondary = BaiyuePrimary,
    tertiary = XiaobaiyuePrimary,
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFFF5F5F5),
)

@Composable
fun BaiyueTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BaiyueColorScheme,
        content = content
    )
}

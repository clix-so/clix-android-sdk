package so.clix.samples.basic

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTheme {
    val background = Color(0xFF000000)
    val surface = Color(0xFF1C1C1E)
    val surfaceVariant = Color(0xFF3A3A3D)
    val primary = Color(0xFFB0C4DE)
    val buttonBackground = Color(0xFFEBEBF5).copy(alpha = 0.9f)
    val buttonText = Color.Black
    val text = Color(0xFFE0E0E0)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AppTheme.primary,
            onPrimary = Color.Black,
            primaryContainer = Color(0xFFDCE4F2),
            onPrimaryContainer = Color(0xFF001E3A),
            secondary = Color(0xFF535F70),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD7E3F8),
            onSecondaryContainer = Color(0xFF101C2B),
            tertiary = Color(0xFF6B5778),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFF2DAFF),
            onTertiaryContainer = Color(0xFF251431),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = AppTheme.background,
            onBackground = AppTheme.text,
            surface = AppTheme.surface,
            onSurface = AppTheme.text,
            surfaceVariant = AppTheme.surfaceVariant,
            onSurfaceVariant = Color(0xFFC1C1C1),
            outline = Color(0xFF8B8B8F),
        ),
        typography = Typography(
            bodyLarge = TextStyle(
                color = AppTheme.text,
                fontSize = 16.sp,
            ),
            labelLarge = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}

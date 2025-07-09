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

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            darkColorScheme(
                primary = Color(0xFFB0C4DE), // Light Steel Blue - More M3 like primary
                onPrimary = Color.Black,
                primaryContainer = Color(0xFFDCE4F2),
                onPrimaryContainer = Color(0xFF001E3A),
                secondary = Color(0xFF535F70), // Slate Gray
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFD7E3F8),
                onSecondaryContainer = Color(0xFF101C2B),
                tertiary = Color(0xFF6B5778), // Lavender Gray
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFF2DAFF),
                onTertiaryContainer = Color(0xFF251431),
                error = Color(0xFFBA1A1A),
                onError = Color.White,
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002),
                background = Color(0xFF000000), // Original Black background
                onBackground = Color(0xFFE0E0E0), // Lighter text on black
                surface =
                    Color(0xFF1C1C1E), // Darker panel, as in screenshot context (TextField bg)
                onSurface = Color(0xFFE0E0E0), // Text on panel
                surfaceVariant = Color(0xFF3A3A3D), // For darker elements on surface if needed
                onSurfaceVariant = Color(0xFFC1C1C1),
                outline = Color(0xFF8B8B8F),
            ),
        typography =
            Typography(
                bodyLarge =
                    TextStyle(
                        color = Color(0xFFE0E0E0), // Consistent light text color
                        fontSize = 16.sp,
                    ),
                labelLarge =
                    TextStyle( // Used for Button text
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
            ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

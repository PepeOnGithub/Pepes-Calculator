package xyz.pepe.glacierclient.calculatorapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Green brand accent, matching Pepe's Notes / Clock / Weather.
private val SeedGreen = androidx.compose.ui.graphics.Color(0xFF2E7D32)

private val LightColors = lightColorScheme(
    primary = SeedGreen,
    secondary = androidx.compose.ui.graphics.Color(0xFF52634F),
    tertiary = androidx.compose.ui.graphics.Color(0xFF39656C)
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF97D791),
    secondary = androidx.compose.ui.graphics.Color(0xFFBACCB4),
    tertiary = androidx.compose.ui.graphics.Color(0xFFA1CED6)
)

@Composable
fun PepesCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.calculatorapp.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Green brand accent, matching Pepe's Notes / Clock / Weather — a fixed dark palette by default,
// same as those apps, rather than following the system light/dark + dynamic-color default.
private val BrandGreen = Color(0xFF97D791)
private val OnBrandGreen = Color(0xFF0B3D0F)
private val PrimaryContainer = Color(0xFF2E7D32)
private val OnPrimaryContainer = Color(0xFFC8E6C9)
private val Secondary = Color(0xFFBACCB4)
private val Tertiary = Color(0xFFA1CED6)
private val Surface = Color(0xFF10140F)
private val SurfaceContainerLow = Color(0xFF171C15)
private val SurfaceContainer = Color(0xFF1D231A)
private val SurfaceContainerHigh = Color(0xFF272E23)
private val SurfaceContainerHighest = Color(0xFF32392D)
private val OnSurface = Color(0xFFE2E6DC)
private val OnSurfaceVariant = Color(0xFFC1C9B8)
private val OutlineVariant = Color(0xFF424940)

private val BrandDarkColors = darkColorScheme(
    primary = BrandGreen,
    onPrimary = OnBrandGreen,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnBrandGreen,
    tertiary = Tertiary,
    onTertiary = OnBrandGreen,
    background = Surface,
    surface = Surface,
    surfaceContainerLowest = Surface,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    onSurface = OnSurface,
    onBackground = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    outlineVariant = OutlineVariant
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/** [wallpaperColorsEnabled] mirrors Notes/Clock/Weather's "wallpaper colors" setting: off (the
 *  default) uses the fixed brand-green palette everywhere; on, pulls Material You colors from
 *  the wallpaper instead, on API 31+. */
@Composable
fun PepesCalculatorTheme(
    wallpaperColorsEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scheme = if (wallpaperColorsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        BrandDarkColors
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = AppShapes,
        content = content
    )
}

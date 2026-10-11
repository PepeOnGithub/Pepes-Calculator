package xyz.pepe.glacierclient.calculatorapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Same frosted-glass recipe as Pepe's Music / Weather, tinted for this app's black/green theme. */
private val GlassTop = Color(0x26FFFFFF)
private val GlassBottom = Color(0x14FFFFFF)
private val GlassBorderColor = Color(0x33FFFFFF)

val GlassBorder = BorderStroke(1.dp, GlassBorderColor)

val GlassCornerRadius = 24.dp

/** Driven by the "Frosted glass" setting; when false cards render as solid tonal surfaces. */
val LocalGlassEnabled = staticCompositionLocalOf { true }

/** Clips to [shape] and paints a translucent white gradient, simulating frosted glass over the theme. */
fun Modifier.glass(shape: Shape): Modifier = this
  .clip(shape)
  .background(Brush.verticalGradient(listOf(GlassTop, GlassBottom)))

/** Small uppercase icon + title row used above every card, as in Pepe's Weather. */
@Composable
fun SectionHeader(title: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
  Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    if (icon != null) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
      Spacer(Modifier.width(6.dp))
    }
    Text(
      title.uppercase(),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.8.sp,
      maxLines = 1,
    )
  }
}

/**
 * The family card: frosted glass (or a solid tonal surface when the glass setting is off) with an
 * optional uppercase icon header, 24 dp corners and 16 dp padding.
 */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(GlassCornerRadius),
  headerIcon: ImageVector? = null,
  headerTitle: String? = null,
  contentPadding: Dp = 16.dp,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val glass = LocalGlassEnabled.current
  val surfaceModifier = if (glass) modifier.glass(shape) else modifier
  val color = if (glass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh
  val border = if (glass) GlassBorder else null
  val inner: @Composable () -> Unit = {
    Column(Modifier.fillMaxWidth().padding(contentPadding)) {
      if (headerTitle != null) SectionHeader(headerTitle, headerIcon, Modifier.fillMaxWidth().padding(bottom = 12.dp))
      content()
    }
  }
  if (onClick != null) Surface(onClick = onClick, modifier = surfaceModifier, shape = shape, color = color, border = border, content = inner)
  else Surface(modifier = surfaceModifier, shape = shape, color = color, border = border, content = inner)
}

/** Tint for an inset panel inside a card (result block, display well). */
@Composable
fun insetColor(): Color = if (LocalGlassEnabled.current) Color.Black.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHighest

@Composable
fun glassFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedContainerColor = insetColor(),
  unfocusedContainerColor = insetColor(),
  unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

val CardSpacing: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(12.dp)

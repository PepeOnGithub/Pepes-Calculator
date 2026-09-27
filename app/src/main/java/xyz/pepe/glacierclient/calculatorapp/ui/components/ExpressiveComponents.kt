package xyz.pepe.glacierclient.calculatorapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shapes usable for [ShapeBadge], matching the set shared with Pepe's Notes / Clock / Weather. */
enum class BadgeShape { COOKIE, SUNNY, CLOVER, PILL, GEM, PUFFY, SOFT_BURST, CIRCLE }

@Composable
fun BadgeShape.toComposeShape(): Shape = when (this) {
    BadgeShape.COOKIE -> MaterialShapes.Cookie9Sided.toShape()
    BadgeShape.SUNNY -> MaterialShapes.Sunny.toShape()
    BadgeShape.CLOVER -> MaterialShapes.Clover4Leaf.toShape()
    BadgeShape.PILL -> MaterialShapes.Pill.toShape()
    BadgeShape.GEM -> MaterialShapes.Gem.toShape()
    BadgeShape.PUFFY -> MaterialShapes.Puffy.toShape()
    BadgeShape.SOFT_BURST -> MaterialShapes.SoftBurst.toShape()
    BadgeShape.CIRCLE -> MaterialShapes.Circle.toShape()
}

/** Rotating container/content color pairs for a Settings row's circular leading icon. */
@Composable
fun rowIconColors(index: Int): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val pairs = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        scheme.errorContainer to scheme.onErrorContainer
    )
    return pairs[index % pairs.size]
}

/** An icon on a filled, shaped container — e.g. a MaterialShapes cookie or circle. */
@Composable
fun ShapeBadge(
    icon: ImageVector,
    shape: Shape,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    size: Dp = 40.dp,
    iconSize: Dp = size * 0.5f
) {
    Box(
        modifier = modifier.size(size).clip(shape).background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
    }
}

/** [ShapeBadge] convenience overload: a Pixel-style row icon using a shared [BadgeShape] and the
 *  rotating [rowIconColors] palette for its row index. */
@Composable
fun ShapeBadge(
    icon: ImageVector,
    shape: BadgeShape,
    tintIndex: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val (container, content) = rowIconColors(tintIndex)
    ShapeBadge(icon = icon, shape = shape.toComposeShape(), modifier = modifier, containerColor = container, contentColor = content, size = size)
}

/**
 * A vertical group of segmented rows that reads as one seamless rounded card — no gap between
 * rows, matching the Pixel Settings look shared with Pepe's Notes / Clock / Weather.
 */
@Composable
fun SegmentedGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, content = { content() })
}

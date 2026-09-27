package xyz.pepe.glacierclient.calculatorapp.ui.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class CalcButtonStyle { NUMBER, OPERATOR, FUNCTION, ACCENT }

/** A single calculator key. Shape/color vary with [style] so the keypad reads with hierarchy:
 *  numbers are quiet, operators/functions are tonal, and the primary action (=) is filled. */
@Composable
fun CalculatorButton(
    label: String,
    style: CalcButtonStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = when (style) {
        CalcButtonStyle.NUMBER -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        CalcButtonStyle.OPERATOR -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        CalcButtonStyle.FUNCTION -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        CalcButtonStyle.ACCENT -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
    FilledTonalButton(
        onClick = onClick,
        colors = colors,
        modifier = modifier.fillMaxWidth().aspectRatio(1.3f).testTag("calc_btn_${label.trim()}")
    ) {
        Text(text = label, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

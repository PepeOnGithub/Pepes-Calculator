@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package xyz.pepe.glacierclient.calculatorapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.pepe.glacierclient.calculatorapp.domain.ConvertibleUnit
import xyz.pepe.glacierclient.calculatorapp.domain.UnitCategory
import xyz.pepe.glacierclient.calculatorapp.domain.UnitConverter
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Built-in unit & currency converter, reached from the mode toggle — mirrors the categories the
 * iPad Calculator exposes (length, weight, area, speed, pressure, energy, currency). Currency
 * uses a fixed approximate snapshot rate, not a live feed, since this app has no network layer.
 */
@Composable
fun ConverterScreen() {
    var category by remember { mutableStateOf(UnitCategory.LENGTH) }
    var fromUnit by remember(category) { mutableStateOf(category.units[0]) }
    var toUnit by remember(category) { mutableStateOf(category.units[1]) }
    var input by remember { mutableStateOf("1") }

    val result = remember(input, fromUnit, toUnit) {
        input.toDoubleOrNull()?.let { UnitConverter.convert(it, fromUnit, toUnit) }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Unit & currency converter", style = MaterialTheme.typography.titleLarge)
        if (category == UnitCategory.CURRENCY) {
            Text(
                "Approximate rates, not live",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(UnitCategory.entries) { cat ->
                FilterChip(
                    selected = cat == category,
                    onClick = { category = cat },
                    label = { Text(cat.label) }
                )
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Value") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UnitPicker(
                units = category.units,
                selected = fromUnit,
                onSelected = { fromUnit = it },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { val t = fromUnit; fromUnit = toUnit; toUnit = t }) {
                Icon(Icons.Default.SwapVert, contentDescription = "Swap units")
            }
            UnitPicker(
                units = category.units,
                selected = toUnit,
                onSelected = { toUnit = it },
                modifier = Modifier.weight(1f)
            )
        }

        Card(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("$input ${fromUnit.symbol} =", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = result?.let { formatConversion(it) + " " + toUnit.symbol } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun UnitPicker(
    units: List<ConvertibleUnit>,
    selected: ConvertibleUnit,
    onSelected: (ConvertibleUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Card(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(selected.symbol, style = MaterialTheme.typography.titleMedium)
                Text(
                    selected.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text("${unit.symbol} · ${unit.label}") },
                    onClick = { onSelected(unit); expanded = false }
                )
            }
        }
    }
}

private fun formatConversion(value: Double): String {
    val decimal = BigDecimal(value).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros()
    return decimal.toPlainString()
}

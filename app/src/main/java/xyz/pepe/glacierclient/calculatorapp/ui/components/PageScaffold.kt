@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.calculatorapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.compose.ui.unit.dp


/**
 * Full-screen page with a collapsing [LargeFlexibleTopAppBar]. Every top-level page uses this so
 * they all share the same scroll behaviour and look. [large] = false swaps in the small bar, for
 * the keypad page, which doesn't scroll and needs the height.
 */
@Composable
fun PageScaffold(
  title: String,
  modifier: Modifier = Modifier,
  icon: (@Composable () -> Unit)? = null,
  onBack: (() -> Unit)? = null,
  actions: @Composable RowScope.() -> Unit = {},
  floatingActionButton: @Composable () -> Unit = {},
  large: Boolean = true,
  subtitle: String? = null,
  content: @Composable (PaddingValues) -> Unit,
) {
  val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      val titleContent: @Composable () -> Unit = {
        if (icon != null) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            icon()
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        } else {
          Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
      val navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        }
      }
      if (large) LargeFlexibleTopAppBar(title = titleContent, subtitle = if (subtitle != null) { { Text(subtitle) } } else { {} }, navigationIcon = navigationIcon, actions = actions, scrollBehavior = scroll)
      else TopAppBar(title = titleContent, navigationIcon = navigationIcon, actions = actions)
    },
    floatingActionButton = floatingActionButton,
    content = content,
  )
}

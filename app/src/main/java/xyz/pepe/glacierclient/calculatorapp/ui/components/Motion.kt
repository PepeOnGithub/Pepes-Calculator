package xyz.pepe.glacierclient.calculatorapp.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext

/** True when the system "animator duration scale" is 0 (Remove animations / reduced motion). */
@Composable
fun rememberReducedMotion(): Boolean {
  val context = LocalContext.current
  return remember(context) {
    runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
  }
}

/** Hand cursor over clickable things when a mouse is attached. */
fun Modifier.handCursor(): Modifier = pointerHoverIcon(PointerIcon.Hand)

/** Right mouse button press, the desktop equivalent of a long press. */
fun Modifier.onSecondaryClick(action: () -> Unit): Modifier = pointerInput(Unit) {
  awaitPointerEventScope {
    while (true) {
      val e = awaitPointerEvent(PointerEventPass.Initial)
      if (e.type == PointerEventType.Press && e.buttons.isSecondaryPressed) action()
    }
  }
}

/** Mouse wheel / touchpad scroll over the element; called with the vertical delta (at most every 400 ms). */
fun Modifier.onWheel(action: (Float) -> Unit): Modifier = pointerInput(Unit) {
  var last = 0L
  awaitPointerEventScope {
    while (true) {
      val e = awaitPointerEvent(PointerEventPass.Initial)
      if (e.type == PointerEventType.Scroll) {
        val now = System.currentTimeMillis()
        val dy = e.changes.firstOrNull()?.scrollDelta?.y ?: 0f
        if (dy != 0f && now - last > 400) { last = now; action(dy) }
      }
    }
  }
}

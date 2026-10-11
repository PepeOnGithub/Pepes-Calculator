package xyz.pepe.glacierclient.calculatorapp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import xyz.pepe.glacierclient.calculatorapp.MainActivity
import xyz.pepe.glacierclient.calculatorapp.domain.CalculatorEngine
import xyz.pepe.glacierclient.calculatorapp.domain.NumberFormat
import xyz.pepe.glacierclient.calculatorapp.domain.NumberStyle

private val Surface = Color(0xFF161816)
private val KeyNumber = Color(0xFF262A26)
private val KeyOperator = Color(0xFF2F4A30)
private val KeyEquals = Color(0xFF7BD27C)
private val OnSurface = Color(0xFFEDEFEA)
private val Muted = Color(0xFFA9AFA6)
private val OnEquals = Color(0xFF0C260D)

private const val PREFS = "widget_calculator"
private val KeyParam = ActionParameters.Key<String>("key")

/** Plain results (no thousands separators) so a result can be fed straight back into the next expression. */
private fun evaluate(expr: String): String? =
  CalculatorEngine.evaluateOrNull(WidgetCalc.toEngine(expr), useRadians = false)
    ?.takeIf { it.isFinite() }
    ?.let { NumberFormat.format(it, NumberStyle(decimals = null, grouping = false)) }

private fun load(context: Context): WidgetCalcState {
  val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
  return WidgetCalcState(p.getString("expr", "") ?: "", p.getString("shown", "") ?: "", p.getBoolean("done", false))
}

private fun save(context: Context, s: WidgetCalcState) {
  context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
    .putString("expr", s.expr).putString("shown", s.shown).putBoolean("done", s.done).apply()
}

/** 4x4 home-screen calculator: a display, a live "= result" preview and a keypad; tap the display to open the app. */
class CalculatorWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val state = load(context)
    val preview = WidgetCalc.preview(state, ::evaluate)
    provideContent {
      Column(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Surface)).cornerRadius(24.dp).padding(8.dp)) {
        Column(
          modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
          horizontalAlignment = Alignment.End,
        ) {
          Text(
            if (state.done) state.expr else (preview ?: " "),
            maxLines = 1,
            style = TextStyle(color = ColorProvider(Muted), fontSize = 12.sp, textAlign = TextAlign.End),
            modifier = GlanceModifier.fillMaxWidth(),
          )
          Text(
            if (state.done) state.shown else state.expr.ifEmpty { "0" },
            maxLines = 1,
            style = TextStyle(color = ColorProvider(OnSurface), fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
            modifier = GlanceModifier.fillMaxWidth(),
          )
        }
        WidgetCalc.KEY_ROWS.forEach { row ->
          Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            row.forEach { key ->
              val isEquals = key == "="
              val isOperator = key in listOf("÷", "×", "−", "+", "C", "⌫", "(", ")")
              Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight().padding(2.dp)) {
                Box(
                  modifier = GlanceModifier.fillMaxSize().cornerRadius(14.dp)
                    .background(ColorProvider(if (isEquals) KeyEquals else if (isOperator) KeyOperator else KeyNumber))
                    .clickable(actionRunCallback<CalculatorKeyAction>(actionParametersOf(KeyParam to key))),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    key,
                    style = TextStyle(
                      color = ColorProvider(if (isEquals) OnEquals else OnSurface),
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Medium,
                      textAlign = TextAlign.Center,
                    ),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

class CalculatorWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = CalculatorWidget()
}

/** One key press: update the saved state, then redraw. */
class CalculatorKeyAction : ActionCallback {
  override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
    val key = parameters[KeyParam] ?: return
    save(context, WidgetCalc.press(load(context), key, ::evaluate))
    CalculatorWidget().updateAll(context)
  }
}

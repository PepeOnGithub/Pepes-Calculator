@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.calculatorapp.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import xyz.pepe.glacierclient.calculatorapp.ui.components.handCursor
import xyz.pepe.glacierclient.calculatorapp.ui.components.rememberReducedMotion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import xyz.pepe.glacierclient.calculatorapp.domain.CalculatorEngine
import xyz.pepe.glacierclient.calculatorapp.domain.NumberFormat
import xyz.pepe.glacierclient.calculatorapp.domain.NumberStyle
import xyz.pepe.glacierclient.calculatorapp.domain.Tools
import xyz.pepe.glacierclient.calculatorapp.domain.ToolsLayout
import xyz.pepe.glacierclient.calculatorapp.domain.model.AppSettings
import xyz.pepe.glacierclient.calculatorapp.ui.components.BadgeShape
import xyz.pepe.glacierclient.calculatorapp.ui.components.CheckSwitch
import xyz.pepe.glacierclient.calculatorapp.ui.components.ConnectedChoiceRow
import xyz.pepe.glacierclient.calculatorapp.ui.components.GlassCard
import xyz.pepe.glacierclient.calculatorapp.ui.components.Haptics
import xyz.pepe.glacierclient.calculatorapp.ui.components.PageScaffold
import xyz.pepe.glacierclient.calculatorapp.ui.components.ShapeBadge
import xyz.pepe.glacierclient.calculatorapp.ui.components.glassFieldColors
import xyz.pepe.glacierclient.calculatorapp.ui.components.insetColor
import xyz.pepe.glacierclient.calculatorapp.ui.components.rowIconColors
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorUiState
import xyz.pepe.glacierclient.calculatorapp.ui.theme.tabularStyle
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem

/** The Tools tab's tools. [id] is the stable key used by ToolsLayout (and saved layouts): never rename it. */
private enum class ToolId(val title: String, val short: String, val summary: String, val icon: ImageVector) {
  PERCENT("Percentages", "Percentages", "Percent of, percent change and discounts", Icons.Rounded.Percent),
  TIP("Tip & split bill", "Tip & split", "Tip, total and each person's share", Icons.Rounded.Calculate),
  TAX("Tax (GST / VAT)", "Tax", "Add tax to a price or take it out", Icons.Rounded.ReceiptLong),
  LOAN("Loan / EMI", "Loan / EMI", "Monthly instalment, total paid and interest", Icons.Rounded.Payments),
  COMPOUND("Compound interest", "Compound interest", "How savings grow over time", Icons.Rounded.Savings),
  BMI("BMI calculator", "BMI", "Body mass index from weight and height", Icons.Rounded.MonitorWeight),
  AGE("Age calculator", "Age", "Age in years, months and days, and the next birthday", Icons.Rounded.Cake),
  DATES("Date calculator", "Dates", "Days between dates, or add days to a date", Icons.Rounded.CalendarMonth),
  BASES("Number bases", "Number bases", "Binary, octal, decimal and hex with bitwise operations", Icons.Rounded.Code),
  PRIMES("Prime factors, GCD & LCM", "Primes, GCD, LCM", "Factorise numbers; greatest common divisor and least common multiple", Icons.Rounded.Tag),
  STATS("Statistics", "Statistics", "Mean, median, mode and standard deviation of a list", Icons.Rounded.QueryStats),
  RANDOM("Random numbers & dice", "Random & dice", "Random number in a range, dice and a coin", Icons.Rounded.Casino);

  val id: String get() = name.lowercase()
}

private val BADGE_SHAPES = listOf(BadgeShape.COOKIE, BadgeShape.SOFT_BURST, BadgeShape.CLOVER, BadgeShape.PUFFY, BadgeShape.GEM, BadgeShape.SUNNY)

private val TILES: Map<String, TileInfo> = ToolId.entries.associate {
  it.id to TileInfo(it.id, it.title, it.short, it.summary, it.icon, it.ordinal, BADGE_SHAPES[it.ordinal % BADGE_SHAPES.size])
}

@Composable
fun ToolsScreen(
  state: CalculatorUiState,
  haptics: Haptics,
  openSettings: () -> Unit,
  update: ((AppSettings) -> AppSettings) -> Unit = {},
  editRequested: Boolean = false,
  onEditConsumed: () -> Unit = {},
) {
  var open by rememberSaveable { mutableStateOf<String?>(null) }
  var editing by rememberSaveable { mutableStateOf(false) }
  val tool = ToolId.entries.firstOrNull { it.name == open }
  val style = state.settings.numberStyle
  val raw = state.settings.toolsLayout
  // The working copy is authoritative while editing (so drags never fight the DataStore round trip).
  var work by remember { mutableStateOf(ToolsLayout.load(raw)) }
  LaunchedEffect(raw, editing) { if (!editing) work = ToolsLayout.load(raw) }
  LaunchedEffect(editRequested) {
    if (editRequested) { open = null; editing = true; onEditConsumed() }
  }
  val save = { s: ToolsLayout.State -> update { it.copy(toolsLayout = ToolsLayout.encode(s)) } }
  val reduced = rememberReducedMotion()
  BackHandler(enabled = tool != null) { open = null }
  BackHandler(enabled = tool == null && editing) { editing = false }
  val back = { open = null }
  when (tool) {
    null -> PageScaffold(
      title = if (editing) "Edit tools" else "Tools",
      subtitle = if (editing) "Tap a tool to resize, move, pin or hide it" else "Money, dates, health and number helpers",
      actions = {
        if (editing) {
          TextButton(onClick = { haptics.confirm(); work = ToolsLayout.default(); save(work) }) { Text("Reset to default") }
          Button(onClick = { haptics.confirm(); save(work); editing = false }, modifier = Modifier.padding(end = 8.dp)) { Text("Done") }
        } else {
          var menu by remember { mutableStateOf(false) }
          IconButton(onClick = { haptics.tick(); editing = true }) { Icon(Icons.Rounded.Edit, contentDescription = "Edit tools") }
          IconButton(onClick = openSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
          Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More options") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
              DropdownMenuItem(text = { Text("Edit tools layout") }, onClick = { menu = false; editing = true })
              DropdownMenuItem(text = { Text("Reset tools layout") }, onClick = { menu = false; work = ToolsLayout.default(); save(work) })
              DropdownMenuItem(text = { Text("Settings") }, onClick = { menu = false; openSettings() })
            }
          }
        }
      },
    ) { padding ->
      Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(
          Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        ) {
          ToolsGrid(
            tiles = TILES, state = work, editing = editing, haptics = haptics, reduced = reduced,
            onOpen = { id -> open = id.uppercase() },
            onChange = { s, persist -> work = s; if (persist) save(s) },
          )
        }
      }
    }
    ToolId.AGE -> AgeTool(haptics, back)
    ToolId.BMI -> BmiTool(style, haptics, back)
    ToolId.COMPOUND -> CompoundTool(style, haptics, back)
    ToolId.DATES -> DatesTool(haptics, back)
    ToolId.LOAN -> LoanTool(style, haptics, back)
    ToolId.BASES -> BasesTool(haptics, back)
    ToolId.PERCENT -> PercentTool(style, haptics, back)
    ToolId.PRIMES -> PrimesTool(haptics, back)
    ToolId.RANDOM -> RandomTool(haptics, back)
    ToolId.STATS -> StatsTool(style, haptics, back)
    ToolId.TAX -> TaxTool(state, haptics, back)
    ToolId.TIP -> TipTool(state, haptics, back)
  }
}

// Shared pieces -------------------------------------------------------------------------------

@Composable
private fun ToolPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
  PageScaffold(title = title, onBack = onBack) { padding ->
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
      Column(
        Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
      )
    }
  }
}

/** A tool's input (or result) card: glass, uppercase icon header, 12 dp between children. */
@Composable
private fun ToolCard(title: String, icon: ImageVector = Icons.Rounded.Edit, content: @Composable ColumnScope.() -> Unit) {
  GlassCard(Modifier.fillMaxWidth(), headerIcon = icon, headerTitle = title) {
    val reduced = rememberReducedMotion()
    Column(
      Modifier.animateContentSize(if (reduced) snap() else spring(dampingRatio = 0.85f, stiffness = 500f)),
      verticalArrangement = Arrangement.spacedBy(12.dp), content = content,
    )
  }
}

@Composable
private fun Field(
  label: String,
  value: String,
  onChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  keyboard: KeyboardType = KeyboardType.Decimal,
  error: String? = null,
) {
  // Numeric fields flag anything that isn't a number (basic math like 12*3 is accepted).
  val message = error ?: if (keyboard == KeyboardType.Decimal && value.isNotBlank() && num(value) == null) "Enter a number" else null
  OutlinedTextField(
    value = value, onValueChange = { onChange(it.replace('\n', ' ')) }, label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }, singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Done),
    shape = MaterialTheme.shapes.medium, colors = glassFieldColors(),
    isError = message != null, supportingText = if (message != null) { { Text(message) } } else null,
    modifier = modifier.fillMaxWidth(),
  )
}

/** A number typed into a field; basic math ("12*3") is accepted too. Null when blank or invalid. */
private fun num(text: String): Double? = if (text.isBlank()) null else CalculatorEngine.evaluateOrNull(text, false)

private fun money(x: Double, style: NumberStyle) = NumberFormat.format(Tools.round2(x), NumberStyle(2, style.grouping))

private fun plain(x: Double, style: NumberStyle) =
  NumberFormat.format(if (style.decimals == null) Math.round(x * 10000) / 10000.0 else x, style)

@Composable
private fun Hint(text: String) {
  Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp).padding(top = 2.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

/**
 * The answer: first row is the headline (big number in an inset panel), the rest are sub-rows.
 * Copy and Send to Pepe's Notes act on all rows.
 */
@Composable
private fun ResultPanel(title: String, rows: List<Pair<String, String>>) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val text = title + "\n" + rows.joinToString("\n") { "${it.first}: ${it.second}" }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val head = rows.first()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(insetColor()).padding(horizontal = 16.dp, vertical = 14.dp)) {
      Text(head.first, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      BasicText(
        head.second,
        style = tabularStyle(MaterialTheme.typography.headlineLarge).copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold),
        maxLines = 1, overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 36.sp, stepSize = 1.sp),
        modifier = Modifier.fillMaxWidth(),
      )
    }
    rows.drop(1).forEachIndexed { i, (k, v) ->
      if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(k, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
          v, style = tabularStyle(MaterialTheme.typography.titleMedium), color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.End, modifier = Modifier.weight(1.3f),
        )
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      TextButton(onClick = { clipboard.setText(AnnotatedString(text)) }) { Text("Copy") }
      TextButton(onClick = {
        if (!PepeEcosystem.send(context, PepeEcosystem.CreateNote(text, title))) shareAsText(context, text)
      }) { Text("Send to Notes") }
    }
  }
}

/** The result as its own card. */
@Composable
private fun ResultCard(title: String, rows: List<Pair<String, String>>) {
  ToolCard(title, Icons.Rounded.Calculate) { ResultPanel(title, rows) }
}

private fun shareAsText(context: Context, text: String) {
  val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
  runCatching { context.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun todayYmd(): Tools.Ymd {
  val c = Calendar.getInstance()
  return Tools.Ymd(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

private val DATE_RE = Regex("""^\s*(\d{4})-(\d{1,2})-(\d{1,2})\s*$""")

private fun parseYmd(text: String): Tools.Ymd? {
  val m = DATE_RE.matchEntire(text) ?: return null
  val (y, mo, d) = m.destructured
  return if (Tools.isValidDate(y.toInt(), mo.toInt(), d.toInt())) Tools.Ymd(y.toInt(), mo.toInt(), d.toInt()) else null
}

private fun dateError(text: String): String? = if (text.isNotBlank() && parseYmd(text) == null) "Use the format YYYY-MM-DD, like 2026-03-15" else null

// Tools -----------------------------------------------------------------------------------------

@Composable
private fun PercentTool(style: NumberStyle, haptics: Haptics, onBack: () -> Unit) {
  var p1 by rememberSaveable { mutableStateOf("15") }
  var v1 by rememberSaveable { mutableStateOf("200") }
  var a2 by rememberSaveable { mutableStateOf("30") }
  var b2 by rememberSaveable { mutableStateOf("120") }
  var a3 by rememberSaveable { mutableStateOf("80") }
  var b3 by rememberSaveable { mutableStateOf("100") }
  var price by rememberSaveable { mutableStateOf("59.99") }
  var off by rememberSaveable { mutableStateOf("25") }
  ToolPage("Percentages", onBack) {
    ToolCard("What is X% of Y?", Icons.Rounded.Percent) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("Percent", p1, { p1 = it }, Modifier.weight(1f))
        Field("Of", v1, { v1 = it }, Modifier.weight(1f))
      }
      num(p1)?.let { p -> num(v1)?.let { v -> ResultPanel("Percent of", listOf("${p1}% of ${v1}" to plain(Tools.percentOf(p, v), style))) } }
    }

    ToolCard("X is what percent of Y?", Icons.Rounded.Percent) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("Part", a2, { a2 = it }, Modifier.weight(1f))
        Field("Whole", b2, { b2 = it }, Modifier.weight(1f))
      }
      val w = num(a2)?.let { a -> num(b2)?.let { b -> Tools.whatPercent(a, b) } }
      if (w != null) ResultPanel("What percent", listOf("$a2 of $b2" to plain(w, style) + "%"))
      else if (num(a2) != null && num(b2) != null) Hint("The whole can't be zero.")
    }

    ToolCard("Percent change", Icons.Rounded.QueryStats) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("From", a3, { a3 = it }, Modifier.weight(1f))
        Field("To", b3, { b3 = it }, Modifier.weight(1f))
      }
      val ch = num(a3)?.let { a -> num(b3)?.let { b -> Tools.percentChange(a, b) } }
      if (ch != null) ResultPanel("Percent change", listOf("$a3 to $b3" to (if (ch > 0) "+" else "") + plain(ch, style) + "%"))
      else if (num(a3) != null && num(b3) != null) Hint("The starting value can't be zero.")
    }

    ToolCard("Discount", Icons.Rounded.Payments) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("Price", price, { price = it }, Modifier.weight(1f))
        Field("Discount %", off, { off = it }, Modifier.weight(1f))
      }
      val dc = num(price)?.let { pr -> num(off)?.let { o -> Tools.discount(pr, o) } }
      if (dc != null) ResultPanel("Discount", listOf("Final price" to money(dc.finalPrice, style), "You save" to money(dc.saved, style)))
    }
  }
}

@Composable
private fun TipTool(state: CalculatorUiState, haptics: Haptics, onBack: () -> Unit) {
  val style = state.settings.numberStyle
  var bill by rememberSaveable { mutableStateOf("") }
  var tip by rememberSaveable { mutableStateOf(state.settings.tipPercent.toString()) }
  var people by rememberSaveable { mutableIntStateOf(1) }
  ToolPage("Tip & split bill", onBack) {
    ToolCard("Bill") {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("Bill", bill, { bill = it }, Modifier.weight(1.4f))
        Field("Tip %", tip, { tip = it }, Modifier.weight(1f))
      }
      ConnectedChoiceRow(listOf(0, 10, 15, 18, 20), num(tip)?.toInt() ?: -1, { "$it%" }, { tip = it.toString() }, haptics)
      Text("People: $people", style = MaterialTheme.typography.titleSmall)
      ConnectedChoiceRow((1..6).toList(), people.coerceIn(1, 6), { it.toString() }, { people = it }, haptics)
    }
    val b = num(bill); val t = num(tip)
    if (b != null && t != null) {
      val r = Tools.tipSplit(b, t, people)
      ResultCard(
        "Tip & split",
        listOf("Total" to money(r.total, style), "Tip" to money(r.tip, style)) +
          if (people > 1) listOf("Each pays" to money(r.perPerson, style), "Tip each" to money(r.tipPerPerson, style)) else emptyList(),
      )
    } else ToolCard("Result", Icons.Rounded.Calculate) { Hint("Enter the bill to see the tip and each person's share.") }
  }
}

@Composable
private fun TaxTool(state: CalculatorUiState, haptics: Haptics, onBack: () -> Unit) {
  val style = state.settings.numberStyle
  var amount by rememberSaveable { mutableStateOf("") }
  var rate by rememberSaveable { mutableStateOf(state.settings.taxPercent.toString()) }
  var inclusive by rememberSaveable { mutableStateOf(false) }
  ToolPage("Tax (GST / VAT)", onBack) {
    ToolCard("Amount") {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("Amount", amount, { amount = it }, Modifier.weight(1.4f))
        Field("Tax rate %", rate, { rate = it }, Modifier.weight(1f))
      }
      ConnectedChoiceRow(listOf(5, 10, 12, 18, 20), num(rate)?.toInt() ?: -1, { "$it%" }, { rate = it.toString() }, haptics)
      Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Amount already includes tax", modifier = Modifier.weight(1f))
        CheckSwitch(inclusive, { inclusive = it }, haptics)
      }
    }
    val a = num(amount); val r = num(rate)
    if (a != null && r != null) {
      val t = Tools.tax(a, r, inclusive)
      ResultCard("Tax", if (inclusive) listOf("Price before tax" to money(t.net, style), "Tax" to money(t.tax, style), "With tax" to money(t.gross, style))
        else listOf("Total with tax" to money(t.gross, style), "Tax" to money(t.tax, style), "Before tax" to money(t.net, style)))
    } else ToolCard("Result", Icons.Rounded.Calculate) { Hint("Enter an amount and a rate.") }
  }
}

@Composable
private fun LoanTool(style: NumberStyle, haptics: Haptics, onBack: () -> Unit) {
  var principal by rememberSaveable { mutableStateOf("") }
  var rate by rememberSaveable { mutableStateOf("") }
  var years by rememberSaveable { mutableStateOf("") }
  ToolPage("Loan / EMI", onBack) {
    ToolCard("Loan") {
      Field("Loan amount", principal, { principal = it })
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("Rate % / year", rate, { rate = it }, Modifier.weight(1f))
        Field("Term (years)", years, { years = it }, Modifier.weight(1f))
      }
    }
    val p = num(principal); val r = num(rate); val y = num(years)
    val loan = if (p != null && r != null && y != null) Tools.loan(p, r, Math.round(y * 12).toInt()) else null
    if (loan != null) ResultCard("Loan", listOf("Monthly payment" to money(loan.monthly, style), "Total paid" to money(loan.total, style), "Total interest" to money(loan.interest, style)))
    else ToolCard("Result", Icons.Rounded.Calculate) {
      Hint(if (p != null && r != null && y != null) "The loan amount can't be negative and the term must be at least one month." else "Fill in all three fields to see the monthly payment (EMI).")
    }
  }
}

@Composable
private fun CompoundTool(style: NumberStyle, haptics: Haptics, onBack: () -> Unit) {
  var principal by rememberSaveable { mutableStateOf("") }
  var rate by rememberSaveable { mutableStateOf("") }
  var years by rememberSaveable { mutableStateOf("") }
  var times by rememberSaveable { mutableIntStateOf(12) }
  ToolPage("Compound interest", onBack) {
    ToolCard("Savings") {
      Field("Starting amount", principal, { principal = it })
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("Rate % / year", rate, { rate = it }, Modifier.weight(1f))
        Field("Years", years, { years = it }, Modifier.weight(1f))
      }
      Text("Compounded", style = MaterialTheme.typography.titleSmall)
      ConnectedChoiceRow(
        listOf(1, 2, 4, 12, 365), times,
        { when (it) { 1 -> "Yearly"; 2 -> "Half-yearly"; 4 -> "Quarterly"; 12 -> "Monthly"; else -> "Daily" } },
        { times = it }, haptics,
      )
    }
    val p = num(principal); val r = num(rate); val y = num(years)
    val c = if (p != null && r != null && y != null) Tools.compound(p, r, y, times) else null
    if (c != null) ResultCard("Compound interest", listOf("Final amount" to money(c.finalAmount, style), "Interest earned" to money(c.interest, style)))
    else ToolCard("Result", Icons.Rounded.Calculate) {
      Hint(if (p != null && r != null && y != null) "The number of years can't be negative." else "Fill in all three fields to see the growth.")
    }
  }
}

@Composable
private fun BmiTool(style: NumberStyle, haptics: Haptics, onBack: () -> Unit) {
  var kg by rememberSaveable { mutableStateOf("") }
  var cm by rememberSaveable { mutableStateOf("") }
  ToolPage("BMI calculator", onBack) {
    ToolCard("Body") {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("Weight (kg)", kg, { kg = it }, Modifier.weight(1f))
        Field("Height (cm)", cm, { cm = it }, Modifier.weight(1f))
      }
    }
    val b = num(kg)?.let { w -> num(cm)?.let { h -> Tools.bmi(w, h) } }
    if (b != null) ResultCard("BMI", listOf("BMI" to NumberFormat.format(b, NumberStyle(1, false)), "Category" to Tools.bmiCategory(b)))
    else ToolCard("Result", Icons.Rounded.Calculate) { Hint("Enter your weight and height. BMI is a rough guide, not a diagnosis.") }
  }
}

@Composable
private fun DatesTool(haptics: Haptics, onBack: () -> Unit) {
  val today = remember { todayYmd() }
  var mode by rememberSaveable { mutableIntStateOf(0) }
  var start by rememberSaveable { mutableStateOf(today.toString()) }
  var end by rememberSaveable { mutableStateOf(today.toString()) }
  var days by rememberSaveable { mutableStateOf("30") }
  ToolPage("Date calculator", onBack) {
    ToolCard("Dates", Icons.Rounded.CalendarMonth) {
      ConnectedChoiceRow(listOf(0, 1), mode, { if (it == 0) "Days between" else "Add days" }, { mode = it }, haptics)
      Field("Start date (YYYY-MM-DD)", start, { start = it }, keyboard = KeyboardType.Text, error = dateError(start))
      if (mode == 0) Field("End date (YYYY-MM-DD)", end, { end = it }, keyboard = KeyboardType.Text, error = dateError(end))
      else Field(
        "Days to add (negative to subtract)", days, { days = it }, keyboard = KeyboardType.Number,
        error = if (days.isNotBlank() && days.trim().toLongOrNull() == null) "Enter a whole number" else null,
      )
    }
    val a = parseYmd(start)
    if (mode == 0) {
      val b = parseYmd(end)
      if (a != null && b != null) {
        val d = Tools.daysBetween(a, b)
        val abs = Math.abs(d)
        ResultCard("Days between", listOf("Days" to "$d", "Weeks" to "${abs / 7} wk ${abs % 7} d", "Start day" to Tools.weekdayName(a), "End day" to Tools.weekdayName(b)))
      } else ToolCard("Result", Icons.Rounded.Calculate) { Hint("Use the format 2026-03-15 for both dates.") }
    } else {
      val n = days.trim().toLongOrNull()
      if (a != null && n != null && Math.abs(n) < 3_000_000L) {
        val r = Tools.addDays(a, n)
        ResultCard("Date", listOf("Result" to r.toString(), "Day" to Tools.weekdayName(r)))
      } else ToolCard("Result", Icons.Rounded.Calculate) { Hint("Enter a start date like 2026-03-15 and a whole number of days.") }
    }
  }
}

@Composable
private fun AgeTool(haptics: Haptics, onBack: () -> Unit) {
  var birth by rememberSaveable { mutableStateOf("") }
  ToolPage("Age calculator", onBack) {
    ToolCard("Birth date", Icons.Rounded.Cake) {
      Field("Date of birth (YYYY-MM-DD)", birth, { birth = it }, keyboard = KeyboardType.Text, error = dateError(birth))
    }
    val b = parseYmd(birth)
    val age = b?.let { Tools.age(it, todayYmd()) }
    if (age != null) ResultCard(
      "Age",
      listOf(
        "Age" to "${age.years} y ${age.months} m ${age.days} d",
        "Days lived" to NumberFormat.format(age.totalDays.toDouble(), NumberStyle(0, true)),
        "Next birthday in" to if (age.daysToBirthday == 0L) "Today!" else "${age.daysToBirthday} days",
      ),
    ) else ToolCard("Result", Icons.Rounded.Calculate) {
      Hint(if (b != null) "That date is in the future." else "Enter a birth date like 1995-08-23.")
    }
  }
}

@Composable
private fun StatsTool(style: NumberStyle, haptics: Haptics, onBack: () -> Unit) {
  var text by rememberSaveable { mutableStateOf("4, 8, 15, 16, 23, 42") }
  ToolPage("Statistics", onBack) {
    val values = Tools.parseNumbers(text)
    ToolCard("Numbers", Icons.Rounded.QueryStats) {
      OutlinedTextField(
        value = text, onValueChange = { text = it }, label = { Text("Separate with commas, spaces or new lines") },
        shape = MaterialTheme.shapes.medium, colors = glassFieldColors(), minLines = 3,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        isError = values == null, supportingText = if (values == null) { { Text("Some entries aren't numbers") } } else null,
        modifier = Modifier.fillMaxWidth(),
      )
    }
    val s = values?.let { Tools.stats(it) }
    if (s != null) ResultCard(
      "Statistics",
      listOf(
        "Mean" to plain(s.mean, style), "Count" to "${s.count}", "Sum" to plain(s.sum, style), "Median" to plain(s.median, style),
        "Mode" to if (s.modes.isEmpty()) "none" else s.modes.joinToString(", ") { plain(it, style) },
        "Smallest" to plain(s.min, style), "Largest" to plain(s.max, style), "Range" to plain(s.range, style),
        "Std. deviation (population)" to plain(s.populationStdDev, style),
        "Std. deviation (sample)" to (s.sampleStdDev?.let { plain(it, style) } ?: "needs 2+ numbers"),
      ),
    ) else if (values != null) ToolCard("Result", Icons.Rounded.Calculate) { Hint("Type some numbers to see their statistics.") }
  }
}

@Composable
private fun PrimesTool(haptics: Haptics, onBack: () -> Unit) {
  var text by rememberSaveable { mutableStateOf("360") }
  ToolPage("Prime factors, GCD & LCM", onBack) {
    val nums = text.split(Regex("[\\s,;]+")).filter { it.isNotEmpty() }.map { it.toLongOrNull() }
    val bad = nums.any { it == null || it < 1 }
    ToolCard("Numbers", Icons.Rounded.Tag) {
      Field(
        "Whole numbers (spaces or commas)", text, { text = it }, keyboard = KeyboardType.Text,
        error = if (text.isNotBlank() && bad) "Use whole numbers from 1 up" else null,
      )
    }
    if (nums.isEmpty() || bad) {
      ToolCard("Result", Icons.Rounded.Calculate) {
        Hint("Enter one or more whole numbers from 1 up. One number is factorised; two or more also get their GCD and LCM.")
      }
    } else {
      val values = nums.filterNotNull()
      val rows = ArrayList<Pair<String, String>>()
      if (values.size > 1) {
        rows += "GCD" to (Tools.gcdAll(values)?.toString() ?: "—")
        rows += "LCM" to (Tools.lcmAll(values)?.toString() ?: "too large")
      }
      for (v in values.take(6)) {
        val f = Tools.primeFactors(v)
        rows += "$v" to when {
          v == 1L -> "1 (neither prime nor composite)"
          f.size == 1 -> "prime"
          else -> Tools.factorsText(f)
        }
      }
      ResultCard("Prime factors", rows)
    }
  }
}

@Composable
private fun BasesTool(haptics: Haptics, onBack: () -> Unit) {
  var base by rememberSaveable { mutableIntStateOf(10) }
  var a by rememberSaveable { mutableStateOf("255") }
  var b by rememberSaveable { mutableStateOf("15") }
  var opIndex by rememberSaveable { mutableIntStateOf(0) }
  val ops = Tools.BitOp.entries
  ToolPage("Number bases", onBack) {
    val x = Tools.parseBase(a, base)
    val y = Tools.parseBase(b, base)
    ToolCard("Convert", Icons.Rounded.Code) {
      Text("Type the number in", style = MaterialTheme.typography.titleSmall)
      ConnectedChoiceRow(listOf(2, 8, 10, 16), base, { when (it) { 2 -> "BIN"; 8 -> "OCT"; 10 -> "DEC"; else -> "HEX" } }, { base = it }, haptics)
      Field("Number", a, { a = it }, keyboard = KeyboardType.Text, error = if (a.isNotBlank() && x == null) "That isn't a valid base-$base number" else null)
      if (x != null) ResultPanel("Number bases", baseRows(x))
    }

    ToolCard("Bitwise operation", Icons.Rounded.Calculate) {
      ConnectedChoiceRow(ops, ops[opIndex.coerceIn(0, ops.lastIndex)], { it.label }, { opIndex = ops.indexOf(it) }, haptics)
      Field("Second number (same base)", b, { b = it }, keyboard = KeyboardType.Text, error = if (b.isNotBlank() && y == null) "That isn't a valid base-$base number" else null)
      if (x != null && y != null) {
        val op = ops[opIndex.coerceIn(0, ops.lastIndex)]
        ResultPanel("${a} ${op.label} ${b}", baseRows(Tools.bitwise(op, x, y)))
      }
    }
  }
}

private fun baseRows(v: Long) = listOf(
  "Decimal" to v.toString(), "Hex" to Tools.toBase(v, 16), "Octal" to Tools.toBase(v, 8), "Binary" to Tools.toBase(v, 2),
)

@Composable
private fun RandomTool(haptics: Haptics, onBack: () -> Unit) {
  val rng = remember { java.util.Random() }
  var min by rememberSaveable { mutableStateOf("1") }
  var max by rememberSaveable { mutableStateOf("100") }
  var number by rememberSaveable { mutableStateOf("") }
  var sides by rememberSaveable { mutableIntStateOf(6) }
  var count by rememberSaveable { mutableIntStateOf(2) }
  var dice by rememberSaveable { mutableStateOf("") }
  var coin by rememberSaveable { mutableStateOf("") }
  ToolPage("Random numbers & dice", onBack) {
    ToolCard("Random number", Icons.Rounded.Casino) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("Smallest", min, { min = it }, Modifier.weight(1f))
        Field("Largest", max, { max = it }, Modifier.weight(1f))
      }
      val lo = num(min)?.toLong(); val hi = num(max)?.toLong()
      Button(enabled = lo != null && hi != null, onClick = {
        if (lo != null && hi != null) { haptics.confirm(); number = Tools.randomInt(lo, hi, rng).toString() }
      }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Generate") }
      if (number.isNotEmpty()) ResultPanel("Random number", listOf("Number" to number))
    }

    ToolCard("Dice", Icons.Rounded.Casino) {
      ConnectedChoiceRow(listOf(4, 6, 8, 10, 12, 20), sides, { "d$it" }, { sides = it }, haptics)
      Text("Number of dice: $count", style = MaterialTheme.typography.titleSmall)
      ConnectedChoiceRow((1..6).toList(), count, { it.toString() }, { count = it }, haptics)
      Button(onClick = {
        haptics.confirm()
        val rolls = Tools.rollDice(count, sides, rng)
        dice = rolls.joinToString(" + ") + (if (rolls.size > 1) " = ${rolls.sum()}" else "")
      }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Roll") }
      if (dice.isNotEmpty()) ResultPanel("Dice roll", listOf("Result" to dice))
    }

    ToolCard("Coin", Icons.Rounded.Casino) {
      FilledTonalButton(onClick = { haptics.confirm(); coin = if (rng.nextBoolean()) "Heads" else "Tails" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Flip a coin") }
      if (coin.isNotEmpty()) ResultPanel("Coin flip", listOf("Result" to coin))
    }
  }
}

package app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rork.qu1cg8ux8i9pbr72mmsss.data.BankCard
import app.rork.qu1cg8ux8i9pbr72mmsss.data.Transaction
import app.rork.qu1cg8ux8i9pbr72mmsss.data.WalletData
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.BankCardView
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.Fmt
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.MpayLogo
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.TransactionRow
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.AmountStyle
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay
import java.util.Calendar

@Composable
fun HomeScreen(
    data: WalletData,
    contentPadding: PaddingValues,
    onSettings: () -> Unit,
    onOpenCard: (String) -> Unit,
    onAddCard: () -> Unit,
    onAllCards: () -> Unit,
    onAllHistory: () -> Unit,
    onTransaction: (Transaction) -> Unit,
    onPay: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 84.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MpayLogo(30.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Настройки") }
                }
            }
            item { SpendingCard(data.transactions) }
            item { SectionHeader("Мои карты", if (data.cards.isNotEmpty()) onAllCards else null) }
            item {
                if (data.cards.isEmpty()) EmptyCardsTile(onAddCard)
                else LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(data.cards, key = { it.id }) { c ->
                        BankCardView(
                            c.brand, c.number, c.style, compact = true,
                            modifier = Modifier.width(196.dp).clickable { onOpenCard(c.id) }
                        )
                    }
                    item { AddCardTile(onAddCard) }
                }
            }
            item { SectionHeader("Последние операции", if (data.transactions.isNotEmpty()) onAllHistory else null) }
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Mpay.Surface),
                ) {
                    val recent = data.transactions.take(3)
                    if (recent.isEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.ReceiptLong, null, tint = Mpay.TextSecondary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Здесь появятся ваши покупки", color = Mpay.TextSecondary)
                        }
                    } else recent.forEachIndexed { i, t ->
                        if (i > 0) HorizontalDivider(Modifier.padding(start = 76.dp), color = Mpay.Outline)
                        TransactionRow(t, Fmt.shortDateTime(t.timestamp)) { onTransaction(t) }
                    }
                }
            }
        }
        SwipeToPayHandle(
            enabled = data.cards.isNotEmpty(),
            onPay = onPay,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun SectionHeader(title: String, onAll: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (onAll != null) TextButton(onClick = onAll) {
            Text("Все", color = Mpay.TextSecondary)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Mpay.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

private enum class ChartRange(val title: String) { Week("Неделя"), HalfYear("Полгода") }

@Composable
private fun SpendingCard(txs: List<Transaction>) {
    var range by rememberSaveable { mutableStateOf(ChartRange.Week) }
    var menu by remember { mutableStateOf(false) }
    val now = Calendar.getInstance()
    val month = now.get(Calendar.MONTH)
    val year = now.get(Calendar.YEAR)
    fun monthSum(m: Int, y: Int) = txs.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
        c.get(Calendar.MONTH) == m && c.get(Calendar.YEAR) == y
    }.sumOf { it.amount }
    val thisMonth = monthSum(month, year)
    val prevM = if (month == 0) 11 else month - 1
    val prevY = if (month == 0) year - 1 else year
    val prevMonth = monthSum(prevM, prevY)

    val bars: List<Pair<String, Double>> = when (range) {
        ChartRange.Week -> {
            val names = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
            (6 downTo 0).map { back ->
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -back) }
                val start = Fmt.dayStart(c.timeInMillis)
                names[c.get(Calendar.DAY_OF_WEEK) - 1] to txs.filter { it.timestamp in start until start + 86_400_000L }.sumOf { it.amount }
            }
        }
        ChartRange.HalfYear -> {
            val short = listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
            (5 downTo 0).map { back ->
                val c = Calendar.getInstance().apply { add(Calendar.MONTH, -back) }
                short[c.get(Calendar.MONTH)] to monthSum(c.get(Calendar.MONTH), c.get(Calendar.YEAR))
            }
        }
    }

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Mpay.Surface),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Расходы в ${Fmt.monthsPrep[month]}", color = Mpay.TextSecondary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(Fmt.money(thisMonth), style = AmountStyle)
                }
                Box {
                    Surface(onClick = { menu = true }, shape = RoundedCornerShape(50), color = Mpay.SurfaceHigh) {
                        Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(range.title, style = MaterialTheme.typography.labelLarge)
                            Icon(Icons.Rounded.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
                        }
                    }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        ChartRange.entries.forEach { r ->
                            DropdownMenuItem(text = { Text(r.title) }, onClick = { range = r; menu = false })
                        }
                    }
                }
            }
            if (prevMonth > 0) {
                val pct = ((thisMonth - prevMonth) / prevMonth * 100).toInt()
                val down = pct <= 0
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(if (down) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, null, tint = if (down) Mpay.Income else Mpay.Expense, modifier = Modifier.size(16.dp))
                    Text(
                        "${if (pct > 0) "+" else "−"}${kotlin.math.abs(pct)}%",
                        color = if (down) Mpay.Income else Mpay.Expense, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 2.dp, end = 4.dp)
                    )
                    Text("к ${Fmt.monthsDat[prevM]}", color = Mpay.TextSecondary)
                }
            } else {
                Text("Операции за месяц: ${txs.count { val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }; c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year }}", color = Mpay.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(18.dp))
            BarChart(bars, highlightLast = true)
        }
    }
}

@Composable
fun BarChart(bars: List<Pair<String, Double>>, highlightLast: Boolean = false, height: Int = 110) {
    val max = (bars.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    val grow = remember { Animatable(0f) }
    LaunchedEffect(bars) { grow.snapTo(0f); grow.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.height(height.dp).padding(end = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(Fmt.compact(max), color = Mpay.TextSecondary, fontSize = 11.sp)
            Text(Fmt.compact(max / 2), color = Mpay.TextSecondary, fontSize = 11.sp)
            Text("0", color = Mpay.TextSecondary, fontSize = 11.sp)
        }
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth().height(height.dp), verticalAlignment = Alignment.Bottom) {
                bars.forEachIndexed { i, (_, v) ->
                    val frac = (v / max).toFloat() * grow.value
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.55f)
                                .fillMaxHeight(frac.coerceAtLeast(if (v > 0) 0.04f else 0.015f))
                                .background(
                                    if (v <= 0) Brush.verticalGradient(listOf(Mpay.SurfaceHigh, Mpay.SurfaceHigh))
                                    else if (highlightLast && i == bars.lastIndex) Brush.verticalGradient(listOf(Mpay.Lime, Mpay.Lime))
                                    else Brush.verticalGradient(listOf(Mpay.Lime.copy(alpha = 0.75f), Mpay.Lime.copy(alpha = 0.35f))),
                                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                                )
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                bars.forEach { (label, _) ->
                    Text(label, color = Mpay.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun AddCardTile(onAdd: () -> Unit) {
    Surface(
        onClick = onAdd,
        shape = RoundedCornerShape(18.dp),
        color = Mpay.Surface,
        modifier = Modifier.width(120.dp).aspectRatio(0.98f).border(1.dp, Mpay.Outline, RoundedCornerShape(18.dp)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(40.dp).background(Mpay.LimeDim, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, null, tint = Mpay.Lime)
            }
            Spacer(Modifier.height(8.dp))
            Text("Добавить", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EmptyCardsTile(onAdd: () -> Unit) {
    Surface(
        onClick = onAdd,
        shape = RoundedCornerShape(22.dp),
        color = Mpay.Surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).border(1.dp, Mpay.Outline, RoundedCornerShape(22.dp)),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(Mpay.Lime, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, null, tint = Mpay.OnLime, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Добавьте первую карту", style = MaterialTheme.typography.titleMedium)
                Text("Сканируйте камерой или приложите карту к телефону", color = Mpay.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Samsung Pay–style grab handle: drag up (or tap) to pull out the payment card. */
@Composable
fun SwipeToPayHandle(enabled: Boolean, onPay: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val drag = remember { Animatable(0f) }
    var dragAmount by remember { mutableStateOf(0f) }
    val bob = rememberInfiniteTransition(label = "bob")
    val bobY by bob.animateFloat(0f, -5f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bobY")
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Mpay.Canvas.copy(alpha = 0f), Mpay.Canvas, Mpay.Canvas)))
            .padding(top = 18.dp, bottom = 10.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { dragAmount = 0f },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        dragAmount = (dragAmount + dy).coerceAtMost(0f)
                    },
                    onDragEnd = {
                        if (dragAmount < -60f) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onPay() }
                        dragAmount = 0f
                    },
                    onDragCancel = { dragAmount = 0f },
                )
            }
            .clickable(enabled = enabled, role = Role.Button) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onPay() }
            .semantics { contentDescription = "Оплатить: смахните вверх" }
            .graphicsLayer { translationY = (dragAmount * 0.4f).coerceAtLeast(-40f) + drag.value },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.KeyboardArrowUp, null,
            tint = if (enabled) Mpay.Lime else Mpay.TextSecondary,
            modifier = Modifier.size(28.dp).offset(y = if (enabled) bobY.dp else 0.dp)
        )
        Text(
            if (enabled) "Смахните вверх, чтобы оплатить" else "Добавьте карту, чтобы оплачивать",
            color = Mpay.TextSecondary, style = MaterialTheme.typography.bodyMedium
        )
    }
}

fun cardFor(data: WalletData, t: Transaction): BankCard? = data.cards.firstOrNull { it.id == t.cardId }

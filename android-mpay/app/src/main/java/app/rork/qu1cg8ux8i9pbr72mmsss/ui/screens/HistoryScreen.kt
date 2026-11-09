package app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rork.qu1cg8ux8i9pbr72mmsss.data.BankCard
import app.rork.qu1cg8ux8i9pbr72mmsss.data.Transaction
import app.rork.qu1cg8ux8i9pbr72mmsss.data.WalletData
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.Fmt
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.MerchantAvatar
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.TransactionRow
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.AmountStyle
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay

enum class Period(val title: String, val caption: String, val days: Int?) {
    Week("7 дней", "за 7 дней", 7),
    Month("30 дней", "за 30 дней", 30),
    Year("Год", "за год", 365),
    All("Всё время", "за всё время", null),
}

@Composable
fun HistoryScreen(
    data: WalletData,
    contentPadding: PaddingValues,
    onTransaction: (Transaction) -> Unit,
) {
    var cardFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var period by rememberSaveable { mutableStateOf(Period.Week) }
    var menu by remember { mutableStateOf(false) }

    val since = period.days?.let { Fmt.dayStart(System.currentTimeMillis()) - (it - 1) * 86_400_000L } ?: 0L
    val filtered = data.transactions.filter { (cardFilter == null || it.cardId == cardFilter) && it.timestamp >= since }
    val groups = filtered.groupBy { Fmt.dayStart(it.timestamp) }.toSortedMap(compareByDescending { it })

    LazyColumn(Modifier.fillMaxSize().padding(contentPadding), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("История", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.CalendarMonth, "Период") }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        Period.entries.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.title, color = if (p == period) Mpay.Lime else Mpay.TextPrimary) },
                                onClick = { period = p; menu = false }
                            )
                        }
                    }
                }
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { HistoryChip("Все", cardFilter == null) { cardFilter = null } }
                items(data.cards, key = { it.id }) { c ->
                    HistoryChip(c.shortName, cardFilter == c.id) { cardFilter = c.id }
                }
            }
        }
        item { SpentSummary(filtered, period) }
        if (groups.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.ReceiptLong, null, tint = Mpay.TextSecondary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Операций пока нет", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Оплатите покупку свайпом вверх на главной — она появится здесь",
                        color = Mpay.TextSecondary, textAlign = TextAlign.Center
                    )
                }
            }
        }
        groups.forEach { (day, list) ->
            item(key = "h$day") {
                Text(
                    Fmt.dayHeader(day), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp)
                )
            }
            list.forEachIndexed { i, t ->
                item(key = t.id) {
                    Column(Modifier.padding(horizontal = 4.dp)) {
                        TransactionRow(t, "${Fmt.shortDateTime(t.timestamp)} · ${cardFor(data, t)?.shortName ?: ""}") { onTransaction(t) }
                        if (i < list.lastIndex) HorizontalDivider(Modifier.padding(start = 80.dp, end = 16.dp), color = Mpay.Outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) },
        shape = RoundedCornerShape(50),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Mpay.Surface,
            labelColor = Mpay.TextPrimary,
            selectedContainerColor = Mpay.Lime,
            selectedLabelColor = Mpay.OnLime,
        ),
    )
}

@Composable
private fun SpentSummary(txs: List<Transaction>, period: Period) {
    val total = txs.sumOf { it.amount }
    val days = 5
    val today = Fmt.dayStart(System.currentTimeMillis())
    val mini = (days - 1 downTo 0).map { back ->
        val s = today - back * 86_400_000L
        txs.filter { it.timestamp in s until s + 86_400_000L }.sumOf { it.amount }
    }
    val max = (mini.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Mpay.Surface),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Потрачено", color = Mpay.TextSecondary)
                Text(Fmt.money(total), style = AmountStyle.copy(fontSize = AmountStyle.fontSize * 0.85f))
                Text("${period.caption} · ${txs.size} опер.", color = Mpay.TextSecondary)
            }
            Row(Modifier.height(64.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                mini.forEach { v ->
                    Box(
                        Modifier.width(14.dp).fillMaxHeight(((v / max).toFloat()).coerceAtLeast(0.08f))
                            .background(Brush.verticalGradient(listOf(Mpay.Lime, Mpay.Lime.copy(alpha = 0.3f))), RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSheet(t: Transaction, card: BankCard?, onDelete: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Mpay.Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MerchantAvatar(t.category, 64)
            Spacer(Modifier.height(12.dp))
            Text(t.merchant, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(Fmt.expense(t.amount), style = AmountStyle, color = Mpay.Expense)
            Spacer(Modifier.height(16.dp))
            val rows = listOf(
                "Дата" to Fmt.fullDateTime(t.timestamp),
                "Карта" to (card?.let { "${it.brand.title} ${it.masked}" } ?: "Удалена"),
                "Категория" to t.category.title,
                "Статус" to "Выполнено",
            )
            Card(colors = CardDefaults.cardColors(containerColor = Mpay.SurfaceHigh), shape = RoundedCornerShape(18.dp)) {
                rows.forEach { (k, v) ->
                    ListItem(
                        headlineContent = { Text(k, color = Mpay.TextSecondary) },
                        trailingContent = { Text(v, fontWeight = FontWeight.Medium, color = Mpay.TextPrimary) },
                        colors = ListItemDefaults.colors(containerColor = Mpay.SurfaceHigh),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Delete, null, tint = Mpay.Expense)
                Spacer(Modifier.width(8.dp))
                Text("Удалить из истории", color = Mpay.Expense)
            }
        }
    }
}

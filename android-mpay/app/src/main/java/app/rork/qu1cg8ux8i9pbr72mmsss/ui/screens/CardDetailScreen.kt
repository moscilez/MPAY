package app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.rork.qu1cg8ux8i9pbr72mmsss.data.BankCard
import app.rork.qu1cg8ux8i9pbr72mmsss.data.Transaction
import app.rork.qu1cg8ux8i9pbr72mmsss.data.WalletData
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.BankCardView
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.Fmt
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.TransactionRow
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.AmountStyle
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    card: BankCard,
    data: WalletData,
    onBack: () -> Unit,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onMakeDefault: () -> Unit,
    onDelete: () -> Unit,
    onTransaction: (Transaction) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val txs = data.transactions.filter { it.cardId == card.id }
    val isDefault = data.defaultCardId == card.id

    Scaffold(
        containerColor = Mpay.Canvas,
        topBar = {
            TopAppBar(
                title = { Text(card.label.ifBlank { card.brand.title }) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Mpay.Canvas),
            )
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                BankCardView(
                    card.brand, card.number, card.style, holder = card.holder, expiry = card.expiry,
                    label = card.label, showContactless = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(Icons.Rounded.Contactless, "Оплатить", primary = true, onClick = onPay, modifier = Modifier.weight(1f))
                    ActionTile(Icons.Rounded.Edit, "Изменить", onClick = onEdit, modifier = Modifier.weight(1f))
                    ActionTile(if (isDefault) Icons.Rounded.Star else Icons.Rounded.StarOutline, if (isDefault) "Основная" else "Основной", onClick = onMakeDefault, modifier = Modifier.weight(1f))
                    ActionTile(Icons.Rounded.Delete, "Удалить", onClick = { confirmDelete = true }, modifier = Modifier.weight(1f))
                }
            }
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Mpay.Surface),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Все расходы по карте", color = Mpay.TextSecondary)
                        Text(Fmt.money(txs.sumOf { it.amount }), style = AmountStyle.copy(fontSize = AmountStyle.fontSize * 0.8f))
                        Text("${txs.size} операций · ${card.brand.title}, срок ${card.expiry.take(2)}/${card.expiry.drop(2)}", color = Mpay.TextSecondary)
                    }
                }
            }
            item { SectionHeader("История по карте", null) }
            if (txs.isEmpty()) item {
                Text("По этой карте ещё не было оплат", color = Mpay.TextSecondary, modifier = Modifier.padding(horizontal = 20.dp))
            }
            items(txs, key = { it.id }) { t -> TransactionRow(t, Fmt.shortDateTime(t.timestamp)) { onTransaction(t) } }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить карту?") },
            text = { Text("Карта ${card.masked} и её история операций будут удалены с этого телефона.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Удалить", color = Mpay.Expense) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
            containerColor = Mpay.Surface,
        )
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (primary) Mpay.Lime else Mpay.Surface,
        contentColor = if (primary) Mpay.OnLime else Mpay.TextPrimary,
        modifier = modifier,
    ) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = if (primary) Mpay.OnLime else Mpay.Lime)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

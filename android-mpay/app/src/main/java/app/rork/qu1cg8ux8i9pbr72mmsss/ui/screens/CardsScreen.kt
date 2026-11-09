package app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rork.qu1cg8ux8i9pbr72mmsss.data.WalletData
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.BankCardView
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.Fmt
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay

@Composable
fun CardsScreen(
    data: WalletData,
    contentPadding: PaddingValues,
    onOpenCard: (String) -> Unit,
    onAddCard: () -> Unit,
    onPay: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
                    Text("Карты", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        if (data.cards.isEmpty()) "Добавьте карту любого банка" else "${data.cards.size} ${plural(data.cards.size)} на телефоне",
                        color = Mpay.TextSecondary
                    )
                }
            }
            if (data.cards.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(88.dp).background(Mpay.Surface, RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CreditCard, null, tint = Mpay.Lime, modifier = Modifier.size(44.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Здесь пока пусто", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Visa, Mastercard, Мир, UnionPay и другие — данные заполнятся автоматически",
                            color = Mpay.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                }
            }
            items(data.cards, key = { it.id }) { c ->
                val spent = data.transactions.filter { it.cardId == c.id }.sumOf { it.amount }
                Column {
                    BankCardView(
                        c.brand, c.number, c.style,
                        holder = c.holder, expiry = c.expiry, label = c.label,
                        modifier = Modifier.fillMaxWidth().clickable { onOpenCard(c.id) }
                    )
                    Row(Modifier.padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (data.defaultCardId == c.id) {
                            Icon(Icons.Rounded.Star, null, tint = Mpay.Lime, modifier = Modifier.size(16.dp))
                            Text("Основная · ", color = Mpay.Lime, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "${c.label.ifBlank { c.brand.title }} · потрачено ${Fmt.money(spent)}",
                            color = Mpay.TextSecondary, style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = onAddCard,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mpay.Surface, contentColor = Mpay.TextPrimary),
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Mpay.Lime)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить карту", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (data.cards.isNotEmpty()) {
            SwipeToPayHandle(enabled = true, onPay = onPay, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

fun plural(n: Int): String {
    val m10 = n % 10; val m100 = n % 100
    return when {
        m10 == 1 && m100 != 11 -> "карта"
        m10 in 2..4 && m100 !in 12..14 -> "карты"
        else -> "карт"
    }
}

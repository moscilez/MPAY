package app.rork.qu1cg8ux8i9pbr72mmsss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rork.qu1cg8ux8i9pbr72mmsss.data.Category
import app.rork.qu1cg8ux8i9pbr72mmsss.data.Transaction
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay

fun Category.icon(): ImageVector = when (this) {
    Category.Groceries -> Icons.Rounded.ShoppingBasket
    Category.Cafe -> Icons.Rounded.LocalCafe
    Category.Transport -> Icons.Rounded.DirectionsCar
    Category.Health -> Icons.Rounded.LocalHospital
    Category.Shopping -> Icons.Rounded.ShoppingBag
    Category.Other -> Icons.Rounded.Payments
}

fun Category.color(): Color = when (this) {
    Category.Groceries -> Color(0xFFE5383B)
    Category.Cafe -> Color(0xFF7A4B2E)
    Category.Transport -> Color(0xFFF2B705)
    Category.Health -> Color(0xFF2FA84F)
    Category.Shopping -> Color(0xFF2D6BFF)
    Category.Other -> Color(0xFF3A4150)
}

@Composable
fun MerchantAvatar(category: Category, size: Int = 46) {
    Box(
        Modifier.size(size.dp).background(category.color(), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(category.icon(), null, tint = Color.White, modifier = Modifier.size((size * 0.5f).dp))
    }
}

@Composable
fun TransactionRow(t: Transaction, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MerchantAvatar(t.category)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t.merchant, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Mpay.TextSecondary, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Text(Fmt.expense(t.amount), color = Mpay.Expense, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Mpay.TextSecondary, modifier = Modifier.padding(start = 4.dp))
    }
}

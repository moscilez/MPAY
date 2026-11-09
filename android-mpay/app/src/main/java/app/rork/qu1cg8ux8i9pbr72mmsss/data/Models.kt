package app.rork.qu1cg8ux8i9pbr72mmsss.data

import kotlinx.serialization.Serializable

@Serializable
data class BankCard(
    val id: String,
    val number: String,
    val expiry: String,
    val holder: String,
    val label: String,
    val brand: CardBrand,
    val style: Int,
    val createdAt: Long,
) {
    val last4: String get() = number.takeLast(4)
    val masked: String get() = "•• $last4"
    val shortName: String get() = "${brand.short} $last4"
}

@Serializable
data class Transaction(
    val id: String,
    val cardId: String,
    val merchant: String,
    val amount: Double,
    val timestamp: Long,
    val category: Category,
)

@Serializable
enum class Category(val title: String) {
    Groceries("Продукты"),
    Cafe("Кафе и рестораны"),
    Transport("Транспорт"),
    Health("Здоровье"),
    Shopping("Покупки"),
    Other("Другое"),
}

@Serializable
data class WalletData(
    val cards: List<BankCard> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val defaultCardId: String? = null,
)

package app.rork.qu1cg8ux8i9pbr72mmsss.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

class WalletViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SecureStore(app)
    val pin = PinManager(app)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _state = MutableStateFlow(WalletData())
    val state: StateFlow<WalletData> = _state.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _hasPin = MutableStateFlow(pin.hasPin)
    val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

    private val _biometrics = MutableStateFlow(pin.biometricsEnabled)
    val biometrics: StateFlow<Boolean> = _biometrics.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val data = store.read()?.let { runCatching { json.decodeFromString<WalletData>(it) }.getOrNull() }
            if (data != null) _state.value = data
            _loaded.value = true
        }
    }

    private fun mutate(block: (WalletData) -> WalletData) {
        _state.update(block)
        val snapshot = _state.value
        viewModelScope.launch(Dispatchers.IO) { store.write(json.encodeToString(WalletData.serializer(), snapshot)) }
    }

    fun card(id: String?): BankCard? = _state.value.cards.firstOrNull { it.id == id }

    fun saveCard(id: String?, number: String, expiry: String, holder: String, label: String, style: Int): String {
        val digits = CardUtils.digits(number)
        val brand = CardBrand.detect(digits)
        val newId = id ?: UUID.randomUUID().toString()
        mutate { d ->
            val existing = d.cards.firstOrNull { it.id == id }
            val card = BankCard(
                id = newId,
                number = digits,
                expiry = CardUtils.digits(expiry),
                holder = holder.trim().uppercase(),
                label = label.trim(),
                brand = brand,
                style = style,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            val cards = if (existing != null) d.cards.map { if (it.id == id) card else it } else d.cards + card
            d.copy(cards = cards, defaultCardId = d.defaultCardId ?: newId)
        }
        return newId
    }

    fun deleteCard(id: String) = mutate { d ->
        val cards = d.cards.filterNot { it.id == id }
        d.copy(
            cards = cards,
            transactions = d.transactions.filterNot { it.cardId == id },
            defaultCardId = if (d.defaultCardId == id) cards.firstOrNull()?.id else d.defaultCardId,
        )
    }

    fun setDefault(id: String) = mutate { it.copy(defaultCardId = id) }

    fun addTransaction(cardId: String, merchant: String, amount: Double, category: Category? = null) = mutate { d ->
        val t = Transaction(
            id = UUID.randomUUID().toString(),
            cardId = cardId,
            merchant = merchant.trim().ifEmpty { "Оплата" },
            amount = amount,
            timestamp = System.currentTimeMillis(),
            category = category ?: guessCategory(merchant),
        )
        d.copy(transactions = listOf(t) + d.transactions)
    }

    fun deleteTransaction(id: String) = mutate { d -> d.copy(transactions = d.transactions.filterNot { it.id == id }) }

    fun setPin(newPin: String) { pin.setPin(newPin); _hasPin.value = true }

    fun setBiometrics(enabled: Boolean) { pin.biometricsEnabled = enabled; _biometrics.value = enabled }

    companion object {
        fun guessCategory(merchant: String): Category {
            val m = merchant.lowercase()
            fun has(vararg k: String) = k.any { m.contains(it) }
            return when {
                has("пятёроч", "пятероч", "магнит", "лента", "перекр", "вкусвил", "ашан", "дикси", "spar", "продукт", "market", "маркет") -> Category.Groceries
                has("кофе", "coffee", "кафе", "ресторан", "бар", "пицц", "burger", "бургер", "шоколадниц", "вкусно") -> Category.Cafe
                has("такси", "taxi", " go", "метро", "транспорт", "азс", "лукойл", "газпром", "бензин", "uber", "автобус") -> Category.Transport
                has("аптек", "клиник", "36.6", "здоров", "ригла") -> Category.Health
                has("ozon", "озон", "wildberries", "вб", "зара", "магазин", "store", "shop", "одежд", "dns", "м.видео") -> Category.Shopping
                else -> Category.Other
            }
        }
    }
}

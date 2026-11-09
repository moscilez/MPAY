package app.rork.qu1cg8ux8i9pbr72mmsss.data

import kotlinx.serialization.Serializable

@Serializable
enum class CardBrand(val title: String, val short: String) {
    Visa("Visa", "Visa"),
    Mastercard("Mastercard", "MC"),
    Mir("Мир", "Mir"),
    Maestro("Maestro", "Maestro"),
    Amex("American Express", "Amex"),
    UnionPay("UnionPay", "UP"),
    Jcb("JCB", "JCB"),
    Discover("Discover", "Discover"),
    Unknown("Карта", "Карта");

    val numberLength: IntRange
        get() = when (this) {
            Amex -> 15..15
            Maestro -> 12..19
            UnionPay, Mir, Jcb, Discover -> 16..19
            Visa -> 13..19
            Mastercard -> 16..16
            Unknown -> 12..19
        }

    companion object {
        fun detect(raw: String): CardBrand {
            val n = raw.filter { it.isDigit() }
            if (n.isEmpty()) return Unknown
            fun prefix(len: Int): Int = n.take(len).toIntOrNull()?.takeIf { n.length >= len } ?: -1
            return when {
                prefix(4) in 2200..2204 -> Mir
                n.startsWith("4") -> Visa
                prefix(2) in 51..55 || prefix(4) in 2221..2720 -> Mastercard
                prefix(2) == 34 || prefix(2) == 37 -> Amex
                prefix(4) in 3528..3589 -> Jcb
                n.startsWith("6011") || n.startsWith("65") || prefix(3) in 644..649 -> Discover
                n.startsWith("62") -> UnionPay
                prefix(2) == 50 || prefix(2) in 56..69 -> Maestro
                else -> Unknown
            }
        }
    }
}

object CardUtils {
    fun digits(s: String) = s.filter { it.isDigit() }

    fun luhn(number: String): Boolean {
        val d = digits(number)
        if (d.length < 12) return false
        var sum = 0
        var alt = false
        for (i in d.length - 1 downTo 0) {
            var n = d[i] - '0'
            if (alt) { n *= 2; if (n > 9) n -= 9 }
            sum += n
            alt = !alt
        }
        return sum % 10 == 0
    }

    fun formatNumber(raw: String): String {
        val d = digits(raw)
        return if (CardBrand.detect(d) == CardBrand.Amex) {
            listOf(d.take(4), d.drop(4).take(6), d.drop(10)).filter { it.isNotEmpty() }.joinToString(" ")
        } else d.chunked(4).joinToString(" ")
    }

    fun maskedNumber(raw: String): String {
        val d = digits(raw)
        if (d.length < 8) return formatNumber(d)
        return "${d.take(4)} •••• •••• ${d.takeLast(4)}"
    }

    /** Accepts "MMYY" digits, returns true when the month is valid and not expired. */
    fun expiryValid(mmYY: String): Boolean {
        val d = digits(mmYY)
        if (d.length != 4) return false
        val m = d.take(2).toInt()
        val y = 2000 + d.drop(2).toInt()
        if (m !in 1..12) return false
        val cal = java.util.Calendar.getInstance()
        val cy = cal.get(java.util.Calendar.YEAR)
        val cm = cal.get(java.util.Calendar.MONTH) + 1
        return y > cy || (y == cy && m >= cm)
    }

    fun formatExpiry(mmYY: String): String {
        val d = digits(mmYY).take(4)
        return if (d.length > 2) "${d.take(2)}/${d.drop(2)}" else d
    }
}

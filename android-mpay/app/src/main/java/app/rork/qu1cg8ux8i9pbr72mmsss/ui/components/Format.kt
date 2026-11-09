package app.rork.qu1cg8ux8i9pbr72mmsss.ui.components

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale

object Fmt {
    val ru: Locale = Locale.forLanguageTag("ru-RU")
    private val symbols = DecimalFormatSymbols(ru).apply { groupingSeparator = '\u00A0'; decimalSeparator = ',' }
    private val whole = DecimalFormat("#,##0", symbols)
    private val frac = DecimalFormat("#,##0.00", symbols)

    fun money(v: Double): String {
        val s = if (v % 1.0 == 0.0) whole.format(v) else frac.format(v)
        return "$s\u00A0₽"
    }

    fun expense(v: Double) = "−" + money(v)

    fun compact(v: Double): String = when {
        v >= 1_000_000 -> "${(v / 1_000_000).toInt()}M"
        v >= 1000 -> "${(v / 1000).toInt()}K"
        else -> v.toInt().toString()
    }

    private val monthsGen = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    private val monthsShort = listOf("янв.", "февр.", "мар.", "апр.", "мая", "июн.", "июл.", "авг.", "сент.", "окт.", "нояб.", "дек.")
    val monthsPrep = listOf("январе", "феврале", "марте", "апреле", "мае", "июне", "июле", "августе", "сентябре", "октябре", "ноябре", "декабре")
    val monthsDat = listOf("январю", "февралю", "марту", "апрелю", "маю", "июню", "июлю", "августу", "сентябрю", "октябрю", "ноябрю", "декабрю")

    fun dayStart(ts: Long): Long = Calendar.getInstance().apply {
        timeInMillis = ts; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun relDay(ts: Long): Int {
        val today = dayStart(System.currentTimeMillis())
        val d = dayStart(ts)
        return ((today - d) / 86_400_000L).toInt()
    }

    fun time(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    fun shortDateTime(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        val prefix = when (relDay(ts)) {
            0 -> "Сегодня"
            1 -> "Вчера"
            else -> "${c.get(Calendar.DAY_OF_MONTH)} ${monthsShort[c.get(Calendar.MONTH)]}"
        }
        return "$prefix, ${time(ts)}"
    }

    fun dayHeader(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        val now = Calendar.getInstance()
        return when (relDay(ts)) {
            0 -> "Сегодня"
            1 -> "Вчера"
            else -> buildString {
                append("${c.get(Calendar.DAY_OF_MONTH)} ${monthsGen[c.get(Calendar.MONTH)]}")
                if (c.get(Calendar.YEAR) != now.get(Calendar.YEAR)) append(" ${c.get(Calendar.YEAR)}")
            }
        }
    }

    fun fullDateTime(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return "${c.get(Calendar.DAY_OF_MONTH)} ${monthsGen[c.get(Calendar.MONTH)]} ${c.get(Calendar.YEAR)}, ${time(ts)}"
    }

    fun parseAmount(s: String): Double? = s.replace(',', '.').replace(" ", "").replace("\u00A0", "").toDoubleOrNull()
}

package app.rork.qu1cg8ux8i9pbr72mmsss.scan

import app.rork.qu1cg8ux8i9pbr72mmsss.data.CardUtils

data class ScannedCard(
    val number: String? = null,
    val expiry: String? = null, // MMYY digits
    val holder: String? = null,
) {
    fun merge(other: ScannedCard) = ScannedCard(
        number = other.number ?: number,
        expiry = other.expiry ?: expiry,
        holder = other.holder ?: holder,
    )
}

object CardTextParser {
    private val numberRegex = Regex("(?<!\\d)(\\d[\\d ]{11,22}\\d)(?!\\d)")
    private val expiryRegex = Regex("(?<!\\d)(0[1-9]|1[0-2])\\s?[/\\\\-]\\s?(\\d{2})(?!\\d)")
    private val stopWords = setOf(
        "VISA", "MASTERCARD", "MAESTRO", "MIR", "BANK", "VALID", "THRU", "FROM", "MONTH", "YEAR",
        "DEBIT", "CREDIT", "CARD", "PLATINUM", "GOLD", "CLASSIC", "WORLD", "ELITE", "SIGNATURE",
        "ELECTRON", "BUSINESS", "INFINITE", "PREMIUM", "MOMENTUM", "UNIONPAY", "EXPRESS", "AMERICAN",
        "SBERBANK", "TINKOFF", "ALFA", "VTB", "GOOD", "MEMBER", "SINCE", "EXPIRES", "END", "BLACK",
        "DIGITAL", "VIRTUAL", "PAY", "CONTACTLESS", "INTERNATIONAL", "PREPAID", "STANDARD",
    )

    fun parse(lines: List<String>): ScannedCard {
        var number: String? = null
        val all = lines.joinToString("\n")
        for (m in numberRegex.findAll(all.replace('O', '0').replace('o', '0'))) {
            val d = CardUtils.digits(m.value)
            if (d.length in 13..19 && CardUtils.luhn(d)) { number = d; break }
        }
        var expiry: String? = null
        val expMatches = expiryRegex.findAll(all).toList()
        // Cards with "valid from" + "thru" show two dates: the later one is expiry.
        expMatches.mapNotNull { m ->
            val mm = m.groupValues[1]; val yy = m.groupValues[2]
            (mm + yy).takeIf { CardUtils.expiryValid(it) }
        }.maxByOrNull { it.drop(2) + it.take(2) }?.let { expiry = it }

        var holder: String? = null
        for (line in lines) {
            val t = line.trim()
            if (t.length !in 5..26) continue
            if (!t.matches(Regex("^[A-Z][A-Z.'\\- ]+$"))) continue
            val words = t.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size !in 2..3) continue
            if (words.any { it.uppercase() in stopWords || it.length < 2 }) continue
            holder = words.joinToString(" "); break
        }
        return ScannedCard(number, expiry, holder)
    }
}

/** Minimal BER-TLV helpers for reading EMV contactless cards. */
object Tlv {
    fun find(data: ByteArray, tag: Int): ByteArray? {
        var i = 0
        while (i < data.size) {
            val start = i
            var t = data[i].toInt() and 0xFF
            if (t == 0x00 || t == 0xFF) { i++; continue }
            val constructed = (t and 0x20) != 0
            i++
            if ((t and 0x1F) == 0x1F) {
                do {
                    if (i >= data.size) return null
                    t = (t shl 8) or (data[i].toInt() and 0xFF)
                    i++
                } while ((data[i - 1].toInt() and 0x80) != 0)
            }
            if (i >= data.size) return null
            var len = data[i].toInt() and 0xFF
            i++
            if (len and 0x80 != 0) {
                val n = len and 0x7F
                len = 0
                repeat(n) { if (i < data.size) { len = (len shl 8) or (data[i].toInt() and 0xFF); i++ } }
            }
            if (i + len > data.size || len < 0) return null
            val value = data.copyOfRange(i, i + len)
            if (t == tag) return value
            if (constructed) find(value, tag)?.let { return it }
            i += len
            if (i <= start) return null
        }
        return null
    }

    fun findAll(data: ByteArray, tag: Int): List<ByteArray> {
        val out = mutableListOf<ByteArray>()
        collect(data, tag, out, 0)
        return out
    }

    private fun collect(data: ByteArray, tag: Int, out: MutableList<ByteArray>, depth: Int) {
        if (depth > 8) return
        var i = 0
        while (i < data.size) {
            var t = data[i].toInt() and 0xFF
            if (t == 0x00 || t == 0xFF) { i++; continue }
            val constructed = (t and 0x20) != 0
            i++
            if ((t and 0x1F) == 0x1F) {
                do {
                    if (i >= data.size) return
                    t = (t shl 8) or (data[i].toInt() and 0xFF)
                    i++
                } while ((data[i - 1].toInt() and 0x80) != 0)
            }
            if (i >= data.size) return
            var len = data[i].toInt() and 0xFF
            i++
            if (len and 0x80 != 0) {
                val n = len and 0x7F
                len = 0
                repeat(n) { if (i < data.size) { len = (len shl 8) or (data[i].toInt() and 0xFF); i++ } }
            }
            if (i + len > data.size) return
            val value = data.copyOfRange(i, i + len)
            if (t == tag) out += value
            if (constructed) collect(value, tag, out, depth + 1)
            i += len
        }
    }

    /** Parses a DOL (tag list with lengths) into pairs. */
    fun parseDol(dol: ByteArray): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        var i = 0
        while (i < dol.size) {
            var t = dol[i].toInt() and 0xFF
            i++
            if ((t and 0x1F) == 0x1F) {
                do {
                    if (i >= dol.size) return out
                    t = (t shl 8) or (dol[i].toInt() and 0xFF)
                    i++
                } while ((dol[i - 1].toInt() and 0x80) != 0)
            }
            if (i >= dol.size) return out
            out += t to (dol[i].toInt() and 0xFF)
            i++
        }
        return out
    }
}

fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

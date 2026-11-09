package app.rork.qu1cg8ux8i9pbr72mmsss.scan

import android.nfc.tech.IsoDep
import app.rork.qu1cg8ux8i9pbr72mmsss.data.CardUtils
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reads the public data (card number, expiry, sometimes holder name) of a contactless
 * EMV bank card held against the phone. No secrets or cryptograms are stored.
 */
object NfcCardReader {
    private val knownAids = listOf(
        "A0000000031010", "A0000000041010", "A0000006581010", "A0000000043060",
        "A0000000032010", "A0000003330101", "A0000000651010", "A00000002501", "A0000001523010",
    )

    fun read(iso: IsoDep): ScannedCard? {
        iso.connect()
        iso.timeout = 5000
        try {
            val aids = mutableListOf<ByteArray>()
            val ppse = transceive(iso, select("2PAY.SYS.DDF01".toByteArray()))
            if (ppse != null) {
                aids += Tlv.findAll(ppse, 0x4F)
            }
            if (aids.isEmpty()) aids += knownAids.map { hex(it) }

            for (aid in aids) {
                val sel = transceive(iso, select(aid)) ?: continue
                val result = readApplication(iso, sel)
                if (result?.number != null) return result
            }
            return null
        } finally {
            runCatching { iso.close() }
        }
    }

    private fun readApplication(iso: IsoDep, selectResp: ByteArray): ScannedCard? {
        val pdol = Tlv.find(selectResp, 0x9F38)
        val pdolData = pdol?.let { buildPdol(it) } ?: byteArrayOf()
        val gpoBody = byteArrayOf(0x83.toByte(), pdolData.size.toByte()) + pdolData
        val gpo = byteArrayOf(0x80.toByte(), 0xA8.toByte(), 0, 0, gpoBody.size.toByte()) + gpoBody + byteArrayOf(0)
        val gpoResp = transceive(iso, gpo) ?: return null

        var card = extract(gpoResp)
        if (card.number != null && card.expiry != null) return card

        val afl: ByteArray? = Tlv.find(gpoResp, 0x94) ?: run {
            // Format 1 response: tag 80 = AIP(2) + AFL
            Tlv.find(gpoResp, 0x80)?.let { if (it.size > 2) it.copyOfRange(2, it.size) else null }
        }
        if (afl != null) {
            var i = 0
            while (i + 3 < afl.size) {
                val sfi = (afl[i].toInt() and 0xFF) shr 3
                val first = afl[i + 1].toInt() and 0xFF
                val last = afl[i + 2].toInt() and 0xFF
                for (rec in first..last) {
                    val cmd = byteArrayOf(0x00, 0xB2.toByte(), rec.toByte(), ((sfi shl 3) or 4).toByte(), 0x00)
                    val r = transceive(iso, cmd) ?: continue
                    card = card.merge(extract(r))
                    if (card.number != null && card.expiry != null && card.holder != null) return card
                }
                i += 4
            }
        }
        return card.takeIf { it.number != null }
    }

    private fun extract(data: ByteArray): ScannedCard {
        var number: String? = null
        var expiry: String? = null
        var holder: String? = null
        Tlv.find(data, 0x5A)?.let { number = it.toHex().trimEnd('F') }
        Tlv.find(data, 0x5F24)?.let { val h = it.toHex(); if (h.length >= 4) expiry = h.substring(2, 4) + h.substring(0, 2) }
        if (number == null || expiry == null) {
            (Tlv.find(data, 0x57) ?: Tlv.find(data, 0x9F6B))?.let { t2 ->
                val h = t2.toHex()
                val d = h.indexOf('D')
                if (d > 0) {
                    if (number == null) number = h.substring(0, d)
                    if (expiry == null && h.length >= d + 5) {
                        val yymm = h.substring(d + 1, d + 5)
                        expiry = yymm.substring(2, 4) + yymm.substring(0, 2)
                    }
                }
            }
        }
        Tlv.find(data, 0x5F20)?.let { raw ->
            val name = String(raw, Charsets.US_ASCII).trim().replace('/', ' ').replace(Regex("\\s+"), " ")
            if (name.length >= 3 && name.any { it.isLetter() }) holder = name
        }
        val validNumber = number?.takeIf { CardUtils.luhn(it) }
        return ScannedCard(validNumber, expiry, holder)
    }

    private fun buildPdol(pdol: ByteArray): ByteArray {
        val out = mutableListOf<Byte>()
        val date = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        for ((tag, len) in Tlv.parseDol(pdol)) {
            val value: ByteArray = when (tag) {
                0x9F66 -> hex("F620C000")
                0x9F02 -> hex("000000000100")
                0x9F03 -> ByteArray(6)
                0x9F1A -> hex("0643")
                0x5F2A -> hex("0643")
                0x95 -> ByteArray(5)
                0x9A -> hex(date)
                0x9C -> byteArrayOf(0)
                0x9F37 -> ByteArray(4).also { SecureRandom().nextBytes(it) }
                0x9F35 -> byteArrayOf(0x22)
                0x9F45 -> ByteArray(2)
                0x9F4C -> ByteArray(8)
                0x9F34 -> ByteArray(3)
                0x9F21 -> ByteArray(3)
                0x9F7C -> ByteArray(20)
                else -> ByteArray(len)
            }
            val fitted = when {
                value.size == len -> value
                value.size > len -> value.copyOfRange(value.size - len, value.size)
                else -> ByteArray(len - value.size) + value
            }
            out += fitted.toList()
        }
        return out.toByteArray()
    }

    private fun select(aid: ByteArray) =
        byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid + byteArrayOf(0x00)

    private fun transceive(iso: IsoDep, cmd: ByteArray): ByteArray? {
        var resp = runCatching { iso.transceive(cmd) }.getOrNull() ?: return null
        if (resp.size < 2) return null
        var sw1 = resp[resp.size - 2].toInt() and 0xFF
        var sw2 = resp[resp.size - 1].toInt() and 0xFF
        if (sw1 == 0x6C) {
            val retry = cmd.copyOf().also { it[it.size - 1] = sw2.toByte() }
            resp = runCatching { iso.transceive(retry) }.getOrNull() ?: return null
            sw1 = resp[resp.size - 2].toInt() and 0xFF
            sw2 = resp[resp.size - 1].toInt() and 0xFF
        }
        if (sw1 == 0x61) {
            resp = runCatching { iso.transceive(byteArrayOf(0x00, 0xC0.toByte(), 0, 0, sw2.toByte())) }.getOrNull() ?: return null
            sw1 = resp[resp.size - 2].toInt() and 0xFF
        }
        return if (sw1 == 0x90) resp.copyOfRange(0, resp.size - 2) else null
    }

    private fun hex(s: String): ByteArray = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

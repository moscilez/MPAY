package app.rork.qu1cg8ux8i9pbr72mmsss.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rork.qu1cg8ux8i9pbr72mmsss.data.CardBrand
import app.rork.qu1cg8ux8i9pbr72mmsss.data.CardUtils
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay

data class CardStyle(val name: String, val top: Color, val bottom: Color, val border: Color)

val CardStyles = listOf(
    CardStyle("Графит", Color(0xFF3A3F4B), Color(0xFF1A1D24), Color(0x55FFFFFF)),
    CardStyle("Кобальт", Color(0xFF1E4BFF), Color(0xFF0A1F8F), Color(0x33FFFFFF)),
    CardStyle("Изумруд", Color(0xFF0E6B52), Color(0xFF07302A), Color(0x33FFFFFF)),
    CardStyle("Медь", Color(0xFFB0562F), Color(0xFF4A1E10), Color(0x33FFFFFF)),
    CardStyle("Лайм", Color(0xFF6E8F12), Color(0xFF1F2A05), Color(0x44C6FF3D)),
    CardStyle("Рубин", Color(0xFFB0213A), Color(0xFF420914), Color(0x33FFFFFF)),
)

fun defaultStyleFor(brand: CardBrand): Int = when (brand) {
    CardBrand.Visa -> 0
    CardBrand.Mir -> 1
    CardBrand.Mastercard, CardBrand.Maestro -> 2
    CardBrand.UnionPay -> 5
    CardBrand.Amex -> 1
    else -> 3
}

@Composable
fun BrandMark(brand: CardBrand, height: Dp = 22.dp, modifier: Modifier = Modifier) {
    val fs: TextUnit = (height.value * 0.95f).sp
    when (brand) {
        CardBrand.Mastercard, CardBrand.Maestro -> Canvas(modifier.size(width = height * 1.6f, height = height)) {
            val r = size.height / 2f
            drawCircle(Color(0xFFEB001B), r, Offset(r, r))
            drawCircle(if (brand == CardBrand.Maestro) Color(0xFF00A2E5) else Color(0xFFF79E1B), r, Offset(size.width - r, r), alpha = 0.9f)
        }
        CardBrand.Visa -> Text("VISA", modifier, color = Color.White, style = TextStyle(fontSize = fs, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = 0.5.sp))
        CardBrand.Mir -> Text("МИР", modifier, color = Color.White, style = TextStyle(fontSize = fs, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic))
        CardBrand.Amex -> Text("AMEX", modifier, color = Color.White, style = TextStyle(fontSize = fs * 0.85f, fontWeight = FontWeight.Black))
        CardBrand.Unknown -> Text("CARD", modifier, color = Color.White.copy(alpha = 0.8f), style = TextStyle(fontSize = fs * 0.75f, fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
        else -> Text(brand.title.uppercase(), modifier, color = Color.White, style = TextStyle(fontSize = fs * 0.8f, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic))
    }
}

/** A credit-card-proportioned surface. Pass [compact] for the small home carousel tiles. */
@Composable
fun BankCardView(
    brand: CardBrand,
    number: String,
    style: Int,
    modifier: Modifier = Modifier,
    holder: String = "",
    expiry: String = "",
    label: String = "",
    compact: Boolean = false,
    showContactless: Boolean = false,
) {
    val s = CardStyles[style.coerceIn(0, CardStyles.lastIndex)]
    val shape = RoundedCornerShape(if (compact) 18.dp else 22.dp)
    val digits = CardUtils.digits(number)
    Box(
        modifier
            .aspectRatio(if (compact) 1.5f else 1.586f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(s.top, s.bottom), start = Offset.Zero, end = Offset(900f, 700f)))
            .border(1.dp, s.border, shape)
    ) {
        // Diagonal sheen
        Canvas(Modifier.fillMaxSize()) {
            val p = Path().apply {
                moveTo(size.width * 0.45f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height * 0.55f)
                close()
            }
            drawPath(p, Color.White.copy(alpha = 0.06f))
            val p2 = Path().apply {
                moveTo(size.width * 0.7f, size.height)
                lineTo(size.width, size.height * 0.62f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(p2, Color.Black.copy(alpha = 0.12f))
        }
        Column(Modifier.fillMaxSize().padding(if (compact) 14.dp else 22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrandMark(brand, height = if (compact) 18.dp else 24.dp)
                Spacer(Modifier.weight(1f))
                if (showContactless) Icon(Icons.Rounded.Contactless, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(26.dp))
                else if (!compact && label.isNotBlank()) Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            if (!compact) {
                Text(
                    if (digits.isEmpty()) "•••• •••• •••• ••••" else if (digits.length < 12) CardUtils.formatNumber(digits) else CardUtils.maskedNumber(digits),
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 17.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(10.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    if (!compact) {
                        Text(holder.ifBlank { " " }, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, maxLines = 1, letterSpacing = 1.sp)
                    } else Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(letterSpacing = 1.sp)) { append("•• ") }
                            append(if (digits.length >= 4) digits.takeLast(4) else "····")
                        },
                        color = Color.White,
                        fontSize = if (compact) 15.sp else 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (!compact && expiry.length == 4) {
                    Text(CardUtils.formatExpiry(expiry), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.padding(end = 12.dp, bottom = 2.dp))
                }
                if (!compact || brand == CardBrand.Visa || brand == CardBrand.Mir) {
                    BrandMark(brand, height = if (compact) 14.dp else 20.dp)
                }
            }
        }
    }
}

@Composable
fun MpayLogo(fontSize: TextUnit = 30.sp, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White)) { append("M") }
            withStyle(SpanStyle(color = Mpay.Lime)) { append("PAY") }
        },
        modifier = modifier,
        style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    )
}

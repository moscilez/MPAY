package app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rork.qu1cg8ux8i9pbr72mmsss.data.CardBrand
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.BankCardView
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.MpayLogo
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.PIN_LENGTH
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.PinDots
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.PinPad
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay

@Composable
fun OnboardingScreen(onPinCreated: (String) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "onb") { s ->
        when (s) {
            0 -> WelcomeStep { step = 1 }
            else -> PinCreateFlow(title = "Придумайте PIN-код", subtitle = "Он нужен для подтверждения оплаты", onDone = onPinCreated)
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Mpay.Canvas).safeDrawingPadding().padding(24.dp),
    ) {
        MpayLogo(34.sp)
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
            BankCardView(CardBrand.Mir, "2200000000001107", 1, compact = true, modifier = Modifier.width(220.dp).offset(x = (-56).dp, y = (-30).dp).rotate(-10f))
            BankCardView(CardBrand.Mastercard, "5500000000009034", 2, compact = true, modifier = Modifier.width(220.dp).offset(x = 56.dp, y = (-10).dp).rotate(8f))
            BankCardView(CardBrand.Visa, "4276000000004821", 0, compact = true, showContactless = true, modifier = Modifier.width(240.dp).offset(y = 30.dp))
        }
        Spacer(Modifier.weight(1f))
        Text("Оплата телефоном\nв одно касание", style = MaterialTheme.typography.headlineLarge, color = Mpay.TextPrimary)
        Spacer(Modifier.height(20.dp))
        Feature(Icons.Rounded.PhoneAndroid, "Всё хранится только на этом телефоне")
        Feature(Icons.Rounded.Lock, "Без регистрации и серверов")
        Feature(Icons.Rounded.Fingerprint, "Оплата по отпечатку или PIN-коду")
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mpay.Lime, contentColor = Mpay.OnLime),
        ) { Text("Начать", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun Feature(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(Mpay.Surface, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Mpay.Lime, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Mpay.TextPrimary)
    }
}

/** Two-step create + confirm PIN flow, reused by onboarding and settings. */
@Composable
fun PinCreateFlow(title: String, subtitle: String, onDone: (String) -> Unit, modifier: Modifier = Modifier) {
    var first by remember { mutableStateOf<String?>(null) }
    var entry by remember { mutableStateOf("") }
    var error by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier.fillMaxSize().background(Mpay.Canvas).safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))
        Text(if (first == null) title else "Повторите PIN-код", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = Mpay.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(message ?: subtitle, color = if (message != null) Mpay.Expense else Mpay.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        PinDots(entry.length, error)
        Spacer(Modifier.weight(1f))
        PinPad(
            onDigit = { d ->
                if (entry.length < PIN_LENGTH) {
                    entry += d
                    message = null
                    if (entry.length == PIN_LENGTH) {
                        val f = first
                        if (f == null) { first = entry; entry = "" }
                        else if (f == entry) onDone(entry)
                        else { error++; entry = ""; first = null; message = "PIN-коды не совпали, попробуйте снова" }
                    }
                }
            },
            onDelete = { entry = entry.dropLast(1) },
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** Verifies the existing PIN. */
@Composable
fun PinVerifyPanel(
    title: String,
    verify: (String) -> Boolean,
    onSuccess: () -> Unit,
    onBiometric: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var entry by remember { mutableStateOf("") }
    var error by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableStateOf(false) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(if (wrong) "Неверный PIN-код" else "Введите 4 цифры", color = if (wrong) Mpay.Expense else Mpay.TextSecondary)
        Spacer(Modifier.height(20.dp))
        PinDots(entry.length, error)
        Spacer(Modifier.height(28.dp))
        PinPad(
            onDigit = { d ->
                if (entry.length < PIN_LENGTH) {
                    entry += d; wrong = false
                    if (entry.length == PIN_LENGTH) {
                        if (verify(entry)) onSuccess() else { error++; wrong = true; entry = "" }
                    }
                }
            },
            onDelete = { entry = entry.dropLast(1) },
            onBiometric = onBiometric,
        )
    }
}

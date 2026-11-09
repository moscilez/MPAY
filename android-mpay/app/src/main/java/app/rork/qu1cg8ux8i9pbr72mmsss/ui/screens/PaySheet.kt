package app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rork.qu1cg8ux8i9pbr72mmsss.data.WalletData
import app.rork.qu1cg8ux8i9pbr72mmsss.data.WalletViewModel
import app.rork.qu1cg8ux8i9pbr72mmsss.security.Biometric
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.BankCardView
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.Fmt
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.MpayLogo
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private enum class PayPhase { Auth, Pin, Ready, Record, Done }

private const val READY_SECONDS = 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaySheet(
    data: WalletData,
    vm: WalletViewModel,
    initialCardId: String?,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val cards = data.cards
    if (cards.isEmpty()) { onDismiss(); return }

    val startIndex = cards.indexOfFirst { it.id == (initialCardId ?: data.defaultCardId) }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = startIndex) { cards.size }
    val current = cards[pager.currentPage.coerceIn(0, cards.lastIndex)]

    var phase by remember { mutableStateOf(PayPhase.Auth) }
    var secondsLeft by remember { mutableIntStateOf(READY_SECONDS) }
    val bioAllowed = vm.pin.biometricsEnabled && Biometric.available(context)

    fun close() = scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
    fun authorized() { haptics.performHapticFeedback(HapticFeedbackType.LongPress); secondsLeft = READY_SECONDS; phase = PayPhase.Ready }
    fun askBio() = Biometric.prompt(
        context, "Подтвердите оплату", "${current.brand.title} ${current.masked}",
        onSuccess = { authorized() },
        onFallback = { if (phase == PayPhase.Auth) phase = PayPhase.Pin },
    )

    LaunchedEffect(Unit) {
        delay(350)
        if (bioAllowed) askBio() else phase = PayPhase.Pin
    }
    LaunchedEffect(phase) {
        if (phase == PayPhase.Ready) {
            while (secondsLeft > 0 && phase == PayPhase.Ready) { delay(1000); secondsLeft-- }
            if (phase == PayPhase.Ready) phase = PayPhase.Auth
        }
        if (phase == PayPhase.Done) { delay(1400); close() }
    }
    DisposableEffect(phase) {
        view.keepScreenOn = phase == PayPhase.Ready
        onDispose { view.keepScreenOn = false }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = Mpay.Surface,
        dragHandle = { Box(Modifier.padding(top = 12.dp).size(40.dp, 4.dp).background(Mpay.TextSecondary.copy(alpha = 0.5f), RoundedCornerShape(2.dp))) },
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MpayLogo(24.sp, Modifier.padding(top = 8.dp, bottom = 16.dp))
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = 44.dp),
                pageSpacing = 12.dp,
                userScrollEnabled = phase != PayPhase.Record && phase != PayPhase.Done,
            ) { page ->
                val c = cards[page]
                val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                BankCardView(
                    c.brand, c.number, c.style, label = c.label, compact = false, showContactless = true,
                    modifier = Modifier.fillMaxWidth().graphicsLayer {
                        val s = 1f - 0.12f * offset
                        scaleX = s; scaleY = s; alpha = 1f - 0.45f * offset
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                buildString {
                    append(current.label.ifBlank { current.brand.title })
                    if (current.id == data.defaultCardId) append(" · основная")
                },
                color = Mpay.TextPrimary, style = MaterialTheme.typography.bodyLarge
            )
            if (cards.size > 1) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(cards.size) { i ->
                    Box(Modifier.size(if (i == pager.currentPage) 8.dp else 6.dp).background(if (i == pager.currentPage) Mpay.Lime else Mpay.TextSecondary.copy(alpha = 0.5f), CircleShape))
                }
            }
            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                phase,
                transitionSpec = { (fadeIn(tween(250)) + slideInVertically { it / 8 }) togetherWith fadeOut(tween(150)) },
                label = "phase"
            ) { p ->
                when (p) {
                    PayPhase.Auth -> AuthPanel(
                        bioAllowed = bioAllowed,
                        onBio = { askBio() },
                        onPin = { phase = PayPhase.Pin },
                    )
                    PayPhase.Pin -> PinVerifyPanel(
                        title = "Введите PIN-код",
                        verify = { vm.pin.verify(it) },
                        onSuccess = { authorized() },
                        onBiometric = if (bioAllowed) ({ askBio() }) else null,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    PayPhase.Ready -> ReadyPanel(
                        secondsLeft = secondsLeft,
                        onRecord = { phase = PayPhase.Record },
                        onCancel = { close() },
                    )
                    PayPhase.Record -> RecordPanel(
                        onSave = { merchant, amount ->
                            vm.addTransaction(current.id, merchant, amount)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            phase = PayPhase.Done
                        },
                        onBack = { phase = PayPhase.Ready },
                    )
                    PayPhase.Done -> DonePanel()
                }
            }
        }
    }
}

@Composable
private fun NfcWaves(active: Boolean) {
    val t = rememberInfiniteTransition(label = "waves")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "p")
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            if (!active) return@Canvas
            val center = Offset(size.width / 2, size.height / 2)
            for (i in 0 until 3) {
                val f = (p + i / 3f) % 1f
                val r = 50.dp.toPx() + f * 70.dp.toPx()
                val a = (1f - f) * 0.9f
                listOf(-40f, 140f).forEach { start ->
                    drawArc(
                        Mpay.Lime.copy(alpha = a), startAngle = start, sweepAngle = 80f, useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2),
                        style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
            }
        }
        Box(Modifier.size(92.dp).background(Mpay.SurfaceHigh, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Contactless, null, tint = if (active) Mpay.TextPrimary else Mpay.TextSecondary, modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun AuthPanel(bioAllowed: Boolean, onBio: () -> Unit, onPin: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        NfcWaves(active = false)
        Text("Подтвердите оплату", style = MaterialTheme.typography.titleMedium)
        HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Mpay.Outline)
        if (bioAllowed) {
            Surface(onClick = onBio, shape = CircleShape, color = Mpay.SurfaceHigh, modifier = Modifier.size(76.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Fingerprint, "Отпечаток", tint = Mpay.Lime, modifier = Modifier.size(44.dp)) }
            }
            Spacer(Modifier.height(10.dp))
            Text("Приложите палец для подтверждения", color = Mpay.TextSecondary)
            Spacer(Modifier.height(12.dp))
        } else {
            Icon(Icons.Rounded.Lock, null, tint = Mpay.TextSecondary)
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onPin) { Text("Ввести PIN-код", color = Mpay.Lime) }
    }
}

@Composable
private fun ReadyPanel(secondsLeft: Int, onRecord: () -> Unit, onCancel: () -> Unit) {
    val pulse = remember { Animatable(0.9f) }
    LaunchedEffect(Unit) { pulse.animateTo(1f, spring(dampingRatio = 0.4f)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }) { NfcWaves(active = true) }
        Text("Поднесите к терминалу", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text("Карта активна · 0:%02d".format(secondsLeft), color = Mpay.Lime, fontWeight = FontWeight.Medium)
        HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Mpay.Outline)
        Button(
            onClick = onRecord,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mpay.Lime, contentColor = Mpay.OnLime),
        ) { Text("Записать покупку", style = MaterialTheme.typography.titleMedium) }
        TextButton(onClick = onCancel) { Text("Отмена", color = Mpay.TextSecondary) }
    }
}

@Composable
private fun RecordPanel(onSave: (String, Double) -> Unit, onBack: () -> Unit) {
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val parsed = Fmt.parseAmount(amount)
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Mpay.Lime, unfocusedBorderColor = Mpay.Outline, focusedLabelColor = Mpay.Lime, cursorColor = Mpay.Lime,
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Покупка", style = MaterialTheme.typography.titleLarge)
        Text("Сохраним её в истории этой карты", color = Mpay.TextSecondary)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(10) },
            label = { Text("Сумма, ₽") }, singleLine = true, colors = colors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Start),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = merchant, onValueChange = { merchant = it.take(40) },
            label = { Text("Где оплатили") }, placeholder = { Text("Например, Пятёрочка") }, singleLine = true, colors = colors,
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { parsed?.let { onSave(merchant, it) } },
            enabled = parsed != null && parsed > 0,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mpay.Lime, contentColor = Mpay.OnLime),
        ) { Text("Сохранить", style = MaterialTheme.typography.titleMedium) }
        TextButton(onClick = onBack) { Text("Назад", color = Mpay.TextSecondary) }
    }
}

@Composable
private fun DonePanel() {
    val s = remember { Animatable(0f) }
    LaunchedEffect(Unit) { s.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(96.dp).graphicsLayer { scaleX = s.value; scaleY = s.value }.background(Mpay.Lime, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Check, null, tint = Mpay.OnLime, modifier = Modifier.size(56.dp)) }
        Spacer(Modifier.height(16.dp))
        Text("Готово", style = MaterialTheme.typography.headlineMedium)
        Text("Покупка сохранена в истории", color = Mpay.TextSecondary)
        Spacer(Modifier.width(1.dp))
    }
}

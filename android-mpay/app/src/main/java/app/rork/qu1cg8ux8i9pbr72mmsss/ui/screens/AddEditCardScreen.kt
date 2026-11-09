package app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.rork.qu1cg8ux8i9pbr72mmsss.data.BankCard
import app.rork.qu1cg8ux8i9pbr72mmsss.data.CardBrand
import app.rork.qu1cg8ux8i9pbr72mmsss.data.CardUtils
import app.rork.qu1cg8ux8i9pbr72mmsss.scan.CameraCardScanner
import app.rork.qu1cg8ux8i9pbr72mmsss.scan.NfcCardListener
import app.rork.qu1cg8ux8i9pbr72mmsss.scan.NfcStatus
import app.rork.qu1cg8ux8i9pbr72mmsss.scan.ScannedCard
import app.rork.qu1cg8ux8i9pbr72mmsss.scan.nfcStatus
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.BankCardView
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.CardStyles
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.components.defaultStyleFor
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush

private enum class TopMode { Camera, Preview }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    existing: BankCard?,
    onBack: () -> Unit,
    onSave: (number: String, expiry: String, holder: String, label: String, style: Int) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var number by remember { mutableStateOf(TextFieldValue(existing?.let { CardUtils.formatNumber(it.number) } ?: "")) }
    var expiry by remember { mutableStateOf(TextFieldValue(existing?.let { CardUtils.formatExpiry(it.expiry) } ?: "")) }
    var holder by remember { mutableStateOf(existing?.holder ?: "") }
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var style by remember { mutableIntStateOf(existing?.style ?: -1) }
    var showErrors by remember { mutableStateOf(false) }
    var topMode by remember { mutableStateOf(if (existing == null) TopMode.Camera else TopMode.Preview) }
    var torch by remember { mutableStateOf(false) }

    val digits = CardUtils.digits(number.text)
    val brand = CardBrand.detect(digits)
    val effectiveStyle = if (style >= 0) style else defaultStyleFor(brand)
    val numberOk = digits.length in brand.numberLength && CardUtils.luhn(digits)
    val expiryOk = CardUtils.expiryValid(expiry.text)

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permissionAsked by remember { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it; permissionAsked = true
    }
    LaunchedEffect(topMode) {
        if (topMode == TopMode.Camera && !hasCameraPermission && !permissionAsked) permLauncher.launch(Manifest.permission.CAMERA)
    }

    fun apply(scan: ScannedCard, source: String) {
        scan.number?.let { val f = CardUtils.formatNumber(it); number = TextFieldValue(f, TextRange(f.length)) }
        scan.expiry?.let { val f = CardUtils.formatExpiry(it); expiry = TextFieldValue(f, TextRange(f.length)) }
        scan.holder?.let { if (holder.isBlank()) holder = it.uppercase() }
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        topMode = TopMode.Preview
        torch = false
        scope.launch { snackbar.showSnackbar("Данные заполнены $source — проверьте и при необходимости исправьте") }
    }

    val nfc = remember { nfcStatus(context) }
    NfcCardListener(
        enabled = existing == null,
        onResult = { apply(it, "по NFC") },
        onError = { scope.launch { snackbar.showSnackbar("Не удалось прочитать карту. Держите её у телефона дольше") } },
    )

    Scaffold(
        containerColor = Mpay.Canvas,
        snackbarHost = { SnackbarHost(snackbar) { Snackbar(it, containerColor = Mpay.SurfaceHigh, contentColor = Mpay.TextPrimary) } },
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Новая карта" else "Изменить карту") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Mpay.Canvas),
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            AnimatedContent(topMode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "top") { mode ->
                when (mode) {
                    TopMode.Camera -> Box(
                        Modifier.fillMaxWidth().aspectRatio(1.4f).background(Mpay.Surface, RoundedCornerShape(24.dp))
                            .border(1.dp, Mpay.Outline, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        when {
                            !hasCameraPermission -> CameraBlocked(
                                denied = permissionAsked,
                                onRequest = {
                                    if (permissionAsked) context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                    ) else permLauncher.launch(Manifest.permission.CAMERA)
                                },
                                onManual = { topMode = TopMode.Preview },
                            )
                            cameraFailed -> CameraUnavailable { topMode = TopMode.Preview }
                            else -> {
                                CameraCardScanner(
                                    torch = torch,
                                    onResult = { apply(it, "с камеры") },
                                    onCameraError = { cameraFailed = true },
                                    modifier = Modifier.fillMaxSize(),
                                )
                                ScanFrame()
                                IconButton(
                                    onClick = { torch = !torch },
                                    modifier = Modifier.padding(12.dp).background(Mpay.Canvas.copy(alpha = 0.55f), CircleShape)
                                ) { Icon(if (torch) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff, "Фонарик") }
                                Surface(
                                    color = Mpay.Canvas.copy(alpha = 0.6f), shape = RoundedCornerShape(50),
                                    modifier = Modifier.align(Alignment.Center)
                                ) { Text("Наведите на карту", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                                Surface(
                                    onClick = { topMode = TopMode.Preview },
                                    color = Mpay.Canvas.copy(alpha = 0.6f), shape = RoundedCornerShape(50),
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                                ) { Text("Ввести вручную", color = Mpay.Lime, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) }
                            }
                        }
                    }
                    TopMode.Preview -> Column {
                        BankCardView(
                            brand, digits, effectiveStyle,
                            holder = holder,
                            expiry = CardUtils.digits(expiry.text), label = label,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CardStyles.forEachIndexed { i, s ->
                                Box(
                                    Modifier.size(34.dp)
                                        .background(Brush.linearGradient(listOf(s.top, s.bottom)), CircleShape)
                                        .border(2.dp, if (i == effectiveStyle) Mpay.Lime else Mpay.Outline, CircleShape)
                                        .clickable { style = i },
                                    contentAlignment = Alignment.Center
                                ) { if (i == effectiveStyle) Icon(Icons.Rounded.Check, s.name, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp)) }
                            }
                            Spacer(Modifier.weight(1f))
                            if (existing == null) IconButton(onClick = { topMode = TopMode.Camera }) {
                                Icon(Icons.Rounded.DocumentScanner, "Сканировать", tint = Mpay.Lime)
                            }
                        }
                    }
                }
            }

            if (existing == null && nfc != NfcStatus.Unsupported) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp).background(Mpay.Surface, RoundedCornerShape(16.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Nfc, null, tint = Mpay.Lime)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (nfc == NfcStatus.Ready) "Или приложите бесконтактную карту к задней панели телефона"
                        else "Включите NFC, чтобы считать карту прикосновением",
                        color = Mpay.TextSecondary, style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            CardField(
                value = number,
                onValueChange = {
                    val d = CardUtils.digits(it.text).take(19)
                    val f = CardUtils.formatNumber(d)
                    number = TextFieldValue(f, TextRange(f.length))
                    if (it.text.isNotEmpty() && topMode == TopMode.Camera) topMode = TopMode.Preview
                },
                label = "Номер карты",
                icon = Icons.Rounded.CreditCard,
                keyboard = KeyboardType.Number,
                error = when {
                    !showErrors && digits.length < brand.numberLength.first -> null
                    digits.isEmpty() -> "Введите номер карты"
                    digits.length !in brand.numberLength -> "Неверная длина номера для ${brand.title}"
                    !CardUtils.luhn(digits) -> "Номер карты введён с ошибкой"
                    else -> null
                },
                supporting = if (digits.length >= 1 && brand != CardBrand.Unknown) brand.title else null,
            )
            CardField(
                value = expiry,
                onValueChange = {
                    val d = CardUtils.digits(it.text).take(4)
                    val f = CardUtils.formatExpiry(d)
                    expiry = TextFieldValue(f, TextRange(f.length))
                },
                label = "Срок действия (ММ/ГГ)",
                icon = Icons.Rounded.CalendarMonth,
                keyboard = KeyboardType.Number,
                error = if ((showErrors || CardUtils.digits(expiry.text).length == 4) && !expiryOk) "Неверный или истёкший срок" else null,
            )
            CardField(
                value = TextFieldValue(holder, TextRange(holder.length)),
                onValueChange = { holder = it.text.uppercase().filter { c -> c.isLetter() || c == ' ' || c == '-' || c == '.' }.take(26) },
                label = "Имя владельца",
                icon = Icons.Rounded.Person,
                keyboard = KeyboardType.Text,
                caps = KeyboardCapitalization.Characters,
            )
            CardField(
                value = TextFieldValue(label, TextRange(label.length)),
                onValueChange = { label = it.text.take(24) },
                label = "Название карты",
                icon = Icons.Rounded.Sell,
                keyboard = KeyboardType.Text,
                caps = KeyboardCapitalization.Sentences,
                supporting = "Например: Зарплатная, Кредитка, Для путешествий",
            )
            Text(
                "CVV-код не нужен и не сохраняется. Данные карты зашифрованы и хранятся только на этом телефоне.",
                color = Mpay.TextSecondary, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    showErrors = true
                    if (numberOk && expiryOk) onSave(digits, CardUtils.digits(expiry.text), holder, label, effectiveStyle)
                    else scope.launch { snackbar.showSnackbar("Проверьте номер и срок действия карты") }
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mpay.Lime, contentColor = Mpay.OnLime),
            ) { Text(if (existing == null) "Сохранить карту" else "Сохранить изменения", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
private fun ScanFrame() {
    Canvas(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 36.dp)) {
        val l = 28.dp.toPx(); val w = 4.dp.toPx(); val c = Mpay.Lime
        val pts = listOf(
            Offset(0f, 0f) to (1f to 1f), Offset(size.width, 0f) to (-1f to 1f),
            Offset(0f, size.height) to (1f to -1f), Offset(size.width, size.height) to (-1f to -1f),
        )
        pts.forEach { (o, d) ->
            drawLine(c, o, Offset(o.x + l * d.first, o.y), w, StrokeCap.Round)
            drawLine(c, o, Offset(o.x, o.y + l * d.second), w, StrokeCap.Round)
        }
    }
}

@Composable
private fun CameraBlocked(denied: Boolean, onRequest: () -> Unit, onManual: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.DocumentScanner, null, tint = Mpay.Lime, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            if (denied) "Доступ к камере запрещён. Разрешите его в настройках, чтобы сканировать карты" else "Разрешите доступ к камере, чтобы отсканировать карту",
            textAlign = TextAlign.Center, color = Mpay.TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = Mpay.Lime, contentColor = Mpay.OnLime)) {
                Text(if (denied) "Открыть настройки" else "Разрешить")
            }
            OutlinedButton(onClick = onManual) { Text("Вручную") }
        }
    }
}

@Composable
private fun CameraUnavailable(onManual: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.VideocamOff, null, tint = Mpay.TextSecondary, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(8.dp))
        Text("Камера недоступна на этом устройстве", color = Mpay.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onManual) { Text("Ввести данные вручную") }
    }
}

@Composable
private fun CardField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboard: KeyboardType,
    caps: KeyboardCapitalization = KeyboardCapitalization.None,
    error: String? = null,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        trailingIcon = { Icon(icon, null) },
        singleLine = true,
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, capitalization = caps),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Mpay.Lime,
            unfocusedBorderColor = Mpay.Outline,
            focusedLabelColor = Mpay.Lime,
            cursorColor = Mpay.Lime,
            focusedContainerColor = Mpay.Surface,
            unfocusedContainerColor = Mpay.Surface,
            focusedTrailingIconColor = Mpay.Lime,
            unfocusedTrailingIconColor = Mpay.TextSecondary,
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

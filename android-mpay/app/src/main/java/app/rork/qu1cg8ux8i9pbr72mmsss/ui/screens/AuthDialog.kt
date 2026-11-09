package app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.rork.qu1cg8ux8i9pbr72mmsss.data.WalletViewModel
import app.rork.qu1cg8ux8i9pbr72mmsss.security.Biometric
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay

/** Full-screen gate: biometric first (if enabled), PIN as fallback. */
@Composable
fun AuthDialog(vm: WalletViewModel, title: String, onSuccess: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bioAllowed = vm.pin.biometricsEnabled && Biometric.available(context)
    val askBio = {
        Biometric.prompt(context, title, "Подтвердите, что это вы", onSuccess = onSuccess, onFallback = {})
    }
    LaunchedEffect(Unit) { if (bioAllowed) askBio() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Mpay.Canvas).safeDrawingPadding()) {
            IconButton(onClick = onDismiss, modifier = Modifier.padding(8.dp)) { Icon(Icons.Rounded.Close, "Закрыть") }
            PinVerifyPanel(
                title = title,
                verify = { vm.pin.verify(it) },
                onSuccess = onSuccess,
                onBiometric = if (bioAllowed) askBio else null,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

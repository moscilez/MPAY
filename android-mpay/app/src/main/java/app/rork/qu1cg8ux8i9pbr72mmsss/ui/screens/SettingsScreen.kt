package app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.rork.qu1cg8ux8i9pbr72mmsss.security.Biometric
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    biometrics: Boolean,
    onBiometrics: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val bioAvailable = Biometric.available(context)
    val itemColors = ListItemDefaults.colors(containerColor = Mpay.Surface)
    Scaffold(
        containerColor = Mpay.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Mpay.Canvas),
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("Безопасность", color = Mpay.TextSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Mpay.Surface)) {
                ListItem(
                    headlineContent = { Text("Оплата по отпечатку") },
                    supportingContent = { Text(if (bioAvailable) "Вместо PIN-кода" else "Биометрия не настроена на телефоне") },
                    leadingContent = { Icon(Icons.Rounded.Fingerprint, null, tint = Mpay.Lime) },
                    trailingContent = {
                        Switch(
                            checked = biometrics && bioAvailable, enabled = bioAvailable, onCheckedChange = onBiometrics,
                            colors = SwitchDefaults.colors(checkedTrackColor = Mpay.Lime, checkedThumbColor = Mpay.OnLime)
                        )
                    },
                    colors = itemColors,
                )
                HorizontalDivider(color = Mpay.Outline)
                ListItem(
                    headlineContent = { Text("Изменить PIN-код") },
                    leadingContent = { Icon(Icons.Rounded.Password, null, tint = Mpay.Lime) },
                    trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                    colors = itemColors,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onChangePin),
                )
            }
            Text("О приложении", color = Mpay.TextSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp))
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Mpay.Surface)) {
                ListItem(
                    headlineContent = { Text("Только на этом телефоне") },
                    supportingContent = { Text("Карты и история зашифрованы и не отправляются на серверы. Регистрация не нужна.") },
                    leadingContent = { Icon(Icons.Rounded.PhoneAndroid, null, tint = Mpay.Lime) },
                    colors = itemColors,
                )
                HorizontalDivider(color = Mpay.Outline)
                ListItem(
                    headlineContent = { Text("MPAY 1.0") },
                    supportingContent = { Text("CVV-коды никогда не запрашиваются и не хранятся.") },
                    leadingContent = { Icon(Icons.Rounded.Info, null, tint = Mpay.Lime) },
                    colors = itemColors,
                )
            }
        }
    }
}

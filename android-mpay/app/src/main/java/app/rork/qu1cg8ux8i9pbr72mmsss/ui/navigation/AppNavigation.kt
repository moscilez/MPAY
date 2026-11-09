package app.rork.qu1cg8ux8i9pbr72mmsss.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.rork.qu1cg8ux8i9pbr72mmsss.data.Transaction
import app.rork.qu1cg8ux8i9pbr72mmsss.data.WalletData
import app.rork.qu1cg8ux8i9pbr72mmsss.data.WalletViewModel
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens.AddEditCardScreen
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens.AuthDialog
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens.CardDetailScreen
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens.CardsScreen
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens.HistoryScreen
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens.HomeScreen
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens.OnboardingScreen
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens.PaySheet
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens.PinCreateFlow
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens.SettingsScreen
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens.TransactionSheet
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.screens.cardFor
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay

private enum class Tab(val title: String, val selected: ImageVector, val unselected: ImageVector) {
    Home("Главная", Icons.Rounded.Home, Icons.Outlined.Home),
    Cards("Карты", Icons.Rounded.CreditCard, Icons.Outlined.CreditCard),
    History("История", Icons.Rounded.History, Icons.Outlined.History),
}

@Composable
fun AppNavigation() {
    val vm: WalletViewModel = viewModel()
    val loaded by vm.loaded.collectAsState()
    val hasPin by vm.hasPin.collectAsState()

    Box(Modifier.fillMaxSize().background(Mpay.Canvas)) {
        when {
            !loaded -> Unit
            !hasPin -> OnboardingScreen(onPinCreated = { vm.setPin(it) })
            else -> WalletNavHost(vm)
        }
    }
}

@Composable
private fun WalletNavHost(vm: WalletViewModel) {
    val nav = rememberNavController()
    val data by vm.state.collectAsState()
    val biometrics by vm.biometrics.collectAsState()

    NavHost(
        navController = nav,
        startDestination = "main",
        enterTransition = { slideInHorizontally(tween(280)) { it / 4 } + fadeIn(tween(280)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { slideOutHorizontally(tween(250)) { it / 4 } + fadeOut(tween(250)) },
    ) {
        composable("main") { MainTabs(nav, vm, data) }

        composable("card/{id}") { entry ->
            val id = entry.arguments?.getString("id")
            val card = data.cards.firstOrNull { it.id == id }
            var pay by remember { mutableStateOf(false) }
            var auth by remember { mutableStateOf(false) }
            var tx by remember { mutableStateOf<Transaction?>(null) }
            if (card == null) {
                Box(Modifier.fillMaxSize().background(Mpay.Canvas))
                return@composable
            }
            CardDetailScreen(
                card = card, data = data,
                onBack = { nav.popBackStack() },
                onPay = { pay = true },
                onEdit = { auth = true },
                onMakeDefault = { vm.setDefault(card.id) },
                onDelete = { nav.popBackStack(); vm.deleteCard(card.id) },
                onTransaction = { tx = it },
            )
            if (auth) AuthDialog(vm, "Подтвердите, чтобы изменить карту",
                onSuccess = { auth = false; nav.navigate("edit/${card.id}") },
                onDismiss = { auth = false })
            if (pay) PaySheet(data, vm, card.id, onDismiss = { pay = false })
            TxSheet(tx, data, vm) { tx = null }
        }

        composable("add") {
            AddEditCardScreen(
                existing = null,
                onBack = { nav.popBackStack() },
                onSave = { number, expiry, holder, label, style ->
                    val id = vm.saveCard(null, number, expiry, holder, label, style)
                    nav.popBackStack()
                    nav.navigate("card/$id")
                },
            )
        }

        composable("edit/{id}") { entry ->
            val id = entry.arguments?.getString("id")
            val card = data.cards.firstOrNull { it.id == id }
            AddEditCardScreen(
                existing = card,
                onBack = { nav.popBackStack() },
                onSave = { number, expiry, holder, label, style ->
                    vm.saveCard(id, number, expiry, holder, label, style)
                    nav.popBackStack()
                },
            )
        }

        composable("settings") {
            var auth by remember { mutableStateOf(false) }
            SettingsScreen(
                biometrics = biometrics,
                onBiometrics = { vm.setBiometrics(it) },
                onChangePin = { auth = true },
                onBack = { nav.popBackStack() },
            )
            if (auth) AuthDialog(vm, "Введите текущий PIN-код",
                onSuccess = { auth = false; nav.navigate("changePin") },
                onDismiss = { auth = false })
        }

        composable("changePin") {
            PinCreateFlow(
                title = "Новый PIN-код",
                subtitle = "Придумайте 4 цифры",
                onDone = { vm.setPin(it); nav.popBackStack() },
            )
        }
    }
}

@Composable
private fun TxSheet(tx: Transaction?, data: WalletData, vm: WalletViewModel, onDismiss: () -> Unit) {
    if (tx == null) return
    TransactionSheet(
        t = tx, card = cardFor(data, tx),
        onDelete = { vm.deleteTransaction(tx.id); onDismiss() },
        onDismiss = onDismiss,
    )
}

@Composable
private fun MainTabs(nav: NavHostController, vm: WalletViewModel, data: WalletData) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var pay by remember { mutableStateOf(false) }
    var tx by remember { mutableStateOf<Transaction?>(null) }
    val openPay = { if (data.cards.isEmpty()) nav.navigate("add") else pay = true }

    Scaffold(
        containerColor = Mpay.Canvas,
        bottomBar = {
            NavigationBar(containerColor = Mpay.Surface) {
                Tab.entries.forEach { t ->
                    val sel = t == tab
                    NavigationBarItem(
                        selected = sel,
                        onClick = { tab = t },
                        icon = { Icon(if (sel) t.selected else t.unselected, null) },
                        label = { Text(t.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Mpay.Lime,
                            selectedTextColor = Mpay.Lime,
                            indicatorColor = Mpay.LimeDim,
                            unselectedIconColor = Mpay.TextSecondary,
                            unselectedTextColor = Mpay.TextSecondary,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        AnimatedContent(tab, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "tabs") { t ->
            when (t) {
                Tab.Home -> HomeScreen(
                    data = data, contentPadding = pad,
                    onSettings = { nav.navigate("settings") },
                    onOpenCard = { nav.navigate("card/$it") },
                    onAddCard = { nav.navigate("add") },
                    onAllCards = { tab = Tab.Cards },
                    onAllHistory = { tab = Tab.History },
                    onTransaction = { tx = it },
                    onPay = openPay,
                )
                Tab.Cards -> CardsScreen(
                    data = data, contentPadding = pad,
                    onOpenCard = { nav.navigate("card/$it") },
                    onAddCard = { nav.navigate("add") },
                    onPay = openPay,
                )
                Tab.History -> HistoryScreen(data = data, contentPadding = pad, onTransaction = { tx = it })
            }
        }
    }
    if (pay && data.cards.isNotEmpty()) PaySheet(data, vm, null, onDismiss = { pay = false })
    TxSheet(tx, data, vm) { tx = null }
}

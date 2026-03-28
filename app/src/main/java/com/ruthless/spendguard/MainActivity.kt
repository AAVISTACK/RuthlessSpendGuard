package com.ruthless.spendguard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruthless.spendguard.ui.screens.*
import com.ruthless.spendguard.ui.theme.*
import com.ruthless.spendguard.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint

enum class Screen { DASHBOARD, TRANSACTIONS, ANALYTICS, SETTINGS, GOALS }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestPermissions()

        setContent {
            SpendGuardTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

                Scaffold(
                    containerColor = SG.Background,
                    bottomBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SG.Surface)
                                .border(BorderStroke(1.dp, SG.CardBorder), shape = androidx.compose.ui.graphics.RectangleShape)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                NavTab(icon = Icons.Default.Home,   label = "Home",      selected = currentScreen == Screen.DASHBOARD)     { currentScreen = Screen.DASHBOARD }
                                NavTab(icon = Icons.Default.List,   label = "Txn",       selected = currentScreen == Screen.TRANSACTIONS)  { currentScreen = Screen.TRANSACTIONS }
                                NavTab(icon = Icons.Default.BarChart, label = "Stats",   selected = currentScreen == Screen.ANALYTICS)     { currentScreen = Screen.ANALYTICS }
                                NavTab(icon = Icons.Default.TrackChanges, label = "Goals", selected = currentScreen == Screen.GOALS)       { currentScreen = Screen.GOALS }
                                NavTab(icon = Icons.Default.Settings, label = "Settings", selected = currentScreen == Screen.SETTINGS)     { currentScreen = Screen.SETTINGS }
                            }
                        }
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .background(SG.Background)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            label = "screen",
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }
                        ) { screen ->
                            when (screen) {
                                Screen.DASHBOARD -> DashboardScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    onNavigateToTransactions = { currentScreen = Screen.TRANSACTIONS },
                                    onNavigateToAnalytics = { currentScreen = Screen.ANALYTICS },
                                    onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                                    onNavigateToGoals = { currentScreen = Screen.GOALS }
                                )
                                Screen.TRANSACTIONS -> TransactionsScreen(uiState, viewModel) {}
                                Screen.ANALYTICS -> AnalyticsScreen(uiState) {}
                                Screen.SETTINGS -> SettingsScreen(viewModel, uiState) {}
                                Screen.GOALS -> GoalsScreen(uiState, viewModel) {}
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RowScope.NavTab(
        icon: ImageVector,
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) SG.Green else SG.TextDim,
                modifier = Modifier.size(20.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) SG.Green else SG.TextDim
            )
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }
}

// ─── TRANSACTIONS SCREEN ──────────────────────────────────────────────────────

@Composable
fun TransactionsScreen(
    uiState: com.ruthless.spendguard.viewmodel.DashboardUiState,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SG.Background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("TRANSACTIONS", style = MaterialTheme.typography.headlineLarge, color = SG.TextPrimary)
            Spacer(Modifier.height(20.dp))
        }

        if (uiState.allTransactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No transactions yet.", style = MaterialTheme.typography.bodyMedium, color = SG.TextDim)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
            ) {
                // Group by date — show all in one card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SG.Card)
                            .border(1.dp, SG.CardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp)
                    ) {
                        uiState.allTransactions.forEachIndexed { index, txn ->
                            com.ruthless.spendguard.ui.components.TransactionItem(
                                category = txn.category.emoji,
                                merchant = txn.merchant,
                                time = txn.timestamp,
                                amount = txn.amount,
                                isWaste = txn.type == com.ruthless.spendguard.data.entities.TransactionType.WASTE
                            )
                            if (index < uiState.allTransactions.size - 1) {
                                com.ruthless.spendguard.ui.components.SGDivider()
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─── GOALS SCREEN ─────────────────────────────────────────────────────────────

@Composable
fun GoalsScreen(
    uiState: com.ruthless.spendguard.viewmodel.DashboardUiState,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var goalName by remember { mutableStateOf("") }
    var goalAmount by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SG.Background)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GOALS", style = MaterialTheme.typography.headlineLarge, color = SG.TextPrimary)
                com.ruthless.spendguard.ui.components.SGButton("+ ADD", onClick = { showAdd = true }, color = SG.Green, outlined = true)
            }
            Spacer(Modifier.height(8.dp))

            if (uiState.goals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("◎", fontSize = 40.sp, color = SG.TextDim)
                        Spacer(Modifier.height(8.dp))
                        Text("No goals set.", style = MaterialTheme.typography.bodyMedium, color = SG.TextDim)
                        Text("Set one to see how waste delays you.", style = MaterialTheme.typography.labelMedium, color = SG.TextDim)
                    }
                }
            } else {
                uiState.goals.forEach { goal ->
                    com.ruthless.spendguard.ui.components.GoalCard(
                        name = goal.name,
                        emoji = goal.emoji,
                        saved = goal.savedAmount,
                        target = goal.targetAmount,
                        wasteToday = uiState.todayWaste
                    )
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }

    if (showAdd) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(SG.Card)
                    .border(1.dp, SG.CardBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(modifier = Modifier.width(36.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(SG.CardBorder).align(Alignment.CenterHorizontally))
                Text("NEW GOAL", style = MaterialTheme.typography.headlineMedium, color = SG.TextPrimary)
                OutlinedTextField(value = goalName, onValueChange = { goalName = it },
                    label = { Text("Goal name", color = SG.TextDim) }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SG.TextPrimary, unfocusedTextColor = SG.TextPrimary, focusedBorderColor = SG.Green, unfocusedBorderColor = SG.CardBorder, cursorColor = SG.Green),
                    shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = goalAmount, onValueChange = { goalAmount = it.filter { c -> c.isDigit() } },
                    label = { Text("Target amount", color = SG.TextDim) }, prefix = { Text("₹", color = SG.Green) }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SG.TextPrimary, unfocusedTextColor = SG.TextPrimary, focusedBorderColor = SG.Green, unfocusedBorderColor = SG.CardBorder, cursorColor = SG.Green),
                    shape = RoundedCornerShape(10.dp))
                com.ruthless.spendguard.ui.components.SGButton("SAVE GOAL", onClick = {
                    val amt = goalAmount.toDoubleOrNull() ?: return@SGButton
                    viewModel.addGoal(com.ruthless.spendguard.data.entities.Goal(name = goalName, targetAmount = amt))
                    showAdd = false; goalName = ""; goalAmount = ""
                }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { showAdd = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = SG.TextDim)
                }
            }
        }
    }
}

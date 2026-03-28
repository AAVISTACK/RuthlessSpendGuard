package com.ruthless.spendguard.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.ruthless.spendguard.data.entities.*
import com.ruthless.spendguard.ui.components.*
import com.ruthless.spendguard.ui.theme.SG
import com.ruthless.spendguard.viewmodel.DashboardUiState
import com.ruthless.spendguard.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    uiState: DashboardUiState,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGoals: () -> Unit
) {
    var showManualEntry by remember { mutableStateOf(false) }
    var impulseSecondsLeft by remember { mutableStateOf(300) }
    var impulseActive by remember { mutableStateOf(false) }

    // Impulse countdown
    LaunchedEffect(impulseActive) {
        if (impulseActive) {
            impulseSecondsLeft = 300
            while (impulseSecondsLeft > 0 && impulseActive) {
                kotlinx.coroutines.delay(1000)
                impulseSecondsLeft--
            }
            impulseActive = false
        }
    }

    // Lockdown wall
    if (uiState.isLockdownActive) {
        LockdownWall(spent = uiState.todayTotal, limit = uiState.dailyLimit)
        return
    }

    // Shame screen
    if (uiState.showShameScreen) {
        ShameScreen(failCount = 3, onAcknowledge = { viewModel.dismissShameScreen() })
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(SG.Background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── TOP BAR ──────────────────────────────────────────────────────
            item {
                TopBar(onSettings = onNavigateToSettings)
            }

            // ── HERO: BUDGET ARC ─────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BudgetArc(spent = uiState.todayTotal, limit = uiState.dailyLimit)
                }
            }

            // ── THIN PROGRESS BAR (secondary visual) ─────────────────────────
            item {
                BudgetBar(
                    progress = uiState.budgetPercent,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // ── STATS (waste / essential / count) ────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                StatsRow(
                    waste = uiState.todayWaste,
                    essential = uiState.todayTotal - uiState.todayWaste,
                    txnCount = uiState.transactions.size
                )
            }

            // ── STREAKS (compact, one card) ──────────────────────────────────
            if (uiState.streaks.isNotEmpty()) {
                item {
                    val pairs = uiState.streaks.map { s ->
                        when (s.type) {
                            StreakType.NO_JUNK -> "No junk food" to s.currentCount
                            StreakType.UNDER_BUDGET -> "Under budget" to s.currentCount
                            StreakType.NO_CIGARETTE -> "No cigarettes" to s.currentCount
                            StreakType.NO_COLD_DRINK -> "No cold drinks" to s.currentCount
                        }
                    }
                    StreakRow(streaks = pairs)
                }
            }

            // ── AI INSIGHT ───────────────────────────────────────────────────
            item {
                val insight = buildInsight(uiState)
                InsightCard(text = insight)
            }

            // ── TODAY'S TRANSACTIONS ─────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("TRANSACTIONS TODAY")
                    if (uiState.transactions.size > 4) {
                        TextButton(
                            onClick = onNavigateToTransactions,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("SEE ALL", style = MaterialTheme.typography.labelSmall, color = SG.TextDim)
                        }
                    }
                }
            }

            if (uiState.transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SG.Card)
                            .border(1.dp, SG.CardBorder, RoundedCornerShape(16.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("◎", fontSize = 32.sp, color = SG.Green)
                            Spacer(Modifier.height(8.dp))
                            Text("Clean slate today", style = MaterialTheme.typography.bodyMedium, color = SG.TextBody)
                        }
                    }
                }
            } else {
                item {
                    // All transactions in ONE card — clean grouped list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SG.Card)
                            .border(1.dp, SG.CardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp)
                    ) {
                        uiState.transactions.take(5).forEachIndexed { index, txn ->
                            TransactionItem(
                                emoji = txn.category.emoji,
                                merchant = txn.merchant,
                                time = formatTime(txn.timestamp),
                                amount = txn.amount,
                                isWaste = txn.type == TransactionType.WASTE
                            )
                            if (index < minOf(uiState.transactions.size - 1, 4)) {
                                SGDivider()
                            }
                        }
                    }
                }
            }

            // ── GOAL ─────────────────────────────────────────────────────────
            uiState.goals.firstOrNull()?.let { goal ->
                item {
                    Spacer(Modifier.height(4.dp))
                    GoalCard(
                        name = goal.name,
                        emoji = goal.emoji,
                        saved = goal.savedAmount,
                        target = goal.targetAmount,
                        wasteToday = uiState.todayWaste
                    )
                }
            }

            // ── REALITY CHECK ─────────────────────────────────────────────────
            item { RealityCard(dailyWaste = uiState.todayWaste) }
        }

        // ── FAB ──────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SG.Green)
                    .clickable { showManualEntry = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = SG.TextOnAccent)
            }
        }

        // ── IMPULSE OVERLAY (floats above everything) ──────────────────────
        if (uiState.showImpulseDialog) {
            ImpulseOverlay(
                merchant = "Last transaction",
                secondsLeft = impulseSecondsLeft,
                onSkip = { viewModel.dismissImpulseDialog() }
            )
        }

        // ── MANUAL ENTRY ──────────────────────────────────────────────────
        if (showManualEntry) {
            ManualEntrySheet(
                onDismiss = { showManualEntry = false },
                onConfirm = { amount, merchant, category, type ->
                    viewModel.addManualTransaction(amount, merchant, category, type)
                    showManualEntry = false
                }
            )
        }
    }
}

// ─── TOP BAR ─────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "SPEND GUARD",
            style = MaterialTheme.typography.headlineLarge,
            color = SG.TextPrimary
        )
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SG.Card)
                .border(1.dp, SG.CardBorder, RoundedCornerShape(8.dp))
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = SG.TextBody,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── BUILD INSIGHT ────────────────────────────────────────────────────────────

private fun buildInsight(uiState: DashboardUiState): String {
    val waste = uiState.todayWaste
    val total = uiState.todayTotal
    val limit = uiState.dailyLimit
    val top = uiState.topMerchants.firstOrNull()

    return when {
        total == 0.0 -> "No spending recorded yet. A clean start."
        waste / limit > 0.5 -> "Over half your budget has gone to waste today. This is the pattern that keeps you stuck."
        top != null && top.count >= 3 -> "${top.name} — ${top.count} visits this week. Aadat hai, zarurat nahi."
        waste > 0 && waste / total > 0.6 -> "${((waste / total) * 100).toInt()}% of your spending today is waste. Identify the trigger."
        uiState.budgetPercent >= 0.7f -> "70% of today's limit used. Slow down before you cross it."
        else -> "Tracking your patterns. Every transaction is data. Use it."
    }
}

// ─── LOCKDOWN SCREEN ─────────────────────────────────────────────────────────
// (Keeping this here as well for direct navigation use)

@Composable
fun LockdownScreen(spent: Double, limit: Double, onDismiss: () -> Unit) {
    LockdownWall(spent = spent, limit = limit)
}

// ─── SHAME SCREEN ─────────────────────────────────────────────────────────────

@Composable
fun ShameScreenCompat(onDismiss: () -> Unit) {
    ShameScreen(failCount = 3, onAcknowledge = onDismiss)
}

// ─── IMPULSE DELAY DIALOG ─────────────────────────────────────────────────────

@Composable
fun ImpulseDelayDialog(
    delayMinutes: Int,
    onComplete: () -> Unit,
    onForceSkip: () -> Unit
) {
    var secondsLeft by remember { mutableStateOf(delayMinutes * 60) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            secondsLeft--
        }
        onComplete()
    }
    ImpulseOverlay(
        merchant = "Potential impulse spend",
        secondsLeft = secondsLeft,
        onSkip = onForceSkip
    )
}

// ─── MANUAL ENTRY ─────────────────────────────────────────────────────────────

@Composable
fun ManualEntrySheet(
    onDismiss: () -> Unit,
    onConfirm: (Double, String, TransactionCategory, TransactionType) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.NEUTRAL) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(SG.Card)
                .border(width = 1.dp, color = SG.CardBorder, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Handle bar
            Box(modifier = Modifier.width(36.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(SG.CardBorder).align(Alignment.CenterHorizontally))

            Text("ADD TRANSACTION", style = MaterialTheme.typography.headlineMedium, color = SG.TextPrimary)

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount", color = SG.TextDim) },
                prefix = { Text("₹", color = SG.Green) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SG.TextPrimary, unfocusedTextColor = SG.TextPrimary,
                    focusedBorderColor = SG.Green, unfocusedBorderColor = SG.CardBorder,
                    cursorColor = SG.Green
                ),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant", color = SG.TextDim) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SG.TextPrimary, unfocusedTextColor = SG.TextPrimary,
                    focusedBorderColor = SG.Green, unfocusedBorderColor = SG.CardBorder,
                    cursorColor = SG.Green
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Type selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SGButton(
                    "WASTE",
                    onClick = { type = TransactionType.WASTE },
                    color = SG.Red,
                    outlined = type != TransactionType.WASTE,
                    modifier = Modifier.weight(1f)
                )
                SGButton(
                    "ESSENTIAL",
                    onClick = { type = TransactionType.ESSENTIAL },
                    color = SG.Green,
                    outlined = type != TransactionType.ESSENTIAL,
                    modifier = Modifier.weight(1f)
                )
            }

            SGButton(
                "SAVE TRANSACTION",
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@SGButton
                    onConfirm(amt, merchant.ifBlank { "Manual Entry" }, TransactionCategory.UNKNOWN, type)
                },
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = SG.TextDim)
            }
        }
    }
}

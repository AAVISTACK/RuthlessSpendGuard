package com.ruthless.spendguard.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.ruthless.spendguard.data.entities.TransactionType
import com.ruthless.spendguard.ui.components.*
import com.ruthless.spendguard.ui.theme.SG
import com.ruthless.spendguard.viewmodel.DashboardUiState

@Composable
fun AnalyticsScreen(uiState: DashboardUiState, onBack: () -> Unit) {
    val weeklyTotal = uiState.weeklyTotal
    val weeklyWaste = uiState.weeklyWaste
    val weeklyEssential = uiState.weeklyEssential
    val wasteRatio = if (weeklyTotal > 0) weeklyWaste / weeklyTotal else 0.0
    val saved = (uiState.dailyLimit * 7 - weeklyTotal).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SG.Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("ANALYTICS", style = MaterialTheme.typography.headlineLarge, color = SG.TextPrimary)
            Spacer(Modifier.height(20.dp))

            // Weekly summary card
            SGCard {
                SectionLabel("THIS WEEK")
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    WeekFigure("Total", "₹${weeklyTotal.toLong()}", SG.TextPrimary)
                    WeekFigure("Waste", "₹${weeklyWaste.toLong()}", SG.Red)
                    WeekFigure("Essential", "₹${weeklyEssential.toLong()}", SG.Green)
                    WeekFigure("Saved", "₹${saved.toLong()}", SG.TextBody)
                }
                Spacer(Modifier.height(16.dp))
                SGDivider()
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Waste ratio", style = MaterialTheme.typography.labelMedium, color = SG.TextDim)
                    Text("${(wasteRatio * 100).toInt()}%", style = MaterialTheme.typography.labelLarge,
                        color = if (wasteRatio > 0.5) SG.Red else SG.Green, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                BudgetBar(progress = wasteRatio.toFloat())
            }

            Spacer(Modifier.height(12.dp))

            // Top merchants
            if (uiState.topMerchants.isNotEmpty()) {
                SGCard {
                    SectionLabel("TOP MERCHANTS")
                    Spacer(Modifier.height(12.dp))
                    uiState.topMerchants.take(5).forEachIndexed { i, m ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(m.name, style = MaterialTheme.typography.titleMedium, color = SG.TextPrimary)
                                Text("${m.count} visits", style = MaterialTheme.typography.labelMedium, color = SG.TextDim)
                            }
                            Text("₹${m.total.toLong()}", style = MaterialTheme.typography.titleMedium,
                                color = SG.Red, fontWeight = FontWeight.SemiBold)
                        }
                        if (i < minOf(uiState.topMerchants.size - 1, 4)) SGDivider()
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Lifetime stats
            SGCard {
                SectionLabel("ALL TIME")
                Spacer(Modifier.height(12.dp))
                val all = uiState.allTransactions
                val totalSpent = all.sumOf { it.amount }
                val wasteCount = all.count { it.type == TransactionType.WASTE }
                val essentialCount = all.count { it.type == TransactionType.ESSENTIAL }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total transactions", style = MaterialTheme.typography.bodyMedium, color = SG.TextBody)
                    Text("${all.size}", style = MaterialTheme.typography.titleMedium, color = SG.TextPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                SGDivider()
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total spent", style = MaterialTheme.typography.bodyMedium, color = SG.TextBody)
                    Text("₹${totalSpent.toLong()}", style = MaterialTheme.typography.titleMedium, color = SG.TextPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                SGDivider()
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Waste / Essential", style = MaterialTheme.typography.bodyMedium, color = SG.TextBody)
                    Text("$wasteCount / $essentialCount", style = MaterialTheme.typography.titleMedium, color = SG.TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun WeekFigure(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = SG.TextDim)
    }
}

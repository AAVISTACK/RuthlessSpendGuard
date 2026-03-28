package com.ruthless.spendguard.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.*
import com.ruthless.spendguard.ui.components.*
import com.ruthless.spendguard.ui.theme.SG
import com.ruthless.spendguard.util.VoiceMode
import com.ruthless.spendguard.viewmodel.DashboardUiState
import com.ruthless.spendguard.viewmodel.DashboardViewModel

@Composable
fun SettingsScreen(
    viewModel: DashboardViewModel,
    uiState: DashboardUiState,
    onBack: () -> Unit
) {
    var limitInput by remember { mutableStateOf(uiState.dailyLimit.toLong().toString()) }
    var showLimitDialog by remember { mutableStateOf(false) }

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
            Text("SETTINGS", style = MaterialTheme.typography.headlineLarge, color = SG.TextPrimary)
            Spacer(Modifier.height(8.dp))

            // Daily Limit
            SGCard(onClick = { showLimitDialog = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        SectionLabel("DAILY LIMIT")
                        Spacer(Modifier.height(4.dp))
                        Text("Tap to change", style = MaterialTheme.typography.labelMedium, color = SG.TextDim)
                    }
                    Text(
                        "₹${uiState.dailyLimit.toLong()}",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                        color = SG.Green
                    )
                }
            }

            // Voice Feedback
            SGCard {
                SectionLabel("VOICE FEEDBACK")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enabled", style = MaterialTheme.typography.bodyMedium, color = SG.TextBody)
                    Switch(
                        checked = uiState.voiceEnabled,
                        onCheckedChange = { viewModel.setVoiceEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SG.TextOnAccent,
                            checkedTrackColor = SG.Green,
                            uncheckedThumbColor = SG.TextDim,
                            uncheckedTrackColor = SG.CardBorder
                        )
                    )
                }

                if (uiState.voiceEnabled) {
                    Spacer(Modifier.height(12.dp))
                    SGDivider()
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("MODE")
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceMode.values().forEach { mode ->
                            val label = when (mode) {
                                VoiceMode.RUTHLESS -> "Ruthless"
                                VoiceMode.CALM -> "Calm"
                                VoiceMode.FUNNY -> "Funny"
                            }
                            SGButton(
                                label,
                                onClick = { viewModel.setVoiceMode(mode) },
                                color = SG.Green,
                                outlined = uiState.voiceMode != mode,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Behavioral controls
            SGCard {
                SectionLabel("BEHAVIORAL CONTROLS")
                Spacer(Modifier.height(12.dp))
                SettingToggle("Impulse delay (5 min)", uiState.impulseDelayEnabled) {}
                SGDivider()
                SettingToggle("Shame screen", uiState.shameScreenEnabled) {}
                SGDivider()
                SettingToggle("Dopamine reset mode", uiState.dopamineModeActive) { viewModel.setDopamineMode(it) }
                SGDivider()
                SettingToggle("Lockdown on limit exceeded", false) { viewModel.enableLockdown(it) }
            }

            // App info
            SGCard {
                SectionLabel("ABOUT")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ruthless Spend Guard v1.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SG.TextBody
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Built to enforce discipline.\nNo excuses. No sugarcoating.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SG.TextDim,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    // Limit dialog
    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            containerColor = SG.Card,
            tonalElevation = 0.dp,
            title = {
                Text("SET DAILY LIMIT", style = MaterialTheme.typography.headlineMedium, color = SG.TextPrimary)
            },
            text = {
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount (₹)", color = SG.TextDim) },
                    prefix = { Text("₹", color = SG.Green) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SG.TextPrimary,
                        unfocusedTextColor = SG.TextPrimary,
                        focusedBorderColor = SG.Green,
                        unfocusedBorderColor = SG.CardBorder,
                        cursorColor = SG.Green
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                SGButton("SAVE", onClick = {
                    limitInput.toDoubleOrNull()?.let { viewModel.setDailyLimit(it) }
                    showLimitDialog = false
                })
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = SG.TextDim)
                }
            }
        )
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SG.TextBody)
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SG.TextOnAccent,
                checkedTrackColor = SG.Green,
                uncheckedThumbColor = SG.TextDim,
                uncheckedTrackColor = SG.CardBorder
            )
        )
    }
}

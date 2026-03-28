package com.ruthless.spendguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruthless.spendguard.data.entities.*
import com.ruthless.spendguard.data.repository.SpendGuardRepository
import com.ruthless.spendguard.di.UserPreferencesManager
import com.ruthless.spendguard.util.VoiceFeedbackManager
import com.ruthless.spendguard.util.VoiceMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val todayTotal: Double = 0.0,
    val todayWaste: Double = 0.0,
    val dailyLimit: Double = 100.0,
    val transactions: List<Transaction> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val streaks: List<Streak> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val isLockdownActive: Boolean = false,
    val budgetPercent: Float = 0f,
    val fearLevel: Float = 0f,
    val voiceMode: VoiceMode = VoiceMode.RUTHLESS,
    val voiceEnabled: Boolean = true,
    val impulseDelayEnabled: Boolean = true,
    val shameScreenEnabled: Boolean = true,
    val dopamineModeActive: Boolean = false,
    val weeklyWaste: Double = 0.0,
    val weeklyEssential: Double = 0.0,
    val weeklyTotal: Double = 0.0,
    val topMerchants: List<MerchantData> = emptyList(),
    val showImpulseDialog: Boolean = false,
    val showShameScreen: Boolean = false,
    val isLoading: Boolean = true
)

data class MerchantData(val name: String, val count: Int, val total: Double)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: SpendGuardRepository,
    private val preferences: UserPreferencesManager,
    private val voiceFeedback: VoiceFeedbackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _showImpulseTimer = MutableStateFlow(false)
    val showImpulseTimer: StateFlow<Boolean> = _showImpulseTimer.asStateFlow()

    init {
        voiceFeedback.initialize()
        loadData()
        initializeApp()
    }

    private fun initializeApp() {
        viewModelScope.launch {
            repository.ensureStreaksExist()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            // Combine all flows
            combine(
                repository.getTodayTotal(),
                repository.getTodayWaste(),
                repository.getTodayTransactions(),
                repository.getAllTransactions(),
                preferences.dailyLimit,
                repository.getAllStreaks(),
                repository.getActiveGoals(),
                preferences.lockdownActive,
                preferences.voiceMode,
                preferences.voiceEnabled,
                preferences.impulseDelayEnabled,
                preferences.shameScreenEnabled,
                preferences.dopamineModeActive
            ) { values ->
                val todayTotal = values[0] as Double
                val todayWaste = values[1] as Double
                @Suppress("UNCHECKED_CAST")
                val todayTxns = values[2] as List<Transaction>
                @Suppress("UNCHECKED_CAST")
                val allTxns = values[3] as List<Transaction>
                val limit = values[4] as Double
                @Suppress("UNCHECKED_CAST")
                val streaks = values[5] as List<Streak>
                @Suppress("UNCHECKED_CAST")
                val goals = values[6] as List<Goal>
                val lockdown = values[7] as Boolean
                val vMode = values[8] as VoiceMode
                val vEnabled = values[9] as Boolean
                val impulse = values[10] as Boolean
                val shame = values[11] as Boolean
                val dopamine = values[12] as Boolean

                val percent = (todayTotal / limit).coerceIn(0.0, 1.0).toFloat()
                val fear = calculateFearLevel(todayTotal, limit, todayWaste, todayTxns.size)

                _uiState.value = _uiState.value.copy(
                    todayTotal = todayTotal,
                    todayWaste = todayWaste,
                    dailyLimit = limit,
                    transactions = todayTxns,
                    allTransactions = allTxns,
                    streaks = streaks,
                    goals = goals,
                    isLockdownActive = lockdown,
                    budgetPercent = percent,
                    fearLevel = fear,
                    voiceMode = vMode,
                    voiceEnabled = vEnabled,
                    impulseDelayEnabled = impulse,
                    shameScreenEnabled = shame,
                    dopamineModeActive = dopamine,
                    isLoading = false
                )
            }.catch { /* handle error */ }.collect()
        }

        loadWeeklyStats()
    }

    private fun loadWeeklyStats() {
        viewModelScope.launch {
            val (weekStart, weekEnd) = repository.getWeekBounds()
            val breakdown = repository.getCategoryBreakdown(weekStart, weekEnd)
            val waste = breakdown.filter { it.category != TransactionCategory.FUEL.name && it.category != TransactionCategory.ESSENTIAL.name }.sumOf { it.total }
            val essential = breakdown.filter { it.category == TransactionCategory.ESSENTIAL.name || it.category == TransactionCategory.FUEL.name }.sumOf { it.total }

            val merchants = repository.getTopMerchants(weekStart)
            val merchantData = merchants.map { MerchantData(it.merchant, it.count, it.total) }

            _uiState.update { state ->
                state.copy(
                    weeklyWaste = waste,
                    weeklyEssential = essential,
                    weeklyTotal = waste + essential,
                    topMerchants = merchantData
                )
            }
        }
    }

    private fun calculateFearLevel(total: Double, limit: Double, waste: Double, count: Int): Float {
        val budgetFear = (total / limit).coerceIn(0.0, 1.0)
        val wasteFear = if (total > 0) (waste / total).coerceIn(0.0, 1.0) else 0.0
        val countFear = (count / 10.0).coerceIn(0.0, 1.0)
        return ((budgetFear * 0.5f + wasteFear * 0.3f + countFear * 0.2f)).toFloat()
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    fun setDailyLimit(limit: Double) {
        viewModelScope.launch { preferences.setDailyLimit(limit) }
    }

    fun setVoiceMode(mode: VoiceMode) {
        viewModelScope.launch { preferences.setVoiceMode(mode) }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setVoiceEnabled(enabled) }
    }

    fun addManualTransaction(amount: Double, merchant: String, category: TransactionCategory, type: TransactionType) {
        viewModelScope.launch {
            val txn = Transaction(
                amount = amount,
                merchant = merchant,
                category = category,
                type = type,
                isManual = true
            )
            repository.insertTransaction(txn)

            if (type == TransactionType.WASTE && _uiState.value.voiceEnabled) {
                val mode = _uiState.value.voiceMode
                voiceFeedback.speakForMode(mode)
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }

    fun markTransactionType(transaction: Transaction, type: TransactionType) {
        viewModelScope.launch {
            repository.updateTransaction(transaction.copy(type = type))
        }
    }

    fun triggerImpulseDialog() {
        _uiState.update { it.copy(showImpulseDialog = true) }
        if (_uiState.value.voiceEnabled) {
            voiceFeedback.speakImpulseDetected()
        }
    }

    fun dismissImpulseDialog() {
        _uiState.update { it.copy(showImpulseDialog = false) }
    }

    fun enableLockdown(enabled: Boolean) {
        viewModelScope.launch { preferences.setLockdownEnabled(enabled) }
    }

    fun setDopamineMode(active: Boolean) {
        viewModelScope.launch { preferences.setDopamineModeActive(active) }
    }

    fun speakMotivation() {
        val mode = _uiState.value.voiceMode
        voiceFeedback.speakForMode(mode)
    }

    fun speakGoodControl() {
        voiceFeedback.speakGoodControl()
    }

    fun addGoal(goal: Goal) {
        viewModelScope.launch { repository.insertGoal(goal) }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch { repository.deleteGoal(goal) }
    }

    fun dismissShameScreen() {
        _uiState.update { it.copy(showShameScreen = false) }
    }

    fun triggerShameScreen() {
        _uiState.update { it.copy(showShameScreen = true) }
        if (_uiState.value.voiceEnabled) {
            voiceFeedback.speak("Tu control me nahi hai abhi. Rok apne aap ko.", VoiceMode.RUTHLESS)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceFeedback.shutdown()
    }
}

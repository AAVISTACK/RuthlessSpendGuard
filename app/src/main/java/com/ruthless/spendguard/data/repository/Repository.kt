package com.ruthless.spendguard.data.repository

import com.ruthless.spendguard.data.dao.*
import com.ruthless.spendguard.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendGuardRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val dailySummaryDao: DailySummaryDao,
    private val goalDao: GoalDao,
    private val keywordDao: KeywordDao,
    private val streakDao: StreakDao,
    private val voiceJournalDao: VoiceJournalDao
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ─── Transactions ─────────────────────────────────────────────────────────

    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insert(transaction)
        updateDailySummary()
        return id
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction)
        updateDailySummary()
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.softDelete(id)
        updateDailySummary()
    }

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    fun getTodayTransactions(): Flow<List<Transaction>> {
        val (start, end) = getDayBounds()
        return transactionDao.getTransactionsForDay(start, end)
    }

    fun getTodayTotal(): Flow<Double> {
        val (start, end) = getDayBounds()
        return transactionDao.getTotalSpentForDay(start, end).map { it ?: 0.0 }
    }

    fun getTodayWaste(): Flow<Double> {
        val (start, end) = getDayBounds()
        return transactionDao.getWasteSpentForDay(start, end).map { it ?: 0.0 }
    }

    suspend fun getCategoryBreakdown(startTime: Long, endTime: Long): List<CategorySummary> =
        transactionDao.getCategoryBreakdown(startTime, endTime)

    suspend fun getTopMerchants(since: Long): List<MerchantSummary> =
        transactionDao.getTopMerchants(since)

    // ─── Daily Summary ────────────────────────────────────────────────────────

    fun getAllDailySummaries(): Flow<List<DailySummary>> =
        dailySummaryDao.getAllSummaries()

    fun getWeeklySummaries(): Flow<List<DailySummary>> =
        dailySummaryDao.getWeeklySummaries()

    fun getMonthlySummaries(): Flow<List<DailySummary>> =
        dailySummaryDao.getMonthlySummaries()

    // FIX: was calling getWasteSpentForDay but never collecting from it (.first()),
    // so wasteSpent was always 0 in DailySummary. Now properly awaits the Flow value.
    private suspend fun updateDailySummary() {
        val today = dateFormat.format(Date())
        val (start, end) = getDayBounds()
        val categoryBreakdown = transactionDao.getCategoryBreakdown(start, end)
        val total = categoryBreakdown.sumOf { it.total }
        val waste = transactionDao.getWasteSpentForDay(start, end).first() ?: 0.0
        val existing = dailySummaryDao.getSummaryForDate(today)
        val summary = (existing ?: DailySummary(date = today)).copy(
            totalSpent = total,
            wasteSpent = waste,
            essentialSpent = (total - waste).coerceAtLeast(0.0),
            transactionCount = categoryBreakdown.sumOf { it.count }
        )
        dailySummaryDao.upsert(summary)
    }

    // ─── Goals ────────────────────────────────────────────────────────────────

    fun getActiveGoals(): Flow<List<Goal>> = goalDao.getActiveGoals()
    fun getAllGoals(): Flow<List<Goal>> = goalDao.getAllGoals()
    suspend fun insertGoal(goal: Goal): Long = goalDao.insert(goal)
    suspend fun updateGoal(goal: Goal) = goalDao.update(goal)
    suspend fun deleteGoal(goal: Goal) = goalDao.delete(goal)

    // ─── Keywords ─────────────────────────────────────────────────────────────

    fun getAllKeywords(): Flow<List<Keyword>> = keywordDao.getAllKeywords()

    suspend fun addKeyword(keyword: Keyword): Long {
        if (keywordDao.existsKeyword(keyword.word) > 0) return -1
        return keywordDao.insert(keyword)
    }

    suspend fun deleteKeyword(keyword: Keyword) = keywordDao.delete(keyword)

    suspend fun insertDefaultKeywords() {
        val defaults = buildList {
            addAll(listOf(
                "pan shop", "cigarette", "bidi", "tobacco", "gutka",
                "swiggy", "zomato", "burger", "pizza", "kfc",
                "cafe", "coffee", "cold drink", "pepsi", "coke"
            ).map { Keyword(word = it, type = KeywordType.WASTE, category = TransactionCategory.UNKNOWN) })

            addAll(listOf(
                "petrol", "fuel", "diesel", "medicine", "hospital",
                "electricity", "rent", "school fee", "insurance"
            ).map { Keyword(word = it, type = KeywordType.ESSENTIAL, category = TransactionCategory.ESSENTIAL) })
        }
        defaults.forEach { keywordDao.insert(it) }
    }

    // ─── Streaks ──────────────────────────────────────────────────────────────

    fun getAllStreaks(): Flow<List<Streak>> = streakDao.getAllStreaks()
    suspend fun getStreakByType(type: StreakType): Streak? = streakDao.getStreakByType(type.name)
    suspend fun incrementStreak(type: StreakType) = streakDao.incrementStreak(type.name)
    suspend fun resetStreak(type: StreakType) = streakDao.resetStreak(type.name)

    suspend fun ensureStreaksExist() {
        StreakType.values().forEach { type ->
            if (streakDao.getStreakByType(type.name) == null) {
                streakDao.upsert(Streak(type = type))
            }
        }
    }

    // ─── Voice Journals ───────────────────────────────────────────────────────

    fun getAllVoiceJournals(): Flow<List<VoiceJournal>> = voiceJournalDao.getAllJournals()
    suspend fun insertVoiceJournal(journal: VoiceJournal): Long = voiceJournalDao.insert(journal)
    suspend fun deleteVoiceJournal(journal: VoiceJournal) = voiceJournalDao.delete(journal)
    suspend fun getFutureWarning(): VoiceJournal? = voiceJournalDao.getFutureWarning()

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun getDayBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 86_400_000L - 1
        return Pair(start, end)
    }

    fun getWeekBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = System.currentTimeMillis()
        return Pair(start, end)
    }
}

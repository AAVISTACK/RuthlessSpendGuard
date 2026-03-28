package com.ruthless.spendguard.data.dao

import androidx.room.*
import com.ruthless.spendguard.data.entities.*
import kotlinx.coroutines.flow.Flow

// ─── Transaction DAO ──────────────────────────────────────────────────────────

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Query("UPDATE transactions SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions
        WHERE isDeleted = 0 AND timestamp >= :startOfDay AND timestamp <= :endOfDay
        ORDER BY timestamp DESC
    """)
    fun getTransactionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<Transaction>>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE isDeleted = 0 AND timestamp >= :startOfDay AND timestamp <= :endOfDay
    """)
    fun getTotalSpentForDay(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE isDeleted = 0 AND type = 'WASTE' AND timestamp >= :startOfDay AND timestamp <= :endOfDay
    """)
    fun getWasteSpentForDay(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("""
        SELECT * FROM transactions
        WHERE isDeleted = 0 AND timestamp >= :startTime
        ORDER BY timestamp DESC
    """)
    fun getTransactionsSince(startTime: Long): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions
        WHERE isDeleted = 0 AND merchant LIKE '%' || :merchant || '%'
        ORDER BY timestamp DESC
    """)
    suspend fun getTransactionsByMerchant(merchant: String): List<Transaction>

    @Query("""
        SELECT merchant as name, COUNT(*) as count, SUM(amount) as total
        FROM transactions
        WHERE isDeleted = 0 AND timestamp >= :since
        GROUP BY merchant
        ORDER BY count DESC
        LIMIT 10
    """)
    suspend fun getTopMerchants(since: Long): List<MerchantSummary>

    // FIX: added COUNT(*) as count to CategorySummary so updateDailySummary can use it
    @Query("""
        SELECT category, SUM(amount) as total, COUNT(*) as count
        FROM transactions
        WHERE isDeleted = 0 AND timestamp >= :startTime AND timestamp <= :endTime
        GROUP BY category
    """)
    suspend fun getCategoryBreakdown(startTime: Long, endTime: Long): List<CategorySummary>

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE isDeleted = 0 AND merchant = :merchant AND timestamp >= :since
    """)
    suspend fun getMerchantFrequency(merchant: String, since: Long): Int
}

// FIX: renamed field from `merchant` to `name` to match alias in query (was `merchant as name`)
data class MerchantSummary(val name: String, val count: Int, val total: Double)

// FIX: added `count` field used by updateDailySummary for transactionCount
data class CategorySummary(val category: String, val total: Double, val count: Int)

// ─── Daily Summary DAO ────────────────────────────────────────────────────────

@Dao
interface DailySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: DailySummary)

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC")
    fun getAllSummaries(): Flow<List<DailySummary>>

    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    suspend fun getSummaryForDate(date: String): DailySummary?

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC LIMIT 7")
    fun getWeeklySummaries(): Flow<List<DailySummary>>

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC LIMIT 30")
    fun getMonthlySummaries(): Flow<List<DailySummary>>

    @Query("SELECT SUM(totalSpent) FROM daily_summaries WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalSpentInRange(startDate: String, endDate: String): Double?

    @Query("SELECT SUM(wasteSpent) FROM daily_summaries WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalWasteInRange(startDate: String, endDate: String): Double?
}

// ─── Goal DAO ─────────────────────────────────────────────────────────────────

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: Goal): Long

    @Update
    suspend fun update(goal: Goal)

    @Delete
    suspend fun delete(goal: Goal)

    @Query("SELECT * FROM goals WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("UPDATE goals SET savedAmount = savedAmount + :amount WHERE id = :goalId")
    suspend fun addSavings(goalId: Long, amount: Double)
}

// ─── Keyword DAO ──────────────────────────────────────────────────────────────

@Dao
interface KeywordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(keyword: Keyword): Long

    @Delete
    suspend fun delete(keyword: Keyword)

    @Query("SELECT * FROM keywords ORDER BY word ASC")
    fun getAllKeywords(): Flow<List<Keyword>>

    @Query("SELECT * FROM keywords WHERE type = :type ORDER BY word ASC")
    fun getKeywordsByType(type: String): Flow<List<Keyword>>

    @Query("SELECT COUNT(*) FROM keywords WHERE LOWER(word) = LOWER(:word)")
    suspend fun existsKeyword(word: String): Int
}

// ─── Streak DAO ───────────────────────────────────────────────────────────────

@Dao
interface StreakDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(streak: Streak)

    @Update
    suspend fun update(streak: Streak)

    @Query("SELECT * FROM streaks ORDER BY type ASC")
    fun getAllStreaks(): Flow<List<Streak>>

    @Query("SELECT * FROM streaks WHERE type = :type LIMIT 1")
    suspend fun getStreakByType(type: String): Streak?

    @Query("""
        UPDATE streaks
        SET currentCount = currentCount + 1,
            bestCount = MAX(bestCount, currentCount + 1),
            lastUpdated = :now
        WHERE type = :type
    """)
    suspend fun incrementStreak(type: String, now: Long = System.currentTimeMillis())

    @Query("""
        UPDATE streaks
        SET currentCount = 0,
            isActive = 0,
            lastUpdated = :now
        WHERE type = :type
    """)
    suspend fun resetStreak(type: String, now: Long = System.currentTimeMillis())
}

// ─── Voice Journal DAO ────────────────────────────────────────────────────────

@Dao
interface VoiceJournalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(journal: VoiceJournal): Long

    @Delete
    suspend fun delete(journal: VoiceJournal)

    @Query("SELECT * FROM voice_journals ORDER BY timestamp DESC")
    fun getAllJournals(): Flow<List<VoiceJournal>>

    @Query("SELECT * FROM voice_journals WHERE isFutureWarning = 1 LIMIT 1")
    suspend fun getFutureWarning(): VoiceJournal?
}

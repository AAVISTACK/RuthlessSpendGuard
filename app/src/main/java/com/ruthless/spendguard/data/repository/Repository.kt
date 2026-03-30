package com.ruthless.spendguard.data.repository

  import com.ruthless.spendguard.data.dao.CategorySummary
  import com.ruthless.spendguard.data.dao.MerchantSummary
  import com.ruthless.spendguard.data.dao.TransactionDao
  import com.ruthless.spendguard.data.entities.*
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.flowOf
  import kotlinx.coroutines.flow.map
  import java.util.*
  import javax.inject.Inject
  import javax.inject.Singleton

  @Singleton
  class SpendGuardRepository @Inject constructor(
      private val transactionDao: TransactionDao
  ) {

      // ─── Transactions ─────────────────────────────────────────────────────────

      suspend fun insertTransaction(transaction: Transaction): Long =
          transactionDao.insert(transaction)

      suspend fun updateTransaction(transaction: Transaction) =
          transactionDao.update(transaction)

      suspend fun deleteTransaction(id: Long) =
          transactionDao.softDelete(id)

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

      // ─── Goals (stubs — DAO removed) ──────────────────────────────────────────

      fun getActiveGoals(): Flow<List<Goal>> = flowOf(emptyList())
      fun getAllGoals(): Flow<List<Goal>> = flowOf(emptyList())
      suspend fun insertGoal(goal: Goal): Long = -1L
      suspend fun updateGoal(goal: Goal) {}
      suspend fun deleteGoal(goal: Goal) {}

      // ─── Keywords (stubs — DAO removed) ───────────────────────────────────────

      fun getAllKeywords(): Flow<List<Keyword>> = flowOf(emptyList())
      suspend fun addKeyword(keyword: Keyword): Long = -1L
      suspend fun deleteKeyword(keyword: Keyword) {}

      // ─── Streaks (stubs — DAO removed) ────────────────────────────────────────

      fun getAllStreaks(): Flow<List<Streak>> = flowOf(emptyList())
      suspend fun ensureStreaksExist() {}
      suspend fun incrementStreak(type: StreakType) {}
      suspend fun resetStreak(type: StreakType) {}

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
          val end = start + (7 * 86_400_000L) - 1
          return Pair(start, end)
      }
  }
  
package com.ruthless.spendguard.data.dao

  import androidx.room.*
  import com.ruthless.spendguard.data.entities.*
  import kotlinx.coroutines.flow.Flow

  data class MerchantSummary(val name: String, val count: Int, val total: Double)
  data class CategorySummary(val category: String, val total: Double, val count: Int)

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
  
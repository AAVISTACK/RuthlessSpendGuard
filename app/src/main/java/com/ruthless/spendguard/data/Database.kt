package com.ruthless.spendguard.data

  import androidx.room.Database
  import androidx.room.RoomDatabase
  import androidx.room.TypeConverters
  import com.ruthless.spendguard.data.dao.TransactionDao
  import com.ruthless.spendguard.data.entities.Transaction

  @Database(
      entities = [Transaction::class],
      version = 1,
      exportSchema = false
  )
  @TypeConverters(Converters::class)
  abstract class SpendGuardDatabase : RoomDatabase() {
      abstract fun transactionDao(): TransactionDao
  }
  
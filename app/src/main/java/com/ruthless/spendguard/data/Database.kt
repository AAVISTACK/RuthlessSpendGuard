package com.ruthless.spendguard.data

  import androidx.room.Database
  import androidx.room.RoomDatabase
  import androidx.room.TypeConverters
  import com.ruthless.spendguard.data.dao.TransactionDao
  import com.ruthless.spendguard.data.dao.DailySummaryDao
  import com.ruthless.spendguard.data.dao.GoalDao
  import com.ruthless.spendguard.data.dao.KeywordDao
  import com.ruthless.spendguard.data.dao.StreakDao
  import com.ruthless.spendguard.data.dao.VoiceJournalDao
  import com.ruthless.spendguard.data.entities.DailySummary
  import com.ruthless.spendguard.data.entities.Goal
  import com.ruthless.spendguard.data.entities.Keyword
  import com.ruthless.spendguard.data.entities.Streak
  import com.ruthless.spendguard.data.entities.Transaction
  import com.ruthless.spendguard.data.entities.VoiceJournal

  @Database(
      entities = [
          Transaction::class,
          DailySummary::class,
          Goal::class,
          Keyword::class,
          Streak::class,
          VoiceJournal::class
      ],
      version = 1,
      exportSchema = true
  )
  @TypeConverters(Converters::class)
  abstract class SpendGuardDatabase : RoomDatabase() {
      abstract fun transactionDao(): TransactionDao
      abstract fun dailySummaryDao(): DailySummaryDao
      abstract fun goalDao(): GoalDao
      abstract fun keywordDao(): KeywordDao
      abstract fun streakDao(): StreakDao
      abstract fun voiceJournalDao(): VoiceJournalDao
  }
  
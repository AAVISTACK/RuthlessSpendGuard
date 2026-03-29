package com.ruthless.spendguard.di

  import android.content.Context
  import androidx.room.Room
  import com.ruthless.spendguard.data.SpendGuardDatabase
  import com.ruthless.spendguard.data.UserPreferencesManager
  import com.ruthless.spendguard.data.dao.*
  import com.ruthless.spendguard.util.VoiceFeedbackManager
  import dagger.Module
  import dagger.Provides
  import dagger.hilt.InstallIn
  import dagger.hilt.android.qualifiers.ApplicationContext
  import dagger.hilt.components.SingletonComponent
  import javax.inject.Singleton

  @Module
  @InstallIn(SingletonComponent::class)
  object DatabaseModule {

      @Provides
      @Singleton
      fun provideDatabase(@ApplicationContext context: Context): SpendGuardDatabase {
          return Room.databaseBuilder(
              context,
              SpendGuardDatabase::class.java,
              "spend_guard_db"
          )
              .fallbackToDestructiveMigration()
              .build()
      }

      @Provides @Singleton fun provideTransactionDao(db: SpendGuardDatabase): TransactionDao = db.transactionDao()
      @Provides @Singleton fun provideDailySummaryDao(db: SpendGuardDatabase): DailySummaryDao = db.dailySummaryDao()
      @Provides @Singleton fun provideGoalDao(db: SpendGuardDatabase): GoalDao = db.goalDao()
      @Provides @Singleton fun provideKeywordDao(db: SpendGuardDatabase): KeywordDao = db.keywordDao()
      @Provides @Singleton fun provideStreakDao(db: SpendGuardDatabase): StreakDao = db.streakDao()
      @Provides @Singleton fun provideVoiceJournalDao(db: SpendGuardDatabase): VoiceJournalDao = db.voiceJournalDao()
  }

  @Module
  @InstallIn(SingletonComponent::class)
  object PreferencesModule {

      @Provides
      @Singleton
      fun provideUserPreferences(@ApplicationContext context: Context): UserPreferencesManager =
          UserPreferencesManager(context)
  }

  @Module
  @InstallIn(SingletonComponent::class)
  object AppModule {

      @Provides
      @Singleton
      fun provideVoiceFeedbackManager(@ApplicationContext context: Context): VoiceFeedbackManager =
          VoiceFeedbackManager(context)
  }
  
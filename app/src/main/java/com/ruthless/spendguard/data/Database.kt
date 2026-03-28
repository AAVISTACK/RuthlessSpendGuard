package com.ruthless.spendguard.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.ruthless.spendguard.data.dao.*
import com.ruthless.spendguard.data.entities.*

// ─── Type Converters ──────────────────────────────────────────────────────────

class Converters {

    @TypeConverter
    fun fromCategory(value: TransactionCategory): String = value.name

    // FIX: use `TransactionCategory.entries` (Kotlin 1.9+) instead of deprecated `.values()`
    @TypeConverter
    fun toCategory(value: String): TransactionCategory =
        TransactionCategory.entries.find { it.name == value } ?: TransactionCategory.UNKNOWN

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        TransactionType.entries.find { it.name == value } ?: TransactionType.NEUTRAL

    @TypeConverter
    fun fromKeywordType(value: KeywordType): String = value.name

    @TypeConverter
    fun toKeywordType(value: String): KeywordType =
        KeywordType.entries.find { it.name == value } ?: KeywordType.WASTE

    @TypeConverter
    fun fromStreakType(value: StreakType): String = value.name

    @TypeConverter
    fun toStreakType(value: String): StreakType =
        StreakType.entries.find { it.name == value } ?: StreakType.NO_JUNK
}

// ─── Room Database ────────────────────────────────────────────────────────────

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

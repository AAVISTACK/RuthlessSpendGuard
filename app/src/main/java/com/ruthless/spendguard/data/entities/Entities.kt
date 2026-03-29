package com.ruthless.spendguard.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Transaction Entity ───────────────────────────────────────────────────────

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val rawSms: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val category: TransactionCategory = TransactionCategory.UNKNOWN,
    val type: TransactionType = TransactionType.WASTE,
    val isManual: Boolean = false,
    val note: String = "",
    val isDeleted: Boolean = false
)

enum class TransactionCategory(val displayName: String, val emoji: String) {
    FOOD("Food & Drinks", "🍔"),
    CIGARETTE("Cigarettes/Pan", "🚬"),
    FUEL("Fuel/Petrol", "⛽"),
    SHOPPING("Shopping", "🛍️"),
    ENTERTAINMENT("Entertainment", "🎮"),
    TRANSPORT("Transport", "🚌"),
    MEDICAL("Medical", "💊"),
    UTILITY("Utilities", "💡"),
    JUNK_FOOD("Junk Food", "🍕"),
    CAFE("Cafe/Coffee", "☕"),
    COLD_DRINK("Cold Drink", "🥤"),
    ESSENTIAL("Essential", "✅"),
    UNKNOWN("Unknown", "❓")
}

enum class TransactionType {
    WASTE, ESSENTIAL, NEUTRAL
}

// ─── Daily Summary Entity ─────────────────────────────────────────────────────

@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey
    val date: String,
    val totalSpent: Double = 0.0,
    val wasteSpent: Double = 0.0,
    val essentialSpent: Double = 0.0,
    val dailyLimit: Double = 100.0,
    val transactionCount: Int = 0,
    val wasteCount: Int = 0,
    val streakMaintained: Boolean = false
)

// ─── Goal Entity ──────────────────────────────────────────────────────────────

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val deadline: Long? = null,
    val isCompleted: Boolean = false,
    val emoji: String = "🎯"
)

// ─── Keyword Entity ───────────────────────────────────────────────────────────

@Entity(tableName = "keywords")
data class Keyword(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val type: KeywordType,
    val category: TransactionCategory = TransactionCategory.UNKNOWN,
    val isUserAdded: Boolean = false
)

enum class KeywordType {
    WASTE, ESSENTIAL, IGNORE
}

// ─── Streak Entity ────────────────────────────────────────────────────────────

@Entity(tableName = "streaks")
data class Streak(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: StreakType,
    val currentCount: Int = 0,
    val bestCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

enum class StreakType(val displayName: String) {
    NO_JUNK("No Junk Food"),
    UNDER_BUDGET("Under Budget"),
    NO_CIGARETTE("No Cigarettes"),
    NO_COLD_DRINK("No Cold Drinks")
}

// ─── Voice Journal Entity ─────────────────────────────────────────────────────

@Entity(tableName = "voice_journals")
data class VoiceJournal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0,
    val note: String = "",
    val isFutureWarning: Boolean = false
)

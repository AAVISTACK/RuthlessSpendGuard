package com.ruthless.spendguard.data

  import androidx.room.TypeConverter
  import com.ruthless.spendguard.data.entities.KeywordType
  import com.ruthless.spendguard.data.entities.StreakType
  import com.ruthless.spendguard.data.entities.TransactionCategory
  import com.ruthless.spendguard.data.entities.TransactionType

  class Converters {
      @TypeConverter fun fromCategory(v: TransactionCategory): String = v.name
      @TypeConverter fun toCategory(v: String): TransactionCategory =
          TransactionCategory.entries.find { it.name == v } ?: TransactionCategory.UNKNOWN

      @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
      @TypeConverter fun toTransactionType(v: String): TransactionType =
          TransactionType.entries.find { it.name == v } ?: TransactionType.NEUTRAL

      @TypeConverter fun fromKeywordType(v: KeywordType): String = v.name
      @TypeConverter fun toKeywordType(v: String): KeywordType =
          KeywordType.entries.find { it.name == v } ?: KeywordType.WASTE

      @TypeConverter fun fromStreakType(v: StreakType): String = v.name
      @TypeConverter fun toStreakType(v: String): StreakType =
          StreakType.entries.find { it.name == v } ?: StreakType.NO_JUNK
  }
  
package com.ruthless.spendguard.util

import com.ruthless.spendguard.data.entities.TransactionCategory
import com.ruthless.spendguard.data.entities.TransactionType
import java.util.regex.Pattern

object SmsParser {

    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:Rs\\.?|INR|₹)"),
        Pattern.compile("(?:debited|credited|spent|paid)\\s+(?:Rs\\.?|INR|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?:amount|amt)\\s+(?:of\\s+)?(?:Rs\\.?|INR|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    )

    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("(?:at|to|@)\\s+([A-Za-z0-9\\s&'.\\-]{2,30})(?:\\s+on|\\s+via|\\s+using|\\.|,|\$)"),
        Pattern.compile("(?:VPA|UPI ID)\\s+([A-Za-z0-9@.\\-_]{3,50})"),
        Pattern.compile("(?:merchant|store)\\s*:?\\s*([A-Za-z0-9\\s&'.\\-]{2,30})"),
        Pattern.compile("(?:trf to|paid to|sent to)\\s+([A-Za-z0-9\\s&'.\\-]{2,30})")
    )

    val TRIGGER_KEYWORDS = listOf(
        "debited", "debit", "upi", "₹", "rs.", "inr",
        "spent", "paid", "transaction", "purchase", "charged"
    )

    val ESSENTIAL_KEYWORDS = listOf(
        "petrol", "fuel", "hp ", "bharat petroleum", "indian oil", "diesel",
        "electricity", "water bill", "insurance", "emi", "salary", "hospital",
        "medical", "pharmacy", "medicine", "school", "college", "fee",
        "rent", "maintenance", "gas cylinder", "lpg", "recharge"
    )

    val WASTE_KEYWORDS = listOf(
        "pan shop", "paan", "cigarette", "bidi", "tobacco", "gutka",
        "fast food", "burger", "pizza", "domino", "mcdonalds", "kfc",
        "cafe", "coffee", "starbucks", "cafe coffee", "barista",
        "cold drink", "pepsi", "coke", "sprite", "mountain dew", "redbull",
        "swiggy", "zomato", "food delivery",
        "chips", "wafers", "snack", "biscuit",
        "wine shop", "liquor", "beer", "alcohol",
        "gaming", "pubg", "free fire", "game",
        "pan masala", "masala", "supari"
    )

    data class ParsedTransaction(
        val amount: Double?,
        val merchant: String,
        val category: TransactionCategory,
        val type: TransactionType
    )

    fun isBankSms(sms: String): Boolean {
        val lower = sms.lowercase()
        return TRIGGER_KEYWORDS.any { lower.contains(it) } &&
            (lower.contains("debited") || lower.contains("₹") ||
             lower.contains("rs.") || lower.contains("inr"))
    }

    fun parse(sms: String): ParsedTransaction {
        val amount = extractAmount(sms)
        val merchant = extractMerchant(sms)
        val lower = sms.lowercase()

        val isEssential = ESSENTIAL_KEYWORDS.any { lower.contains(it) }
        val isWaste = WASTE_KEYWORDS.any { lower.contains(it) }

        val category = detectCategory(lower)
        val type = when {
            isEssential -> TransactionType.ESSENTIAL
            isWaste -> TransactionType.WASTE
            else -> TransactionType.NEUTRAL
        }

        return ParsedTransaction(amount, merchant, category, type)
    }

    private fun extractAmount(sms: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(sms)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                return amountStr.toDoubleOrNull()
            }
        }
        return null
    }

    fun extractMerchant(sms: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(sms)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim() ?: continue
                if (merchant.length >= 2) return merchant.take(50)
            }
        }
        val words = sms.split(" ")
        val capitalWord = words.firstOrNull {
            it.length > 3 && it[0].isUpperCase() && it != "INR" && it != "UPI"
        }
        return capitalWord ?: "Unknown Merchant"
    }

    // FIX: was `ESSENTIAL_KEYWORDS.any { lower.contains("petrol") || ... }` which always
    // evaluated the lambda body as a constant — changed to use the keyword variable `it`.
    private fun detectCategory(lower: String): TransactionCategory = when {
        lower.contains("petrol") || lower.contains("fuel") || lower.contains("diesel") ->
            TransactionCategory.FUEL
        lower.contains("cigarette") || lower.contains("pan shop") ||
            lower.contains("tobacco") || lower.contains("bidi") ->
            TransactionCategory.CIGARETTE
        lower.contains("swiggy") || lower.contains("zomato") || lower.contains("burger") ||
            lower.contains("pizza") || lower.contains("kfc") || lower.contains("mcdonalds") ->
            TransactionCategory.JUNK_FOOD
        lower.contains("cafe") || lower.contains("coffee") || lower.contains("starbucks") ->
            TransactionCategory.CAFE
        lower.contains("cold drink") || lower.contains("pepsi") ||
            lower.contains("coke") || lower.contains("sprite") ->
            TransactionCategory.COLD_DRINK
        lower.contains("medical") || lower.contains("pharmacy") ||
            lower.contains("hospital") || lower.contains("medicine") ->
            TransactionCategory.MEDICAL
        lower.contains("uber") || lower.contains("ola") || lower.contains("auto") ||
            lower.contains("metro") || lower.contains("bus") ->
            TransactionCategory.TRANSPORT
        lower.contains("electricity") || lower.contains("water") ||
            lower.contains("internet") || lower.contains("recharge") ->
            TransactionCategory.UTILITY
        lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") ||
            lower.contains("shop") || lower.contains("store") ->
            TransactionCategory.SHOPPING
        else -> TransactionCategory.UNKNOWN
    }

    fun classifyWithCustomKeywords(
        sms: String,
        wasteKeywords: List<String>,
        essentialKeywords: List<String>
    ): TransactionType {
        val lower = sms.lowercase()
        val isEssential = essentialKeywords.any { lower.contains(it.lowercase()) }
        val isWaste = wasteKeywords.any { lower.contains(it.lowercase()) }
        return when {
            isEssential -> TransactionType.ESSENTIAL
            isWaste -> TransactionType.WASTE
            else -> TransactionType.NEUTRAL
        }
    }
}

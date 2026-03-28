package com.ruthless.spendguard.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

enum class VoiceMode { RUTHLESS, CALM, FUNNY }

class VoiceFeedbackManager @Inject constructor(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    // ─── Content banks ────────────────────────────────────────────────────────

    private val ruthlessLines = listOf(
        "Ye kya kar raha hai tu? Teri wealth kahan ja rahi hai?",
        "Ek aur waste. Teri future self ro rahi hai.",
        "Ruk ja. Soch. Kya ye zaroori tha?",
        "Paise gaye. Wapas nahi aayenge."
    )

    private val calmLines = listOf(
        "You're doing okay. Stay mindful.",
        "Every rupee saved is a step forward.",
        "Pause. Think. Is this aligned with your goals?",
        "You are in control."
    )

    private val funnyLines = listOf(
        "Bhai, tera wallet ro raha hai!",
        "ATM ne bola — mat karo bhai, mat karo!",
        "Future self ne WhatsApp kiya: please ruko!",
        "Ye paise nahi, sapne hain!"
    )

    private val limitExceededLines = listOf(
        "Bas! Limit cross ho gayi. Aaj ke liye khatam.",
        "No more spending today. You're done.",
        "Budget gaya. Kal fresh start."
    )

    private val goodControlLines = listOf(
        "Excellent control. Keep it up.",
        "Wah! Aaj ka din accha raha.",
        "Discipline = freedom. You're on track."
    )

    private val impulseDetectedLines = listOf(
        "Ruk. 5 minute soch. Zaroor kharidna hai?",
        "Impulse detected. Wait it out.",
        "Is this need or habit? Think before you tap."
    )

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    fun initialize() {
        if (isInitialized) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
                isInitialized = true
            }
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun speak(text: String, mode: VoiceMode = VoiceMode.CALM) {
        if (!isInitialized || tts == null) return
        tts?.stop()

        when (mode) {
            VoiceMode.RUTHLESS -> {
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(0.85f)
            }
            VoiceMode.CALM -> {
                tts?.setSpeechRate(0.9f)
                tts?.setPitch(1.0f)
            }
            VoiceMode.FUNNY -> {
                tts?.setSpeechRate(1.1f)
                tts?.setPitch(1.15f)
            }
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun speakNewTransaction(amount: Double, merchant: String, isWaste: Boolean, mode: VoiceMode) {
        val amountText = "₹${amount.toInt()}"
        val msg = if (isWaste) {
            when (mode) {
                VoiceMode.RUTHLESS -> "Phir se! $amountText gaya $merchant pe. ${ruthlessLines.random()}"
                VoiceMode.CALM -> "$amountText spent at $merchant. ${calmLines.random()}"
                VoiceMode.FUNNY -> "$amountText gaya $merchant me! ${funnyLines.random()}"
            }
        } else {
            when (mode) {
                VoiceMode.RUTHLESS -> "$amountText kharcha hua. Essential hai toh theek hai."
                VoiceMode.CALM -> "$amountText spent. ${goodControlLines.random()}"
                VoiceMode.FUNNY -> "$amountText. Acchi jagah gaya at least!"
            }
        }
        speak(msg, mode)
    }

    fun speakForMode(mode: VoiceMode) {
        when (mode) {
            VoiceMode.RUTHLESS -> speak(ruthlessLines.random(), mode)
            VoiceMode.CALM -> speak(calmLines.random(), mode)
            VoiceMode.FUNNY -> speak(funnyLines.random(), mode)
        }
    }

    fun speakLimitExceeded() = speak(limitExceededLines.random(), VoiceMode.RUTHLESS)
    fun speakGoodControl() = speak(goodControlLines.random(), VoiceMode.CALM)
    fun speakImpulseDetected() = speak(impulseDetectedLines.random(), VoiceMode.RUTHLESS)

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

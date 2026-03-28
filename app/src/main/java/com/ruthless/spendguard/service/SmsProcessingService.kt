package com.ruthless.spendguard.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ruthless.spendguard.MainActivity
import com.ruthless.spendguard.R
import com.ruthless.spendguard.data.entities.Transaction
import com.ruthless.spendguard.data.repository.SpendGuardRepository
import com.ruthless.spendguard.di.UserPreferencesManager
import com.ruthless.spendguard.util.SmsParser
import com.ruthless.spendguard.util.VoiceFeedbackManager
import com.ruthless.spendguard.util.VoiceMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class SmsProcessingService : Service() {

    @Inject lateinit var repository: SpendGuardRepository
    @Inject lateinit var preferences: UserPreferencesManager
    @Inject lateinit var voiceFeedback: VoiceFeedbackManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val EXTRA_SMS_BODY = "sms_body"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val CHANNEL_ID = "sms_processing"
        const val CHANNEL_ALERT_ID = "spend_alerts"
        const val NOTIF_ID_PROCESSING = 1001
        const val NOTIF_ID_ALERT = 1002
        const val NOTIF_ID_LOCKDOWN = 1003
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        voiceFeedback.initialize()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val smsBody = intent?.getStringExtra(EXTRA_SMS_BODY) ?: run {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val sender = intent.getStringExtra(EXTRA_SENDER) ?: ""
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())

        startForeground(NOTIF_ID_PROCESSING, buildProcessingNotification())

        serviceScope.launch {
            try {
                processSms(smsBody, sender, timestamp)
            } catch (e: Exception) {
                // Log error silently — service must never crash on bad SMS
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    // FIX: Added onDestroy to cancel the coroutine scope and avoid memory leaks.
    // Previously the service had no cleanup — coroutines would leak after the service stopped.
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun processSms(smsBody: String, sender: String, timestamp: Long) {
        if (!SmsParser.isBankSms(smsBody)) return

        val parsed = SmsParser.parse(smsBody)
        val amount = parsed.amount ?: return

        val keywords = repository.getAllKeywords().first()
        val wasteKw = keywords.filter { it.type.name == "WASTE" }.map { it.word }
        val essentialKw = keywords.filter { it.type.name == "ESSENTIAL" }.map { it.word }
        val finalType = SmsParser.classifyWithCustomKeywords(smsBody, wasteKw, essentialKw)

        val transaction = Transaction(
            amount = amount,
            merchant = parsed.merchant,
            rawSms = smsBody,
            timestamp = timestamp,
            category = parsed.category,
            type = finalType
        )
        repository.insertTransaction(transaction)

        val todayTotal = repository.getTodayTotal().first()
        val dailyLimit = preferences.dailyLimit.first()
        val voiceEnabled = preferences.voiceEnabled.first()
        val voiceMode = preferences.voiceMode.first()
        val isWaste = finalType.name == "WASTE"

        if (voiceEnabled) {
            withContext(Dispatchers.Main) {
                voiceFeedback.speakNewTransaction(amount, parsed.merchant, isWaste, voiceMode)
            }
        }

        val usagePercent = if (dailyLimit > 0) todayTotal / dailyLimit else 0.0
        when {
            usagePercent >= 1.0 -> handleLimitExceeded(dailyLimit, voiceEnabled, voiceMode)
            usagePercent >= 0.7 -> handleWarning70(todayTotal, dailyLimit)
        }
    }

    private suspend fun handleLimitExceeded(limit: Double, voiceEnabled: Boolean, voiceMode: VoiceMode) {
        showAlertNotification(
            title = "Budget Limit Exceeded",
            message = "You've crossed your ₹${limit.toLong()} daily limit!"
        )
        if (voiceEnabled) {
            withContext(Dispatchers.Main) { voiceFeedback.speakLimitExceeded() }
        }
    }

    private fun handleWarning70(total: Double, limit: Double) {
        showAlertNotification(
            title = "Spending Warning",
            message = "₹${total.toLong()} of ₹${limit.toLong()} used — slow down!"
        )
    }

    private fun showAlertNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_ALERT, notification)
    }

    private fun buildProcessingNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rupee)
            .setContentTitle("SpendGuard")
            .setContentText("Processing transaction...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(
            CHANNEL_ID,
            "SMS Processing",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Background SMS transaction processing" }
            .also { nm.createNotificationChannel(it) }

        NotificationChannel(
            CHANNEL_ALERT_ID,
            "Spend Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Budget limit and spending alerts" }
            .also { nm.createNotificationChannel(it) }
    }
}

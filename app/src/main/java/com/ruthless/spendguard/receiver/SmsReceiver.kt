package com.ruthless.spendguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ruthless.spendguard.service.SmsProcessingService

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Group multi-part messages by originating address
        val grouped = messages.groupBy { it.originatingAddress ?: "" }

        for ((sender, parts) in grouped) {
            val body = parts.joinToString("") { it.messageBody ?: "" }
            if (body.isBlank()) continue

            val serviceIntent = Intent(context, SmsProcessingService::class.java).apply {
                putExtra(SmsProcessingService.EXTRA_SMS_BODY, body)
                putExtra(SmsProcessingService.EXTRA_SENDER, sender)
                putExtra(SmsProcessingService.EXTRA_TIMESTAMP, parts.first().timestampMillis)
            }
            context.startForegroundService(serviceIntent)
        }
    }
}

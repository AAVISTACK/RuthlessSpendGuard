package com.ruthless.spendguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruthless.spendguard.di.UserPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LockdownReceiver : BroadcastReceiver() {

    @Inject lateinit var preferences: UserPreferencesManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.ruthless.spendguard.LOCKDOWN_ACTION") {
            scope.launch {
                preferences.setLockdownActive(true)
            }
        }
    }
}

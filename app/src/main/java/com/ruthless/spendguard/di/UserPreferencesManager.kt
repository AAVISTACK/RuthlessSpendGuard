package com.ruthless.spendguard.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ruthless.spendguard.util.VoiceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spend_guard_prefs")

class UserPreferencesManager(private val context: Context) {

    companion object {
        val DAILY_LIMIT            = doublePreferencesKey("daily_limit")
        val VOICE_MODE             = stringPreferencesKey("voice_mode")
        val VOICE_ENABLED          = booleanPreferencesKey("voice_enabled")
        val LOCKDOWN_ENABLED       = booleanPreferencesKey("lockdown_enabled")
        val LOCKDOWN_ACTIVE        = booleanPreferencesKey("lockdown_active")
        val URGE_DELAY_MINUTES     = intPreferencesKey("urge_delay_minutes")
        val PIN_ENABLED            = booleanPreferencesKey("pin_enabled")
        val PIN_HASH               = stringPreferencesKey("pin_hash")
        val ONBOARDING_DONE        = booleanPreferencesKey("onboarding_done")
        val PRO_UNLOCKED           = booleanPreferencesKey("pro_unlocked")
        val NOTIFICATION_ENABLED   = booleanPreferencesKey("notification_enabled")
        val REALITY_CHECK_ENABLED  = booleanPreferencesKey("reality_check_enabled")
        val DOPAMINE_MODE_ACTIVE   = booleanPreferencesKey("dopamine_mode_active")
        val IMPULSE_DELAY_ENABLED  = booleanPreferencesKey("impulse_delay_enabled")
        val SHAME_SCREEN_ENABLED   = booleanPreferencesKey("shame_screen_enabled")
        val AI_INSIGHTS_ENABLED    = booleanPreferencesKey("ai_insights_enabled")
    }

    private val dataFlow = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    val dailyLimit: Flow<Double> = dataFlow.map { it[DAILY_LIMIT] ?: 500.0 }

    val voiceMode: Flow<VoiceMode> = dataFlow.map { prefs ->
        try { VoiceMode.valueOf(prefs[VOICE_MODE] ?: VoiceMode.RUTHLESS.name) }
        catch (_: IllegalArgumentException) { VoiceMode.RUTHLESS }
    }

    val voiceEnabled: Flow<Boolean> = dataFlow.map { it[VOICE_ENABLED] ?: true }
    val lockdownEnabled: Flow<Boolean> = dataFlow.map { it[LOCKDOWN_ENABLED] ?: false }
    val lockdownActive: Flow<Boolean> = dataFlow.map { it[LOCKDOWN_ACTIVE] ?: false }
    val urgeDelayMinutes: Flow<Int> = dataFlow.map { it[URGE_DELAY_MINUTES] ?: 5 }
    val pinEnabled: Flow<Boolean> = dataFlow.map { it[PIN_ENABLED] ?: false }
    val pinHash: Flow<String> = dataFlow.map { it[PIN_HASH] ?: "" }
    val onboardingDone: Flow<Boolean> = dataFlow.map { it[ONBOARDING_DONE] ?: false }
    val proUnlocked: Flow<Boolean> = dataFlow.map { it[PRO_UNLOCKED] ?: false }
    val notificationEnabled: Flow<Boolean> = dataFlow.map { it[NOTIFICATION_ENABLED] ?: true }
    val realityCheckEnabled: Flow<Boolean> = dataFlow.map { it[REALITY_CHECK_ENABLED] ?: false }
    val dopamineModeActive: Flow<Boolean> = dataFlow.map { it[DOPAMINE_MODE_ACTIVE] ?: false }
    val impulseDelayEnabled: Flow<Boolean> = dataFlow.map { it[IMPULSE_DELAY_ENABLED] ?: false }
    val shameScreenEnabled: Flow<Boolean> = dataFlow.map { it[SHAME_SCREEN_ENABLED] ?: false }
    val aiInsightsEnabled: Flow<Boolean> = dataFlow.map { it[AI_INSIGHTS_ENABLED] ?: false }

    // ─── Setters ──────────────────────────────────────────────────────────────

    suspend fun setDailyLimit(limit: Double) = edit { it[DAILY_LIMIT] = limit }
    suspend fun setVoiceMode(mode: VoiceMode) = edit { it[VOICE_MODE] = mode.name }
    suspend fun setVoiceEnabled(enabled: Boolean) = edit { it[VOICE_ENABLED] = enabled }
    suspend fun setLockdownEnabled(enabled: Boolean) = edit { it[LOCKDOWN_ENABLED] = enabled }
    suspend fun setLockdownActive(active: Boolean) = edit { it[LOCKDOWN_ACTIVE] = active }
    suspend fun setUrgeDelayMinutes(minutes: Int) = edit { it[URGE_DELAY_MINUTES] = minutes }
    suspend fun setPinEnabled(enabled: Boolean) = edit { it[PIN_ENABLED] = enabled }
    suspend fun setPinHash(hash: String) = edit { it[PIN_HASH] = hash }
    suspend fun setOnboardingDone(done: Boolean) = edit { it[ONBOARDING_DONE] = done }
    suspend fun setProUnlocked(unlocked: Boolean) = edit { it[PRO_UNLOCKED] = unlocked }
    suspend fun setNotificationEnabled(enabled: Boolean) = edit { it[NOTIFICATION_ENABLED] = enabled }
    suspend fun setRealityCheckEnabled(enabled: Boolean) = edit { it[REALITY_CHECK_ENABLED] = enabled }
    suspend fun setDopamineModeActive(active: Boolean) = edit { it[DOPAMINE_MODE_ACTIVE] = active }
    suspend fun setImpulseDelayEnabled(enabled: Boolean) = edit { it[IMPULSE_DELAY_ENABLED] = enabled }
    suspend fun setShameScreenEnabled(enabled: Boolean) = edit { it[SHAME_SCREEN_ENABLED] = enabled }
    suspend fun setAiInsightsEnabled(enabled: Boolean) = edit { it[AI_INSIGHTS_ENABLED] = enabled }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().also(block)
        }
    }
}

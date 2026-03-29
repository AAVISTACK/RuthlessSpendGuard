package com.ruthless.spendguard.data

  import android.content.Context
  import androidx.datastore.core.DataStore
  import androidx.datastore.preferences.core.Preferences
  import androidx.datastore.preferences.core.booleanPreferencesKey
  import androidx.datastore.preferences.core.doublePreferencesKey
  import androidx.datastore.preferences.core.edit
  import androidx.datastore.preferences.core.emptyPreferences
  import androidx.datastore.preferences.core.stringPreferencesKey
  import androidx.datastore.preferences.preferencesDataStore
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.catch
  import kotlinx.coroutines.flow.map

  private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

  class UserPreferencesManager(private val context: Context) {

      companion object {
          val DAILY_LIMIT = doublePreferencesKey("daily_limit")
          val LOCKDOWN_ENABLED = booleanPreferencesKey("lockdown_enabled")
          val LOCKDOWN_THRESHOLD = doublePreferencesKey("lockdown_threshold")
          val CURRENCY = stringPreferencesKey("currency")
      }

      // FIX: Extracted single dataFlow to avoid duplicated catch{} blocks on every getter
      private val dataFlow: Flow<Preferences> = context.dataStore.data
          .catch { emit(emptyPreferences()) }

      // FIX: default dailyLimit changed from 100.0 to 500.0 (₹100 is unrealistically low)
      val dailyLimit: Flow<Double> = dataFlow.map { it[DAILY_LIMIT] ?: 500.0 }
      val lockdownEnabled: Flow<Boolean> = dataFlow.map { it[LOCKDOWN_ENABLED] ?: false }
      val lockdownThreshold: Flow<Double> = dataFlow.map { it[LOCKDOWN_THRESHOLD] ?: 400.0 }
      val currency: Flow<String> = dataFlow.map { it[CURRENCY] ?: "INR" }

      suspend fun setDailyLimit(value: Double) {
          context.dataStore.edit { it[DAILY_LIMIT] = value }
      }

      suspend fun setLockdownEnabled(value: Boolean) {
          context.dataStore.edit { it[LOCKDOWN_ENABLED] = value }
      }

      suspend fun setLockdownThreshold(value: Double) {
          context.dataStore.edit { it[LOCKDOWN_THRESHOLD] = value }
      }

      suspend fun setCurrency(value: String) {
          context.dataStore.edit { it[CURRENCY] = value }
      }
  }
  
package com.android.fingram.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vyapaar_prefs")

class AppPreferences(private val context: Context) {

    private object Keys {
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val LANGUAGE_SELECTED = booleanPreferencesKey("language_selected")
        val LANGUAGE = stringPreferencesKey("language")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val THEME = stringPreferencesKey("theme")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_7_DAYS = booleanPreferencesKey("reminder_7_days")
        val REMINDER_3_DAYS = booleanPreferencesKey("reminder_3_days")
        val REMINDER_1_DAY = booleanPreferencesKey("reminder_1_day")
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.FIRST_LAUNCH] ?: true
    }

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LANGUAGE] ?: "en"
    }

    val currencySymbol: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENCY_SYMBOL] ?: "$"
    }

    val isLanguageSelected: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LANGUAGE_SELECTED] ?: false
    }

    val isPinEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PIN_ENABLED] ?: false
    }

    val pinHash: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.PIN_HASH]
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BIOMETRIC_ENABLED] ?: false
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val reminder7DaysEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.REMINDER_7_DAYS] ?: true
    }

    val reminder3DaysEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.REMINDER_3_DAYS] ?: true
    }

    val reminder1DayEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.REMINDER_1_DAY] ?: true
    }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { it[Keys.FIRST_LAUNCH] = false }
    }

    suspend fun setLanguage(code: String) {
        context.dataStore.edit { 
            it[Keys.LANGUAGE] = code
            it[Keys.LANGUAGE_SELECTED] = true
        }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit {
            it[Keys.CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun hasLanguageSelected(): Boolean = isLanguageSelected.first()

    suspend fun setPinEnabled(enabled: Boolean, hash: String? = null) {
        context.dataStore.edit {
            it[Keys.PIN_ENABLED] = enabled
            if (hash != null) it[Keys.PIN_HASH] = hash
            if (!enabled) it.remove(Keys.PIN_HASH)
        }
    }

    suspend fun setPinHash(hash: String) {
        context.dataStore.edit { it[Keys.PIN_HASH] = hash }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setReminder7DaysEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMINDER_7_DAYS] = enabled }
    }

    suspend fun setReminder3DaysEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMINDER_3_DAYS] = enabled }
    }

    suspend fun setReminder1DayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMINDER_1_DAY] = enabled }
    }

    suspend fun getLanguage(): String = language.first()
    suspend fun getCurrencySymbol(): String = currencySymbol.first()
    suspend fun getPinHash(): String? = pinHash.first()
    suspend fun getBiometricEnabled(): Boolean = isBiometricEnabled.first()

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}

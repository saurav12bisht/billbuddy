package com.android.billreminder.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.data.export.ExportImportManager
import com.android.billreminder.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

data class ReminderSettingsUiState(
    val sevenDays: Boolean = true,
    val threeDays: Boolean = true,
    val oneDay: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val appPreferences: AppPreferences,
    private val exportImportManager: ExportImportManager
) : ViewModel() {

    // exportResult moved to SummaryViewModel

    private val _importResult = MutableStateFlow<Result<ExportImportManager.ImportResult>?>(null)
    val importResult = _importResult.asStateFlow()

    private val _reminderSettings = MutableStateFlow(ReminderSettingsUiState())
    val reminderSettings = _reminderSettings.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferences.reminder7DaysEnabled.collect { enabled ->
                _reminderSettings.value = _reminderSettings.value.copy(sevenDays = enabled)
            }
        }
        viewModelScope.launch {
            appPreferences.reminder3DaysEnabled.collect { enabled ->
                _reminderSettings.value = _reminderSettings.value.copy(threeDays = enabled)
            }
        }
        viewModelScope.launch {
            appPreferences.reminder1DayEnabled.collect { enabled ->
                _reminderSettings.value = _reminderSettings.value.copy(oneDay = enabled)
            }
        }
    }

    fun resetImportResult() {
        _importResult.value = null
    }

    fun setLanguage(code: String) {
        viewModelScope.launch { appPreferences.setLanguage(code) }
    }

    fun setPinEnabled(enabled: Boolean, pin: String? = null) {
        viewModelScope.launch {
            val hash = pin?.let { sha256(it) }
            appPreferences.setPinEnabled(enabled, hash)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setBiometricEnabled(enabled)
        }
    }

    fun backup(context: Context) {
        viewModelScope.launch {

        }
    }

    fun setReminder7Days(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setReminder7DaysEnabled(enabled) }
    }

    fun setReminder3Days(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setReminder3DaysEnabled(enabled) }
    }

    fun setReminder1Day(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setReminder1DayEnabled(enabled) }
    }

    // exportTransactions moved to SummaryViewModel

    fun importTransactions(uri: Uri) {
        viewModelScope.launch {
            _importResult.value = exportImportManager.importFromExcel(uri)
        }
    }

    private fun sha256(s: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

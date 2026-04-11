package com.android.billreminder.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class PinLockViewModel @Inject constructor(
    val appPreferences: AppPreferences
) : ViewModel() {

    private val _currentLength = MutableLiveData(0)
    val currentLength: LiveData<Int> = _currentLength

    private val _pinEntered = MutableLiveData<Boolean>()
    val pinEntered: LiveData<Boolean> = _pinEntered

    private val _wrongPin = MutableLiveData<Unit>()
    val wrongPin: LiveData<Unit> = _wrongPin

    private val sb = StringBuilder()

    init {
        viewModelScope.launch {
            if (appPreferences.getPinHash().isNullOrBlank()) {
                _pinEntered.value = true
            }
        }
    }

    private var failedAttempts = 0
    private val _lockoutTimeRemaining = MutableLiveData<Long>(0)
    val lockoutTimeRemaining: LiveData<Long> = _lockoutTimeRemaining

    fun addDigit(d: Int) {
        if (isLockedOut()) return
        if (sb.length >= 4) return
        sb.append(d)
        _currentLength.value = sb.length
        if (sb.length == 4) verifyPin()
    }
    fun backspace() {
        if (sb.isNotEmpty()) {
            sb.deleteCharAt(sb.length - 1)
            _currentLength.value = sb.length
        }
    }

    fun authenticateSuccess() {
        _pinEntered.value = true
    }
    private fun isLockedOut(): Boolean {
        return (_lockoutTimeRemaining.value ?: 0L) > 0L
    }

    private fun verifyPin() {
        viewModelScope.launch {
            if (isLockedOut()) return@launch

            val hash = appPreferences.getPinHash() ?: run {
                sb.clear()
                _currentLength.value = 0
                _wrongPin.value = Unit
                handleWrongPin()
                return@launch
            }
            val inputHash = sha256(sb.toString())
            if (inputHash == hash) {
                failedAttempts = 0
                _pinEntered.value = true
            } else {
                sb.clear()
                _currentLength.value = 0
                _wrongPin.value = Unit
                handleWrongPin()
            }
        }
    }

    private fun handleWrongPin() {
        failedAttempts++
        if (failedAttempts >= 3) {
            startLockoutTimer()
        }
    }

    private fun startLockoutTimer() {
        viewModelScope.launch {
            var remaining = 10000L // 10 seconds
            while (remaining > 0) {
                _lockoutTimeRemaining.value = remaining
                kotlinx.coroutines.delay(1000)
                remaining -= 1000
            }
            _lockoutTimeRemaining.value = 0
            failedAttempts = 0 // Reset attempts after lockout
        }
    }

    private fun sha256(s: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

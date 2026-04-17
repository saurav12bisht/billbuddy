package com.android.fingram.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.fingram.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

enum class SetPinStep {
    ENTER_PIN,
    CONFIRM_PIN
}

@HiltViewModel
class SetPinViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _currentStep = MutableLiveData(SetPinStep.ENTER_PIN)
    val currentStep: LiveData<SetPinStep> = _currentStep

    private val _currentLength = MutableLiveData(0)
    val currentLength: LiveData<Int> = _currentLength

    private val _pinMismatch = MutableLiveData<Unit>()
    val pinMismatch: LiveData<Unit> = _pinMismatch

    private val _pinSaved = MutableLiveData<Boolean>()
    val pinSaved: LiveData<Boolean> = _pinSaved

    private val firstPin = StringBuilder()
    private val secondPin = StringBuilder()
    private val currentPin: StringBuilder
        get() = if (_currentStep.value == SetPinStep.ENTER_PIN) firstPin else secondPin

    private var transitionUnlockTime = 0L

    fun addDigit(d: Int) {
        val now = System.currentTimeMillis()
        // Only block if we are in the "cooldown" period after a step transition
        if (now < transitionUnlockTime) return 

        val pin = currentPin
        if (pin.length >= 4) return
        pin.append(d)
        _currentLength.value = pin.length
        
        if (pin.length == 4) {
            when (_currentStep.value) {
                SetPinStep.ENTER_PIN -> {
                    // Move to confirm step
                    _currentStep.value = SetPinStep.CONFIRM_PIN
                    _currentLength.value = 0
                    secondPin.clear() // Ensure second pin is clean
                    // Add delay to prevent immediate input on next screen (phantom touches)
                    transitionUnlockTime = System.currentTimeMillis() + 500 
                }
                SetPinStep.CONFIRM_PIN -> {
                    // Validate PINs match
                    if (firstPin.toString() == secondPin.toString()) {
                        // PINs match, ready to save - save button will be enabled
                    } else {
                        // PINs don't match
                        _pinMismatch.value = Unit
                    }
                }

                null -> { /* Should not happen */ }
            }
        }
    }

    fun backspace() {
        val pin = currentPin
        if (pin.isNotEmpty()) {
            pin.deleteCharAt(pin.length - 1)
            _currentLength.value = pin.length
        }
    }

    fun canSave(): Boolean {
        return _currentStep.value == SetPinStep.CONFIRM_PIN &&
                firstPin.length == 4 &&
                secondPin.length == 4 &&
                firstPin.toString() == secondPin.toString()
    }

    fun getCurrentLength(): Int {
        return currentPin.length
    }

    private var isSaving = false

    fun savePin() {
        if (canSave() && !isSaving) {
            isSaving = true
            viewModelScope.launch {
                try {
                    val pin = firstPin.toString()
                    val hash = sha256(pin)
                    appPreferences.setPinEnabled(true, hash)
                    _pinSaved.value = true
                } catch (e: Exception) {
                    isSaving = false
                    // Handle error?
                }
            }
        }
    }

    fun resetToEnterPin() {
        firstPin.clear()
        secondPin.clear()
        _currentStep.value = SetPinStep.ENTER_PIN
        _currentLength.value = 0
    }

    private fun sha256(s: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

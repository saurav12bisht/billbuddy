package com.mobile.fingram.ui.splash

import androidx.lifecycle.ViewModel
import com.mobile.fingram.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    val appPreferences: AppPreferences
) : ViewModel() {

    suspend fun clearPinEnabled() {
        appPreferences.setPinEnabled(false)
    }
}

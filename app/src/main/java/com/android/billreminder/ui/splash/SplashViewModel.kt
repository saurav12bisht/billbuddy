package com.android.billreminder.ui.splash

import androidx.lifecycle.ViewModel
import com.android.billreminder.data.preferences.AppPreferences
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

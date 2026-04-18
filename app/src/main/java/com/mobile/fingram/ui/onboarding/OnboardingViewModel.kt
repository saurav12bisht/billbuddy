package com.mobile.fingram.ui.onboarding

import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.appcompat.app.AppCompatDelegate
import com.mobile.fingram.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    val appPreferences: AppPreferences
) : ViewModel() {

    suspend fun completeOnboarding() {
        appPreferences.setFirstLaunchComplete()
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            appPreferences.setLanguage(code)
            val locale = LocaleListCompat.forLanguageTags(code)
            AppCompatDelegate.setApplicationLocales(locale)
        }
    }
}

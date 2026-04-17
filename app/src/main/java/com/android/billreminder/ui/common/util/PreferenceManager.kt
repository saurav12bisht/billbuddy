package com.android.billreminder.ui.common.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fingram_prefs", Context.MODE_PRIVATE)

    var isCreditCardEducationShown: Boolean
        get() = prefs.getBoolean(KEY_CC_EDUCATION_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_CC_EDUCATION_SHOWN, value).apply()

    var isCCTransactionTutorialShown: Boolean
        get() = prefs.getBoolean(KEY_CC_TX_TUTORIAL_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_CC_TX_TUTORIAL_SHOWN, value).apply()

    companion object {
        private const val KEY_CC_EDUCATION_SHOWN = "is_cc_education_shown"
        private const val KEY_CC_TX_TUTORIAL_SHOWN = "is_cc_tx_tutorial_shown"
    }
}

package com.android.billreminder

import android.app.Application
import com.android.billreminder.ui.common.util.AdManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BillReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AdManager.initialize(this)
    }
}

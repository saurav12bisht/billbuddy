package com.android.billreminder

import android.app.Application
import com.android.billreminder.ui.common.util.AdManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.work.*
import com.android.billreminder.worker.CreditCardReminderWorker
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class BillReminderApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        AdManager.initialize(this)
        scheduleCreditCardReminders()
    }

    private fun scheduleCreditCardReminders() {
        val request = PeriodicWorkRequestBuilder<CreditCardReminderWorker>(24, TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .addTag("cc_reminders")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "credit_card_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

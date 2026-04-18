package com.mobile.fingram

import android.app.Application
import com.mobile.fingram.data.preferences.AppPreferences
import com.mobile.fingram.ui.common.util.AdManager
import com.mobile.fingram.ui.common.util.CurrencyFormatter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.work.*
import com.mobile.fingram.worker.CreditCardReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class Fingram : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    @Inject
    lateinit var appPreferences: AppPreferences

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            appPreferences.currencySymbol.collect { symbol ->
                CurrencyFormatter.setCurrencySymbol(symbol)
            }
        }
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

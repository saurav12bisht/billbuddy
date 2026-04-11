package com.android.billreminder.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.billreminder.R
import com.android.billreminder.data.repository.TransactionRepository
import com.android.billreminder.ui.common.util.CurrencyFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PaymentReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionRepo: TransactionRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val customerId = inputData.getInt("customer_id", -1)
        val customerName = inputData.getString("customer_name") ?: return Result.failure()
        val amountPaise = inputData.getLong("amount_paise", 0L)
        val channelId = "payment_reminder"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(channelId, "Payment Reminders", NotificationManager.IMPORTANCE_HIGH))
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_rupee_notif)
            .setContentTitle("Payment Due: $customerName")
            .setContentText("Outstanding: ${CurrencyFormatter.formatPaiseToRupee(amountPaise)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(customerId, notification)
        return Result.success()
    }
}

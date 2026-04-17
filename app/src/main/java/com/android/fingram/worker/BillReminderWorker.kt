package com.android.fingram.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.fingram.R
import com.android.fingram.data.repository.BillRepository
import com.android.fingram.ui.common.util.CurrencyFormatter
import com.android.fingram.ui.common.util.DateFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BillReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val billRepository: BillRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val billId = inputData.getLong(BillReminderScheduler.KEY_BILL_ID, -1L)
        val daysBefore = inputData.getInt(BillReminderScheduler.KEY_DAYS_BEFORE, 0)
        if (billId <= 0L) return Result.failure()

        val bill = billRepository.getBillById(billId) ?: return Result.success()
        if (bill.isPaid) return Result.success()

        val channelId = "bill_reminders"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    applicationContext.getString(R.string.bill_reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_rupee_notif)
            .setContentTitle(applicationContext.getString(R.string.bill_due_soon_title, bill.title))
            .setContentText(
                applicationContext.getString(
                    R.string.bill_due_soon_message,
                    daysBefore,
                    CurrencyFormatter.formatUsdCents(bill.amountCents),
                    DateFormatter.formatMonthDayYear(bill.dueDate)
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(billId.toInt(), notification)
        return Result.success()
    }
}

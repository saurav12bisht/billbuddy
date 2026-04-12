package com.android.billreminder.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.billreminder.R
import com.android.billreminder.domain.repository.CreditCardRepository
import com.android.billreminder.ui.common.util.CurrencyFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.*

@HiltWorker
class CreditCardReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: CreditCardRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cards = repository.getAllCreditCards().first()
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)

        cards.forEach { card ->
            // Check if today is the day after the cycle ends (to notify bill total)
            val cycleEndDay = if (card.billingDay == 1) 28 else card.billingDay - 1
            if (today == card.billingDay) {
                notifyBillGenerated(card)
            }

            // Check if today is 2 days before due date
            val dueRemindDay = if (card.dueDay <= 2) 28 else card.dueDay - 2
            if (today == dueRemindDay) {
                notifyDueSoon(card)
            }
        }

        return Result.success()
    }

    private suspend fun notifyBillGenerated(card: com.android.billreminder.domain.model.CreditCard) {
        val (start, end) = calculateBillingCycle(card.billingDay, true) // Previous cycle
        val total = repository.getTotalSpendForCardInCycle(card.id, start, end).first() ?: 0L
        
        if (total > 0) {
            sendNotification(
                id = card.id.toInt() * 10,
                title = "New Bill Generated: ${card.cardName}",
                message = "Your total credit card bill is ${CurrencyFormatter.formatUsdCents(total)}. Please pay before the due date."
            )
        }
    }

    private suspend fun notifyDueSoon(card: com.android.billreminder.domain.model.CreditCard) {
        val (start, end) = calculateBillingCycle(card.billingDay, false) // Current/Just ended cycle
        val total = repository.getTotalSpendForCardInCycle(card.id, start, end).first() ?: 0L

        if (total > 0) {
            sendNotification(
                id = card.id.toInt() * 10 + 1,
                title = "Payment Due Soon: ${card.cardName}",
                message = "₹${CurrencyFormatter.formatUsdCents(total)} is due on the ${card.dueDay}th. Please pay to avoid charges."
            )
        }
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val channelId = "cc_reminders"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Credit Card Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_rupee_notif)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }

    private fun calculateBillingCycle(billingDay: Int, previousCycle: Boolean): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        if (previousCycle) calendar.add(Calendar.MONTH, -1)
        
        val startCalendar = Calendar.getInstance().apply {
            time = calendar.time
            set(Calendar.DAY_OF_MONTH, billingDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        
        val endCalendar = Calendar.getInstance().apply {
            time = startCalendar.time
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        
        return Pair(startCalendar.timeInMillis, endCalendar.timeInMillis)
    }
}

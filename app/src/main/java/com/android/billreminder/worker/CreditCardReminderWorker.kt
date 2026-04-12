package com.android.billreminder.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.billreminder.R
import com.android.billreminder.domain.repository.CreditCardRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class CreditCardReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val creditCardRepository: CreditCardRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "credit_card_reminders"
        private const val CHANNEL_NAME = "Credit Card Reminders"
        const val TWO_DAYS_MILLIS = 2 * 24 * 60 * 60 * 1000L
    }

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override suspend fun doWork(): Result {
        ensureNotificationChannel()

        val now = System.currentTimeMillis()
        val twoDaysFromNow = now + TWO_DAYS_MILLIS

        val allUnpaidBills = creditCardRepository.getAllUnpaidBills().first()

        allUnpaidBills.forEach { bill ->
            val card = creditCardRepository.getCreditCardById(bill.cardId) ?: return@forEach
            val amount = currencyFormat.format(bill.totalAmountCents / 100.0)

            when {
                // ① Overdue — past due date, still unpaid
                bill.dueDateMillis < now -> {
                    val daysOverdue = TimeUnit.MILLISECONDS.toDays(now - bill.dueDateMillis)
                    sendNotification(
                        id = bill.id.toInt() + 1000,
                        title = "⚠️ Overdue: ${card.bankName} Bill",
                        body = "Your credit card bill of $amount is overdue by $daysOverdue day(s)! Pay immediately to avoid charges."
                    )
                }

                // ② Due Soon — within 2 days
                bill.dueDateMillis <= twoDaysFromNow -> {
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(bill.dueDateMillis - now)
                    val dayLabel = if (daysLeft == 0L) "today" else "in $daysLeft day(s)"
                    sendNotification(
                        id = bill.id.toInt(),
                        title = "💳 ${card.bankName} Bill Due $dayLabel",
                        body = "Your credit card bill of $amount is due $dayLabel. Tap to pay."
                    )
                }

                // else: not due yet, no notification
            }
        }

        return Result.success()
    }

    private fun sendNotification(id: Int, title: String, body: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_add_24) // safe fallback icon
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you about upcoming and overdue credit card payments"
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

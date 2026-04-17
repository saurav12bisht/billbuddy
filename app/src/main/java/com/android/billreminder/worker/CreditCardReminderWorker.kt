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
import com.android.billreminder.ui.common.util.CurrencyFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class CreditCardReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val creditCardRepository: CreditCardRepository,
    private val fingramDatabase: com.android.billreminder.data.local.FingramDatabase
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "credit_card_reminders"
        private const val CHANNEL_NAME = "Credit Card Reminders"
    }

    private val logDao = fingramDatabase.notificationLogDao()

    override suspend fun doWork(): Result {
        ensureNotificationChannel()

        val now = System.currentTimeMillis()
        val allUnpaidBills = creditCardRepository.getAllUnpaidBills().first()

        allUnpaidBills.forEach { bill ->
            val card = creditCardRepository.getCreditCardById(bill.cardId) ?: return@forEach
            val amount = CurrencyFormatter.formatUsdCents(bill.totalAmountCents)

            val diffMillis = bill.dueDateMillis - now
            val daysRemain = TimeUnit.MILLISECONDS.toDays(diffMillis) + 1

            when {
                // OVERDUE
                bill.dueDateMillis < now -> {
                    val daysOverdue = TimeUnit.MILLISECONDS.toDays(now - bill.dueDateMillis)
                    maybeSendNotification(
                        billId = bill.id,
                        reminderType = "OVERDUE_$daysOverdue",
                        title = "⚠️ Overdue: ${card.bankName} Bill",
                        body = "Your credit card bill of $amount is overdue by $daysOverdue day(s)!"
                    )
                }

                // GRADUATED REMINDERS: 7, 5, 3, 1 days before
                daysRemain in listOf(7L, 5L, 3L, 1L) -> {
                    val type = "DUE_$daysRemain"
                    val dayLabel = if (daysRemain == 1L) "tomorrow" else "in $daysRemain days"
                    maybeSendNotification(
                        billId = bill.id,
                        reminderType = type,
                        title = "💳 ${card.bankName} Bill Due $dayLabel",
                        body = "Your $amount bill is due $dayLabel. Please pay soon to avoid late fees."
                    )
                }
            }
        }

        return Result.success()
    }

    private suspend fun maybeSendNotification(
        billId: Long,
        reminderType: String,
        title: String,
        body: String
    ) {
        val existing = logDao.getLog(billId, reminderType)
        if (existing == null) {
            sendNotification(billId.toInt(), title, body)
            logDao.insertLog(com.android.billreminder.data.local.entity.NotificationLogEntity(
                billId = billId,
                reminderType = reminderType
            ))
        }
    }

    private fun sendNotification(id: Int, title: String, body: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_add_24)
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

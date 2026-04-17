package com.android.fingram.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object BillReminderScheduler {

    private val reminderOffsetsInDays = listOf(7L, 3L, 1L)

    fun scheduleBillReminders(
        context: Context,
        billId: Long,
        dueDateMillis: Long
    ) {
        reminderOffsetsInDays.forEach { offsetDays ->
            val triggerAt = dueDateMillis - TimeUnit.DAYS.toMillis(offsetDays)
            val delay = (triggerAt - System.currentTimeMillis()).coerceAtLeast(0L)
            val request = OneTimeWorkRequestBuilder<BillReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        KEY_BILL_ID to billId,
                        KEY_DAYS_BEFORE to offsetDays.toInt()
                    )
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(billId, offsetDays),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun cancelBillReminders(context: Context, billId: Long) {
        reminderOffsetsInDays.forEach { offsetDays ->
            WorkManager.getInstance(context).cancelUniqueWork(workName(billId, offsetDays))
        }
    }

    private fun workName(billId: Long, offsetDays: Long): String = "bill_${billId}_${offsetDays}d"

    const val KEY_BILL_ID = "bill_id"
    const val KEY_DAYS_BEFORE = "days_before"
}

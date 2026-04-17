package com.android.billreminder.ui.common.util

import com.android.billreminder.domain.model.CreditCard
import java.util.Calendar

object CreditCardBillingUtils {

    /**
     * Represents a range of time for a billing cycle.
     */
    data class CycleRange(
        val startDateMillis: Long,
        val endDateMillis: Long,
        val dueDateMillis: Long
    )

    /**
     * For a given [transactionDate], find the billing cycle it belongs to.
     * The billing cycle ends on the [card.billingDay].
     */
    fun getCycleForTransaction(card: CreditCard, transactionDate: Long): CycleRange {
        val cal = Calendar.getInstance().apply { timeInMillis = transactionDate }
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val statementClosingCal = Calendar.getInstance().apply { timeInMillis = transactionDate }
        
        if (day > card.billingDay) {
            // Belongs to next month's statement
            statementClosingCal.add(Calendar.MONTH, 1)
        }
        
        // Cap the billing day at the end of that specific month
        val maxDayInMonth = statementClosingCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        statementClosingCal.set(Calendar.DAY_OF_MONTH, minOf(card.billingDay, maxDayInMonth))
        setEndOfDay(statementClosingCal)

        val endDate = statementClosingCal.timeInMillis

        // Start date is 1 month before end date + 1 day
        val statementStartingCal = Calendar.getInstance().apply { 
             timeInMillis = endDate
             add(Calendar.MONTH, -1)
             add(Calendar.DAY_OF_MONTH, 1)
             setStartOfDay(this)
        }
        val startDate = statementStartingCal.timeInMillis

        // Due date calculation: typically in the month following the closing date
        val dueCal = Calendar.getInstance().apply {
            timeInMillis = endDate
            add(Calendar.MONTH, 1)
            val maxDueDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, minOf(card.dueDay, maxDueDay))
            setEndOfDay(this)
        }
        val dueDate = dueCal.timeInMillis

        return CycleRange(startDate, endDate, dueDate)
    }

    /**
     * Gets the current (open) billing cycle range as of [now].
     */
    fun getCurrentCycle(card: CreditCard, now: Long = System.currentTimeMillis()): CycleRange {
        return getCycleForTransaction(card, now)
    }

    private fun setStartOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun setEndOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
    }
}

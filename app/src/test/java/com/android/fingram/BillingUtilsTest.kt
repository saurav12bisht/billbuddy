package com.mobile.fingram

import com.mobile.fingram.domain.model.CreditCard
import com.mobile.fingram.ui.common.util.CreditCardBillingUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class BillingUtilsTest {

    private val card = CreditCard(
        id = 1,
        cardName = "Test Card",
        bankName = "Test Bank",
        lastFourDigits = "1234",
        billingDay = 25,
        dueDay = 5
    )

    @Test
    fun `transaction on 20th falls into current month statement`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.APRIL, 20)
        
        val cycle = CreditCardBillingUtils.getCycleForTransaction(card, cal.timeInMillis)
        
        // Statement closes on April 25
        val endCal = Calendar.getInstance().apply { timeInMillis = cycle.endDateMillis }
        assertEquals(25, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.APRIL, endCal.get(Calendar.MONTH))
        
        // Due on May 5
        val dueCal = Calendar.getInstance().apply { timeInMillis = cycle.dueDateMillis }
        assertEquals(5, dueCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MAY, dueCal.get(Calendar.MONTH))
    }

    @Test
    fun `transaction on 26th falls into next month statement`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.APRIL, 26)
        
        val cycle = CreditCardBillingUtils.getCycleForTransaction(card, cal.timeInMillis)
        
        // Statement closes on May 25
        val endCal = Calendar.getInstance().apply { timeInMillis = cycle.endDateMillis }
        assertEquals(25, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MAY, endCal.get(Calendar.MONTH))
        
        // Due on June 5
        val dueCal = Calendar.getInstance().apply { timeInMillis = cycle.dueDateMillis }
        assertEquals(5, dueCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.JUNE, dueCal.get(Calendar.MONTH))
    }

    @Test
    fun `billing day 31 in February caps to end of month`() {
        val card31 = card.copy(billingDay = 31)
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.FEBRUARY, 10)
        
        val cycle = CreditCardBillingUtils.getCycleForTransaction(card31, cal.timeInMillis)
        
        val endCal = Calendar.getInstance().apply { timeInMillis = cycle.endDateMillis }
        assertEquals(28, endCal.get(Calendar.DAY_OF_MONTH))
    }
}

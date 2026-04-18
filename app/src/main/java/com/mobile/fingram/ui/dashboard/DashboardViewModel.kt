package com.mobile.fingram.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.fingram.data.local.dao.MonthlyBillSummary
import com.mobile.fingram.data.repository.BillRepository
import com.mobile.fingram.domain.model.Bill
import com.mobile.fingram.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val totalBillsAmountCents: Long = 0,
    val paidAmountCents: Long = 0,
    val remainingAmountCents: Long = 0,
    val progressPercent: Int = 0,
    val upcomingBills: List<Bill> = emptyList(),
    val totalIncomeThisMonthCents: Long = 0,
    val totalSpentThisMonthCents: Long = 0,
    val totalCreditDueCents: Long = 0,
    val totalSpentTodayCents: Long = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    billRepository: BillRepository,
    expenseRepository: ExpenseRepository
) : ViewModel() {

    private val monthRange = currentMonthRange()
    private val todayRange = currentDayRange()

    val uiState: StateFlow<DashboardUiState> = combine(
        billRepository.getMonthlySummary(monthRange.first, monthRange.second),
        billRepository.getUpcomingBills(System.currentTimeMillis(), System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L),
        expenseRepository.getMonthlyTotals(monthRange.first, monthRange.second),
        expenseRepository.getTotalAmountInRange(todayRange.first, todayRange.second)
    ) { (summary, upcoming, monthlyTotals, todayExpenses) ->
        val summaryData = summary as MonthlyBillSummary
        val upcomingData = upcoming as List<Bill>
        val totals = monthlyTotals as com.mobile.fingram.data.local.dao.MonthlyTotals
        val todayExp = todayExpenses as Long
        
        val progress = if (summaryData.totalAmountCents == 0L) 0 else {
            ((summaryData.paidAmountCents * 100) / summaryData.totalAmountCents).toInt()
        }
        DashboardUiState(
            totalBillsAmountCents = summaryData.totalAmountCents,
            paidAmountCents = summaryData.paidAmountCents,
            remainingAmountCents = summaryData.unpaidAmountCents,
            progressPercent = progress,
            upcomingBills = upcomingData.take(3),
            totalIncomeThisMonthCents = totals.totalIncome,
            totalSpentThisMonthCents = totals.totalExpense,
            totalCreditDueCents = totals.totalCreditSpent,
            totalSpentTodayCents = todayExp
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private fun currentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        return start to calendar.timeInMillis
    }

    private fun currentDayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        return start to calendar.timeInMillis
    }
}

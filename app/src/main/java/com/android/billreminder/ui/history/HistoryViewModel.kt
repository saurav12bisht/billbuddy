package com.android.billreminder.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.data.repository.BillRepository
import com.android.billreminder.domain.model.Bill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class HistoryMonthPoint(
    val label: String,
    val amountCents: Long
)

data class HistoryUiState(
    val totalSpentCents: Long = 0,
    val monthlyPoints: List<HistoryMonthPoint> = emptyList(),
    val paidBills: List<Bill> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    billRepository: BillRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = billRepository.getAllBills()
        .map { bills ->
            val paidBills = bills.filter { it.isPaid }.sortedByDescending { it.lastPaidAt ?: it.dueDate }
            val monthlyPoints = buildMonthlyPoints(paidBills)
            HistoryUiState(
                totalSpentCents = paidBills.sumOf { it.amountCents },
                monthlyPoints = monthlyPoints,
                paidBills = paidBills
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private fun buildMonthlyPoints(bills: List<Bill>): List<HistoryMonthPoint> {
        val formatter = SimpleDateFormat("MMM", Locale.US)
        val calendar = Calendar.getInstance()
        val buckets = mutableListOf<HistoryMonthPoint>()
        repeat(6) { index ->
            val clone = calendar.clone() as Calendar
            clone.add(Calendar.MONTH, -(5 - index))
            val month = clone.get(Calendar.MONTH)
            val year = clone.get(Calendar.YEAR)
            val amount = bills.filter {
                val billCal = Calendar.getInstance().apply { timeInMillis = it.lastPaidAt ?: it.dueDate }
                billCal.get(Calendar.MONTH) == month && billCal.get(Calendar.YEAR) == year
            }.sumOf { it.amountCents }
            buckets += HistoryMonthPoint(formatter.format(clone.time).uppercase(), amount)
        }
        return buckets
    }
}

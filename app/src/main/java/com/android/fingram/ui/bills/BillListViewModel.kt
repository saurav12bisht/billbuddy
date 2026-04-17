package com.android.fingram.ui.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.fingram.data.repository.BillRepository
import com.android.fingram.domain.model.Bill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillListSummary(
    val totalOutstandingCents: Long = 0,
    val dueSoonCount: Int = 0,
    val paidCount: Int = 0
)

@HiltViewModel
class BillListViewModel @Inject constructor(
    private val billRepository: BillRepository
) : ViewModel() {

    val bills: StateFlow<List<Bill>> = billRepository.getAllBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<BillListSummary> = bills
        .map { currentBills ->
            BillListSummary(
                totalOutstandingCents = currentBills.filterNot(Bill::isPaid).sumOf(Bill::amountCents),
                dueSoonCount = currentBills.count { !it.isPaid },
                paidCount = currentBills.count(Bill::isPaid)
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillListSummary())

    fun togglePaid(bill: Bill) {
        viewModelScope.launch {
            billRepository.markAsPaid(bill.id, !bill.isPaid)
        }
    }

    fun deleteBill(billId: Long) {
        viewModelScope.launch {
            billRepository.deleteBill(billId)
        }
    }
}

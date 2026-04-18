package com.mobile.fingram.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.fingram.data.local.dao.CustomerWithBalance
import com.mobile.fingram.data.repository.CustomerRepository
import com.mobile.fingram.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.mobile.fingram.data.local.dao.RecentTransactionItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeBalance(
    val totalUdhaarGiven: Long,
    val totalReceived: Long,
    val netOutstanding: Long
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val customerRepo: CustomerRepository,
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    val customersWithBalance: StateFlow<List<CustomerWithBalance>> = customerRepo
        .getCustomersWithBalance()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val balance: StateFlow<HomeBalance> = combine(
        transactionRepo.getTotalsFlow(),
        customersWithBalance
    ) { totals, customers ->
        val net = customers.filter { it.balance > 0 }.sumOf { it.balance }
        HomeBalance(totals.given, totals.received, net)
    }.stateIn(viewModelScope, SharingStarted.Lazily, HomeBalance(0L, 0L, 0L))

    val recentWithCustomerName: StateFlow<List<RecentTransactionItem>> =
        transactionRepo.getRecentWithCustomerName(5)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

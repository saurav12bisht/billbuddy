package com.mobile.fingram.ui.summary

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.fingram.data.export.ExportImportManager
import com.mobile.fingram.data.local.dao.CustomerWithBalance
import com.mobile.fingram.data.repository.CustomerRepository
import com.mobile.fingram.data.local.dao.Totals
import com.mobile.fingram.data.repository.TransactionRepository
import com.mobile.fingram.ui.home.HomeBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val customerRepo: CustomerRepository,
    private val transactionRepo: TransactionRepository,
    private val exportImportManager: ExportImportManager
) : ViewModel() {

    private val _balance = MutableLiveData<HomeBalance>()
    val balance: LiveData<HomeBalance> = _balance

    private val _customersWithBalance = MutableLiveData<List<CustomerWithBalance>>()
    val customersWithBalance: LiveData<List<CustomerWithBalance>> = _customersWithBalance

    private val _exportResult = MutableStateFlow<Result<com.mobile.fingram.data.export.ExportImportManager.ExportResult>?>(null)
    val exportResult = _exportResult.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                transactionRepo.getTotalsFlow(),
                customerRepo.getCustomersWithBalance()
            ) { totals: Totals, customers: List<CustomerWithBalance> ->
                val net = customers.filter { it.balance > 0 }.sumOf { it.balance }
                _balance.postValue(HomeBalance(totals.given, totals.received, net))
                _customersWithBalance.postValue(customers)
            }.collect {}
        }
    }

    fun exportSummary(context: Context) {
        viewModelScope.launch {
            _exportResult.value = exportImportManager.exportToExcel()
        }
    }
}

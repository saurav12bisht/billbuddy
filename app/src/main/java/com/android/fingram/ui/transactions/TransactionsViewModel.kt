package com.android.fingram.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.fingram.data.local.dao.MonthlyTotals
import com.android.fingram.data.local.entity.ExpenseEntity
import com.android.fingram.data.local.entity.ExpenseWithCategory
import com.android.fingram.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

sealed class TransactionListItem {
    data class DayHeader(
        val date: LocalDate,
        val incomeTotal: Long,
        val expenseTotal: Long
    ) : TransactionListItem()

    data class TransactionRow(
        val item: ExpenseWithCategory
    ) : TransactionListItem()
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val timeRange = _currentMonth.map { ym ->
        val start = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        start to end
    }

    val monthlyTotals: StateFlow<MonthlyTotals> = timeRange.flatMapLatest { range ->
        repository.getMonthlyTotals(range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyTotals(0, 0, 0))

    private val transactions = timeRange.flatMapLatest { range ->
        repository.getTransactionsByMonth(range.first, range.second)
    }

    private val dailyTotals = timeRange.flatMapLatest { range ->
        repository.getDailyTotals(range.first, range.second)
    }

    val groupedTransactions: StateFlow<List<TransactionListItem>> = combine(
        transactions,
        dailyTotals
    ) { transList, totalsList ->
        val items = mutableListOf<TransactionListItem>()
        val groupedByDate = transList.groupBy { 
            LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.expense.dateMillis), ZoneId.systemDefault()) 
        }
        
        // Iterate through dates in descending order
        groupedByDate.keys.sortedDescending().forEach { date ->
            val dayTotals = totalsList.find { 
                LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.dateMillis), ZoneId.systemDefault()) == date 
            }
            items.add(TransactionListItem.DayHeader(
                date = date,
                incomeTotal = dayTotals?.incomeTotal ?: 0,
                expenseTotal = dayTotals?.expenseTotal ?: 0
            ))
            groupedByDate[date]?.forEach { 
                items.add(TransactionListItem.TransactionRow(it))
            }
        }
        items
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun deleteTransaction(expense: ExpenseEntity) {
        viewModelScope.launch {
            // Reverse the balance impact before deleting
            val reversalDelta = if (expense.type == "INCOME") -expense.amountCents else expense.amountCents
            
            // Only update balance for non-credit transactions (as credit transactions use CreditCardId)
            if (expense.creditCardId == null) {
                repository.updateAccountBalance(expense.accountId, reversalDelta)
            }
            
            repository.deleteTransaction(expense)
        }
    }
}

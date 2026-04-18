package com.mobile.fingram.ui.expenselist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.fingram.domain.repository.ExpenseRepository
import com.mobile.fingram.domain.model.Expense
import com.mobile.fingram.domain.model.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class ExpenseListUiState(
    val expenses: List<Expense> = emptyList(),
    val totalSpentThisMonth: Long = 0L
)

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val monthRange = currentMonthRange()

    val uiState: StateFlow<ExpenseListUiState> = combine(
        expenseRepository.getTransactionsByMonth(monthRange.first, monthRange.second),
        expenseRepository.getTotalAmountInRange(monthRange.first, monthRange.second)
    ) { expensesWithCategory, monthlyTotal ->

        val expenses = expensesWithCategory.map { it.expense.toDomain() }

        ExpenseListUiState(
            expenses = expenses,
            totalSpentThisMonth = monthlyTotal
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ExpenseListUiState()
    )

    fun deleteExpense(id: Long) {
//        viewModelScope.launch {
//            expenseRepository.deleteExpense(id)
//        }
    }

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
}

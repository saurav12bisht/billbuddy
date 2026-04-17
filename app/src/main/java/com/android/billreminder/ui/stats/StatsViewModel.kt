package com.android.billreminder.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.data.local.entity.TransactionType
import com.android.billreminder.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class CategoryStat(
    val categoryId: Long,
    val name: String,
    val iconEmoji: String,
    val colorHex: String,
    val amountCents: Long,
    val percentage: Float
)

data class PaymentModeStat(
    val name: String,
    val amountCents: Long,
    val percentage: Float,
    val colorHex: String
)

enum class StatsFilterType {
    EXPENSE,
    INCOME,
    CREDIT_CARD_DUE
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _type = MutableStateFlow(StatsFilterType.EXPENSE)
    val type: StateFlow<StatsFilterType> = _type.asStateFlow()

    private val timeRange = _currentMonth.map { ym ->
        val start = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        start to end
    }

    val categoryStats: StateFlow<List<CategoryStat>> = combine(
        timeRange.flatMapLatest { repository.getTransactionsByMonth(it.first, it.second) },
        _type
    ) { transactions, selectedType ->
        val filtered = transactions.filter { transaction ->
            when (selectedType) {
                StatsFilterType.EXPENSE ->
                    transaction.expense.type == "EXPENSE" &&
                        transaction.expense.transactionType == TransactionType.NORMAL
                StatsFilterType.INCOME ->
                    transaction.expense.type == "INCOME"
                StatsFilterType.CREDIT_CARD_DUE ->
                    transaction.expense.type == "EXPENSE" &&
                        transaction.expense.transactionType == TransactionType.CREDIT
            }
        }
        val total = filtered.sumOf { it.expense.amountCents }

        if (total == 0L) return@combine emptyList<CategoryStat>()

        filtered.groupBy { it.category.id }
            .map { (catId, items) ->
                val cat = items.first().category
                val sum = items.sumOf { it.expense.amountCents }
                CategoryStat(
                    categoryId = catId,
                    name = cat.name,
                    iconEmoji = cat.iconEmoji,
                    colorHex = cat.colorHex,
                    amountCents = sum,
                    percentage = (sum.toFloat() / total.toFloat()) * 100f
                )
            }.sortedByDescending { it.amountCents }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentModeStats: StateFlow<List<PaymentModeStat>> = combine(
        timeRange.flatMapLatest { repository.getTransactionsByMonth(it.first, it.second) },
        _type
    ) { transactions, selectedType ->
        if (selectedType != StatsFilterType.EXPENSE) return@combine emptyList<PaymentModeStat>()

        val filtered = transactions.filter {
            it.expense.type == "EXPENSE" &&
                it.expense.transactionType == TransactionType.NORMAL
        }
        val total = filtered.sumOf { it.expense.amountCents }
        if (total == 0L) return@combine emptyList<PaymentModeStat>()

        val normalSum = filtered.sumOf { it.expense.amountCents }

        val stats = mutableListOf<PaymentModeStat>()
        if (normalSum > 0) {
            stats.add(PaymentModeStat("Cash / Bank", normalSum, (normalSum.toFloat() / total.toFloat()) * 100f, "#2196F3"))
        }
        stats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setType(type: StatsFilterType) {
        _type.value = type
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }
}

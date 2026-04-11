package com.android.billreminder.domain.repository

import com.android.billreminder.data.local.dao.DailyTotal
import com.android.billreminder.data.local.dao.MonthlyTotals
import com.android.billreminder.data.local.entity.AccountEntity
import com.android.billreminder.data.local.entity.CategoryEntity
import com.android.billreminder.data.local.entity.ExpenseEntity
import com.android.billreminder.data.local.entity.ExpenseWithCategory
import com.android.billreminder.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {

    fun getTransactionsByMonth(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>
    fun getMonthlyTotals(startMillis: Long, endMillis: Long): Flow<MonthlyTotals>
    fun getDailyTotals(startMillis: Long, endMillis: Long): Flow<List<DailyTotal>>

    suspend fun insertTransaction(expense: ExpenseEntity): Long
    suspend fun deleteTransaction(expense: ExpenseEntity)

    // ✅ NEW FUNCTIONS
    suspend fun saveExpense(expense: Expense): Long
    suspend fun getExpenseById(id: Long): Expense?
    suspend fun updateExpense(expense: Expense)

    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun insertCategory(category: CategoryEntity): Long

    fun getAllAccounts(): Flow<List<AccountEntity>>
    suspend fun insertAccount(account: AccountEntity): Long
    suspend fun updateAccountBalance(accountId: Long, delta: Long)

    fun getTotalAmountInRange(start: Long, end: Long): Flow<Long>
}

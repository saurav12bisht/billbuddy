package com.mobile.fingram.domain.repository

import com.mobile.fingram.data.local.dao.DailyTotal
import com.mobile.fingram.data.local.dao.MonthlyTotals
import com.mobile.fingram.data.local.entity.AccountEntity
import com.mobile.fingram.data.local.entity.CategoryEntity
import com.mobile.fingram.data.local.entity.ExpenseEntity
import com.mobile.fingram.data.local.entity.ExpenseWithCategory
import com.mobile.fingram.domain.model.Expense
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
    suspend fun getExpenseByIdEntity(id: Long): ExpenseEntity?
    suspend fun getLatestTransactionEntity(): ExpenseEntity?
    suspend fun updateExpense(expense: Expense)

    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun insertCategory(category: CategoryEntity): Long
    suspend fun getCategoryByName(name: String): CategoryEntity?
    suspend fun getFirstCategory(): CategoryEntity?

    fun getAllAccounts(): Flow<List<AccountEntity>>
    fun getAccountsByType(type: String): Flow<List<AccountEntity>>
    suspend fun insertAccount(account: AccountEntity): Long
    suspend fun updateAccount(account: AccountEntity)
    suspend fun getAccountById(accountId: Long): AccountEntity?
    suspend fun updateAccountBalance(accountId: Long, delta: Long)
    suspend fun deleteAccount(account: AccountEntity)

    fun getTotalAmountInRange(start: Long, end: Long): Flow<Long>
}

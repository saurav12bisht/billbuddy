package com.android.billreminder.data.repository

import com.android.billreminder.data.local.dao.AccountDao
import com.android.billreminder.data.local.dao.DailyTotal
import com.android.billreminder.data.local.dao.ExpenseDao
import com.android.billreminder.data.local.dao.MonthlyTotals
import com.android.billreminder.data.local.entity.AccountEntity
import com.android.billreminder.data.local.entity.CategoryEntity
import com.android.billreminder.data.local.entity.ExpenseEntity
import com.android.billreminder.data.local.entity.ExpenseWithCategory
import com.android.billreminder.domain.repository.ExpenseRepository
import com.android.billreminder.di.IoDispatcher
import com.android.billreminder.domain.model.Expense
import com.android.billreminder.domain.model.toDomain
import com.android.billreminder.domain.model.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val accountDao: AccountDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : ExpenseRepository {

    override fun getTransactionsByMonth(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>> =
        expenseDao.getAllByMonth(startMillis, endMillis).flowOn(io)

    override fun getMonthlyTotals(startMillis: Long, endMillis: Long): Flow<MonthlyTotals> =
        expenseDao.getMonthlyTotals(startMillis, endMillis).flowOn(io)

    override fun getDailyTotals(startMillis: Long, endMillis: Long): Flow<List<DailyTotal>> =
        expenseDao.getDailyTotals(startMillis, endMillis).flowOn(io)

    override suspend fun insertTransaction(expense: ExpenseEntity): Long = withContext(io) {
        expenseDao.insertExpense(expense)
    }

    override suspend fun deleteTransaction(expense: ExpenseEntity) = withContext(io) {
        expenseDao.deleteExpense(expense)
    }



    override suspend fun saveExpense(expense: Expense): Long = withContext(io) {
        val entity = expense.toEntity()

        if (entity.id == 0L) {
            expenseDao.insertExpense(entity)
        } else {
            expenseDao.updateExpense(entity)
            entity.id
        }
    }

    override suspend fun getExpenseById(id: Long): Expense? = withContext(io) {
        expenseDao.getExpenseById(id)?.toDomain()
    }

    override suspend fun getExpenseByIdEntity(id: Long): ExpenseEntity? = withContext(io) {
        expenseDao.getExpenseById(id)
    }

    override suspend fun getLatestTransactionEntity(): ExpenseEntity? = withContext(io) {
        expenseDao.getLatestTransaction()
    }

    override suspend fun updateExpense(expense: Expense) = withContext(io) {
        expenseDao.updateExpense(expense.toEntity())
    }



    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        expenseDao.getAllCategories().flowOn(io)

    override suspend fun insertCategory(category: CategoryEntity): Long = withContext(io) {
        expenseDao.insertCategory(category)
    }

    override fun getAllAccounts(): Flow<List<AccountEntity>> =
        accountDao.getAllAccounts().flowOn(io)

    override fun getAccountsByType(type: String): Flow<List<AccountEntity>> =
        accountDao.getAccountsByType(type).flowOn(io)

    override suspend fun insertAccount(account: AccountEntity): Long = withContext(io) {
        accountDao.insertAccount(account)
    }

    override suspend fun updateAccount(account: AccountEntity) = withContext(io) {
        accountDao.updateAccount(account)
    }

    override suspend fun getAccountById(accountId: Long): AccountEntity? = withContext(io) {
        accountDao.getAccountById(accountId)
    }

    override suspend fun updateAccountBalance(accountId: Long, delta: Long) = withContext(io) {
        accountDao.updateBalance(accountId, delta)
    }

    override suspend fun deleteAccount(account: AccountEntity) = withContext(io) {
        accountDao.deleteAccount(account)
    }

    override fun getTotalAmountInRange(start: Long, end: Long): Flow<Long> =
        expenseDao.getTotalAmountInRange(start, end).flowOn(io)

    override suspend fun getCategoryByName(name: String): CategoryEntity? = withContext(io) {
        expenseDao.getCategoryByName(name)
    }

    override suspend fun getFirstCategory(): CategoryEntity? = withContext(io) {
        expenseDao.getAllCategories().flowOn(io).let { flow ->
            var result: CategoryEntity? = null
            flow.collect { list -> result = list.firstOrNull(); return@collect }
            result
        }
    }
}

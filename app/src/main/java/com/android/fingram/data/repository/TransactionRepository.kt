package com.android.fingram.data.repository

import com.android.fingram.data.local.dao.Totals
import com.android.fingram.data.local.dao.TransactionDao
import com.android.fingram.data.local.entity.TransactionEntity
import com.android.fingram.data.local.dao.RecentTransactionItem
import com.android.fingram.data.local.dao.TransactionWithCustomer
import com.android.fingram.domain.model.Transaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import com.android.fingram.di.IoDispatcher
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun getTransactionsForCustomer(id: Int): Flow<List<TransactionEntity>> =
        dao.getTransactionsForCustomer(id).flowOn(io)

    fun getOverdueTransactions(now: Long): Flow<List<TransactionEntity>> =
        dao.getOverdueTransactions(now).flowOn(io)

    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>> =
        dao.getRecentTransactions(limit).flowOn(io)

    fun getRecentWithCustomerName(limit: Int): Flow<List<RecentTransactionItem>> =
        dao.getRecentWithCustomerName(limit).flowOn(io)

    suspend fun getNetBalance(customerId: Int): Long = withContext(io) {
        (dao.getNetBalance(customerId) ?: 0L)
    }

    suspend fun insert(transaction: Transaction): Long = withContext(io) {
        dao.insert(transaction.toTransactionEntity())
    }

    suspend fun update(transaction: Transaction) = withContext(io) {
        dao.update(transaction.toTransactionEntity())
    }

    private fun Transaction.toTransactionEntity() = TransactionEntity(
        id = id, customerId = customerId, type = type, amountPaise = amountPaise, date = date,
        dueDate = dueDate, interestPercent = interestPercent, category = category, note = note,
        receiptPhotoPath = receiptPhotoPath, isSettlement = isSettlement, createdAt = createdAt
    )

    suspend fun deleteById(id: Long) = withContext(io) { dao.deleteById(id) }
    suspend fun getById(id: Long) = withContext(io) { dao.getById(id) }
    suspend fun getTotalUdhaarGiven(): Long = withContext(io) { dao.getTotalUdhaarGiven() ?: 0L }
    suspend fun getTotalReceived(): Long = withContext(io) { dao.getTotalReceived() ?: 0L }

    fun getTotalsFlow(): Flow<Totals> =
        dao.getTotalsFlow().flowOn(io)

    suspend fun getAllTransactionsWithCustomerName(): List<TransactionWithCustomer> = withContext(io) {
        dao.getAllTransactionsWithCustomerName()
    }

    suspend fun insertAll(transactions: List<Transaction>) = withContext(io) {
        transactions.forEach { transaction ->
            dao.insert(transaction.toTransactionEntity())
        }
    }
}

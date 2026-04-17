package com.android.fingram.data.repository

import com.android.fingram.data.local.dao.BillDao
import com.android.fingram.data.local.dao.MonthlyBillSummary
import com.android.fingram.domain.model.Bill
import com.android.fingram.domain.model.toDomain
import com.android.fingram.domain.model.toEntity
import com.android.fingram.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BillRepository @Inject constructor(
    private val billDao: BillDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {

    fun getAllBills(): Flow<List<Bill>> =
        billDao.getAllBills()
            .map { bills -> bills.map { it.toDomain() } }
            .flowOn(io)

    fun getUpcomingBills(fromMillis: Long, toMillis: Long): Flow<List<Bill>> =
        billDao.getUpcomingBills(fromMillis, toMillis)
            .map { bills -> bills.map { it.toDomain() } }
            .flowOn(io)

    fun getMonthlySummary(startMillis: Long, endMillis: Long): Flow<MonthlyBillSummary> =
        billDao.getMonthlySummary(startMillis, endMillis).flowOn(io)

    suspend fun getBillById(billId: Long): Bill? = withContext(io) {
        billDao.getBillById(billId)?.toDomain()
    }

    suspend fun saveBill(bill: Bill): Long = withContext(io) {
        val now = System.currentTimeMillis()
        billDao.insertBill(
            bill.copy(
                createdAt = if (bill.createdAt == 0L) now else bill.createdAt,
                updatedAt = now
            ).toEntity()
        )
    }

    suspend fun updateBill(bill: Bill) = withContext(io) {
        billDao.updateBill(bill.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteBill(billId: Long) = withContext(io) {
        billDao.deleteBill(billId)
    }

    suspend fun markAsPaid(billId: Long, paid: Boolean) = withContext(io) {
        billDao.markAsPaid(
            billId = billId,
            paid = paid,
            paidAt = if (paid) System.currentTimeMillis() else null
        )
    }
}

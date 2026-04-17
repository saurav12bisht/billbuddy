package com.android.fingram.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.android.fingram.data.local.entity.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Query("DELETE FROM bills WHERE id = :billId")
    suspend fun deleteBill(billId: Long)

    @Query("SELECT * FROM bills WHERE id = :billId LIMIT 1")
    suspend fun getBillById(billId: Long): BillEntity?

    @Query("SELECT * FROM bills ORDER BY isPaid ASC, dueDate ASC, title ASC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query(
        """
        SELECT * FROM bills
        WHERE isPaid = 0 AND dueDate BETWEEN :fromMillis AND :toMillis
        ORDER BY dueDate ASC
        """
    )
    fun getUpcomingBills(fromMillis: Long, toMillis: Long): Flow<List<BillEntity>>

    @Query(
        """
        UPDATE bills
        SET isPaid = :paid,
            lastPaidAt = :paidAt,
            updatedAt = :updatedAt
        WHERE id = :billId
        """
    )
    suspend fun markAsPaid(
        billId: Long,
        paid: Boolean,
        paidAt: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        SELECT
            COUNT(*) AS totalCount,
            SUM(CASE WHEN isPaid = 1 THEN 1 ELSE 0 END) AS paidCount,
            SUM(CASE WHEN isPaid = 0 THEN 1 ELSE 0 END) AS unpaidCount,
            COALESCE(SUM(amountCents), 0) AS totalAmountCents,
            COALESCE(SUM(CASE WHEN isPaid = 1 THEN amountCents ELSE 0 END), 0) AS paidAmountCents,
            COALESCE(SUM(CASE WHEN isPaid = 0 THEN amountCents ELSE 0 END), 0) AS unpaidAmountCents
        FROM bills
        WHERE dueDate BETWEEN :startMillis AND :endMillis
        """
    )
    fun getMonthlySummary(startMillis: Long, endMillis: Long): Flow<MonthlyBillSummary>
}

data class MonthlyBillSummary(
    val totalCount: Int,
    val paidCount: Int,
    val unpaidCount: Int,
    val totalAmountCents: Long,
    val paidAmountCents: Long,
    val unpaidAmountCents: Long
)

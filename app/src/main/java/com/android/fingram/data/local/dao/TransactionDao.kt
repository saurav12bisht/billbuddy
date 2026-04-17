package com.android.fingram.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.fingram.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE customerId = :id ORDER BY date DESC")
    fun getTransactionsForCustomer(id: Int): Flow<List<TransactionEntity>>

    @Query("""
        SELECT SUM(CASE WHEN type='CREDIT_GIVEN' THEN amountPaise ELSE -amountPaise END)
        FROM transactions WHERE customerId = :id
    """)
    suspend fun getNetBalance(id: Int): Long?

    @Query("SELECT * FROM transactions WHERE dueDate < :now AND type = 'CREDIT_GIVEN' AND (dueDate IS NOT NULL AND dueDate > 0)")
    fun getOverdueTransactions(now: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT t.id as transactionId, t.customerId, c.name as customerName, t.type, t.amountPaise, t.date, t.note FROM transactions t INNER JOIN customers c ON t.customerId = c.id WHERE c.isActive = 1 ORDER BY t.date DESC LIMIT :limit")
    fun getRecentWithCustomerName(limit: Int): Flow<List<RecentTransactionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @androidx.room.Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("""
        SELECT SUM(CASE WHEN type = 'CREDIT_GIVEN' THEN amountPaise ELSE -amountPaise END) FROM transactions
    """)
    suspend fun getTotalUdhaarGiven(): Long?

    @Query("""
        SELECT SUM(CASE WHEN type = 'PAYMENT_RECEIVED' THEN amountPaise ELSE 0 END) FROM transactions
    """)
    suspend fun getTotalReceived(): Long?

    @Query("SELECT (SELECT COALESCE(SUM(amountPaise),0) FROM transactions WHERE type='CREDIT_GIVEN') as given, (SELECT COALESCE(SUM(amountPaise),0) FROM transactions WHERE type='PAYMENT_RECEIVED') as received")
    fun getTotalsFlow(): Flow<Totals>

    @Query("""
        SELECT t.id, t.customerId, c.name as customerName, c.phone as customerPhone, t.type, t.amountPaise, t.date, 
               t.dueDate, t.interestPercent, t.category, t.note, t.receiptPhotoPath, 
               t.isSettlement, t.createdAt
        FROM transactions t 
        INNER JOIN customers c ON t.customerId = c.id 
        ORDER BY t.date DESC, t.createdAt DESC
    """)
    suspend fun getAllTransactionsWithCustomerName(): List<TransactionWithCustomer>
}

data class Totals(val given: Long, val received: Long)

data class RecentTransactionItem(
    val transactionId: Long,
    val customerId: Int,
    val customerName: String,
    val type: String,
    val amountPaise: Long,
    val date: Long,
    val note: String
)

data class TransactionWithCustomer(
    val id: Long,
    val customerId: Int,
    val customerName: String,
    val customerPhone: String,
    val type: String,
    val amountPaise: Long,
    val date: Long,
    val dueDate: Long?,
    val interestPercent: Double,
    val category: String,
    val note: String,
    val receiptPhotoPath: String?,
    val isSettlement: Boolean,
    val createdAt: Long
)

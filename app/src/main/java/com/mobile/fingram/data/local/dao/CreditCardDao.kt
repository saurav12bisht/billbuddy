package com.mobile.fingram.data.local.dao

import androidx.room.*
import com.mobile.fingram.data.local.entity.CreditCardBillEntity
import com.mobile.fingram.data.local.entity.CreditCardEntity
import com.mobile.fingram.data.local.entity.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {

    @Query("SELECT * FROM credit_cards ORDER BY cardName ASC")
    fun getAllCreditCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE id = :id LIMIT 1")
    suspend fun getCreditCardById(id: Long): CreditCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(card: CreditCardEntity): Long

    @Update
    suspend fun updateCreditCard(card: CreditCardEntity)

    @Delete
    suspend fun deleteCreditCard(card: CreditCardEntity)

    /**
     * Credit spends (CREDIT type) for a card in a billing cycle period.
     */
    @Transaction
    @Query("""
        SELECT * FROM expenses 
        WHERE creditCardId = :cardId 
        AND dateMillis BETWEEN :startMillis AND :endMillis 
        AND transactionType = 'CREDIT'
        ORDER BY dateMillis DESC
    """)
    fun getExpensesForCardInCycle(cardId: Long, startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>

    /**
     * Sum of CREDIT spends for a card in a billing cycle period.
     */
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM expenses 
        WHERE creditCardId = :cardId 
        AND dateMillis BETWEEN :startMillis AND :endMillis
        AND transactionType = 'CREDIT'
    """)
    fun getTotalSpendForCardInCycle(cardId: Long, startMillis: Long, endMillis: Long): Flow<Long>

    // ──────────────────────────────────────────────
    // Bill management
    // ──────────────────────────────────────────────

    @Query("SELECT * FROM credit_card_bills WHERE cardId = :cardId ORDER BY billingCycleStartDate DESC")
    fun getBillsForCard(cardId: Long): Flow<List<CreditCardBillEntity>>

    @Query("SELECT * FROM credit_card_bills WHERE cardId = :cardId AND isPaid = 0 ORDER BY dueDateMillis ASC")
    fun getUnpaidBills(cardId: Long): Flow<List<CreditCardBillEntity>>

    /** All unpaid bills across ALL cards — used by the notification worker. */
    @Query("SELECT * FROM credit_card_bills WHERE isPaid = 0 ORDER BY dueDateMillis ASC")
    fun getAllUnpaidBills(): Flow<List<CreditCardBillEntity>>

    /**
     * Outstanding amount for a card: sum of all unpaid bills.
     */
    @Query("""
        SELECT COALESCE(SUM(totalAmountCents - paidAmountCents), 0) 
        FROM credit_card_bills 
        WHERE cardId = :cardId AND isPaid = 0
    """)
    fun getOutstandingAmount(cardId: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: CreditCardBillEntity): Long

    @Update
    suspend fun updateBill(bill: CreditCardBillEntity)

    @Query("SELECT * FROM credit_card_bills WHERE cardId = :cardId AND billingCycleStartDate = :cycleStart LIMIT 1")
    suspend fun getBillByCycleStart(cardId: Long, cycleStart: Long): CreditCardBillEntity?
}

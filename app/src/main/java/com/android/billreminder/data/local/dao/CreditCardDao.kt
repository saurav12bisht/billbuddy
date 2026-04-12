package com.android.billreminder.data.local.dao

import androidx.room.*
import com.android.billreminder.data.local.entity.CreditCardBillEntity
import com.android.billreminder.data.local.entity.CreditCardEntity
import com.android.billreminder.data.local.entity.ExpenseWithCategory
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

    @Transaction
    @Query("""
        SELECT * FROM expenses 
        WHERE creditCardId = :cardId 
        AND dateMillis BETWEEN :startMillis AND :endMillis 
        ORDER BY dateMillis DESC
    """)
    fun getExpensesForCardInCycle(cardId: Long, startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>

    @Query("""
        SELECT SUM(amountCents) FROM expenses 
        WHERE creditCardId = :cardId 
        AND dateMillis BETWEEN :startMillis AND :endMillis
    """)
    fun getTotalSpendForCardInCycle(cardId: Long, startMillis: Long, endMillis: Long): Flow<Long?>

    // Bill status management
    @Query("SELECT * FROM credit_card_bills WHERE cardId = :cardId ORDER BY billingCycleStartDate DESC")
    fun getBillsForCard(cardId: Long): Flow<List<CreditCardBillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: CreditCardBillEntity): Long

    @Update
    suspend fun updateBill(bill: CreditCardBillEntity)
}

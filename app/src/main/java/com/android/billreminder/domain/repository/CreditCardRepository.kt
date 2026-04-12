package com.android.billreminder.domain.repository

import com.android.billreminder.data.local.entity.CreditCardBillEntity
import com.android.billreminder.data.local.entity.CreditCardEntity
import com.android.billreminder.data.local.entity.ExpenseWithCategory
import com.android.billreminder.domain.model.CreditCard
import com.android.billreminder.domain.model.CreditCardBill
import kotlinx.coroutines.flow.Flow

interface CreditCardRepository {
    fun getAllCreditCards(): Flow<List<CreditCard>>
    suspend fun getCreditCardById(id: Long): CreditCard?
    suspend fun saveCreditCard(card: CreditCard): Long
    suspend fun deleteCreditCard(card: CreditCard)
    
    fun getExpensesForCardInCycle(cardId: Long, startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>
    fun getTotalSpendForCardInCycle(cardId: Long, startMillis: Long, endMillis: Long): Flow<Long?>
    
    fun getBillsForCard(cardId: Long): Flow<List<CreditCardBill>>
    suspend fun saveBill(bill: CreditCardBill): Long
}

package com.android.billreminder.domain.repository

import com.android.billreminder.data.local.entity.ExpenseWithCategory
import com.android.billreminder.domain.model.CreditCard
import com.android.billreminder.domain.model.CreditCardBill
import kotlinx.coroutines.flow.Flow

interface CreditCardRepository {

    // ── Card CRUD ──────────────────────────────────────────────
    fun getAllCreditCards(): Flow<List<CreditCard>>
    suspend fun getCreditCardById(id: Long): CreditCard?
    suspend fun saveCreditCard(card: CreditCard): Long
    suspend fun deleteCreditCard(card: CreditCard)

    // ── Credit Spend Queries ──────────────────────────────────
    fun getExpensesForCardInCycle(cardId: Long, startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>
    fun getTotalSpendForCardInCycle(cardId: Long, startMillis: Long, endMillis: Long): Flow<Long>
    fun getOutstandingAmount(cardId: Long): Flow<Long>

    // ── Bill Management ───────────────────────────────────────
    fun getBillsForCard(cardId: Long): Flow<List<CreditCardBill>>
    fun getUnpaidBills(cardId: Long): Flow<List<CreditCardBill>>
    fun getAllUnpaidBills(): Flow<List<CreditCardBill>>
    suspend fun saveBill(bill: CreditCardBill): Long

    /**
     * The core "Pay Bill" action.
     * 1. Creates a NORMAL/EXPENSE entry for the bill amount from [paidFromAccountId].
     * 2. Marks the bill as paid and links the generated expense.
     *
     * Returns the ID of the generated expense entry.
     */
    suspend fun markBillAsPaid(
        bill: CreditCardBill,
        paidFromAccountId: Long,
        ccPaymentCategoryId: Long
    ): Long

    /**
     * Generates (or retrieves existing) bill for the specified billing cycle.
     * Called lazily when the user opens the card detail screen.
     */
    suspend fun generateOrGetBillForCycle(
        card: CreditCard,
        cycleStartMillis: Long,
        cycleEndMillis: Long,
        dueDateMillis: Long
    ): CreditCardBill
}

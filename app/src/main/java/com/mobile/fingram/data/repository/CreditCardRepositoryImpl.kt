package com.mobile.fingram.data.repository

import com.mobile.fingram.data.local.dao.AccountDao
import com.mobile.fingram.data.local.dao.CreditCardDao
import com.mobile.fingram.data.local.dao.ExpenseDao
import com.mobile.fingram.data.local.entity.ExpenseEntity
import com.mobile.fingram.data.local.entity.ExpenseWithCategory
import com.mobile.fingram.data.local.entity.TransactionType
import com.mobile.fingram.di.IoDispatcher
import com.mobile.fingram.domain.model.CreditCard
import com.mobile.fingram.domain.model.CreditCardBill
import com.mobile.fingram.domain.model.toDomain
import com.mobile.fingram.domain.model.toEntity
import com.mobile.fingram.domain.repository.CreditCardRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreditCardRepositoryImpl @Inject constructor(
    private val creditCardDao: CreditCardDao,
    private val expenseDao: ExpenseDao,
    private val accountDao: AccountDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : CreditCardRepository {

    // ── Card CRUD ──────────────────────────────────────────────

    override fun getAllCreditCards(): Flow<List<CreditCard>> =
        creditCardDao.getAllCreditCards()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(io)

    override suspend fun getCreditCardById(id: Long): CreditCard? = withContext(io) {
        creditCardDao.getCreditCardById(id)?.toDomain()
    }

    override suspend fun saveCreditCard(card: CreditCard): Long = withContext(io) {
        val entity = card.toEntity()
        if (entity.id == 0L) {
            creditCardDao.insertCreditCard(entity)
        } else {
            creditCardDao.updateCreditCard(entity)
            entity.id
        }
    }

    override suspend fun deleteCreditCard(card: CreditCard) = withContext(io) {
        creditCardDao.deleteCreditCard(card.toEntity())
    }

    // ── Credit Spend Queries ──────────────────────────────────

    override fun getExpensesForCardInCycle(
        cardId: Long,
        startMillis: Long,
        endMillis: Long
    ): Flow<List<ExpenseWithCategory>> =
        creditCardDao.getExpensesForCardInCycle(cardId, startMillis, endMillis).flowOn(io)

    override fun getTotalSpendForCardInCycle(
        cardId: Long,
        startMillis: Long,
        endMillis: Long
    ): Flow<Long> =
        creditCardDao.getTotalSpendForCardInCycle(cardId, startMillis, endMillis).flowOn(io)

    override fun getOutstandingAmount(cardId: Long): Flow<Long> =
        creditCardDao.getOutstandingAmount(cardId).flowOn(io)

    // ── Bill Management ───────────────────────────────────────

    override fun getBillsForCard(cardId: Long): Flow<List<CreditCardBill>> =
        creditCardDao.getBillsForCard(cardId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(io)

    override fun getUnpaidBills(cardId: Long): Flow<List<CreditCardBill>> =
        creditCardDao.getUnpaidBills(cardId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(io)

    override fun getAllUnpaidBills(): Flow<List<CreditCardBill>> =
        creditCardDao.getAllUnpaidBills()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(io)

    override suspend fun saveBill(bill: CreditCardBill): Long = withContext(io) {
        val entity = bill.toEntity()
        if (entity.id == 0L) {
            creditCardDao.insertBill(entity)
        } else {
            creditCardDao.updateBill(entity)
            entity.id
        }
    }

    /**
     * Records a payment against a credit card bill.
     * 1. Creates a real EXPENSE entry for the amount.
     * 2. Updates the bill record (paidAmount, status, etc.).
     * 3. Deducts amount from the bank balance.
     */
    override suspend fun markBillAsPaid(
        bill: CreditCardBill,
        paidFromAccountId: Long,
        ccPaymentCategoryId: Long,
        amountCents: Long
    ): Long = withContext(io) {
        if (amountCents <= 0) {
            throw IllegalArgumentException("Invalid payment amount")
        }

        val remainingAmount = (bill.totalAmountCents - bill.paidAmountCents).coerceAtLeast(0L)
        if (remainingAmount <= 0L) {
            throw IllegalStateException("This bill is already paid")
        }
        if (amountCents > remainingAmount) {
            throw IllegalArgumentException("Payment amount cannot exceed the remaining due")
        }

        val now = System.currentTimeMillis()
        val sourceAccount = accountDao.getAccountById(paidFromAccountId)
            ?: throw IllegalArgumentException("Selected source account was not found")
        val card = creditCardDao.getCreditCardById(bill.cardId)
        val cardLabel = card?.let { "${it.bankName} ••••${it.lastFourDigits}" } ?: "credit card bill"

        // Record the bank debit as a normal expense so it appears in Transactions.
        val paymentExpense = ExpenseEntity(
            id = 0,
            type = "EXPENSE",
            transactionType = TransactionType.NORMAL,
            amountCents = amountCents,
            categoryId = ccPaymentCategoryId,
            accountId = paidFromAccountId,
            creditCardId = null,
            note = "Credit card payment from ${sourceAccount.name} for $cardLabel",
            dateMillis = now,
            createdAt = now
        )
        val expenseId = expenseDao.insertExpense(paymentExpense)

        // Debit the chosen bank account.
        accountDao.updateBalance(paidFromAccountId, -amountCents)

        // Update the bill with the payment that was recorded above.
        val newPaidAmount = bill.paidAmountCents + amountCents
        val isFullPayment = newPaidAmount >= bill.totalAmountCents
        
        val updatedBill = bill.toEntity().copy(
            paidAmountCents = newPaidAmount,
            isPaid = isFullPayment,
            status = if (isFullPayment) CreditCardBill.BILL_STATUS_PAID else CreditCardBill.BILL_STATUS_PARTIALLY_PAID,
            paidAt = now,
            paidFromAccountId = paidFromAccountId,
            generatedExpenseId = expenseId
        )
        creditCardDao.updateBill(updatedBill)

        expenseId
    }

    /**
     * Lazily generates a bill for the current billing cycle, or returns the
     * existing one if it was already generated. Calculates the total from
     * CREDIT-type transactions in that cycle range.
     */
    override suspend fun generateOrGetBillForCycle(
        card: CreditCard,
        cycleStartMillis: Long,
        cycleEndMillis: Long,
        dueDateMillis: Long
    ): CreditCardBill = withContext(io) {
        // Check if a bill already exists for this cycle
        val existingEntity = creditCardDao.getBillByCycleStart(card.id, cycleStartMillis)
        if (existingEntity != null) {
            return@withContext existingEntity.toDomain()
        }

        // Calculate total from CREDIT spends in the cycle
        val total = creditCardDao
            .getTotalSpendForCardInCycle(card.id, cycleStartMillis, cycleEndMillis)
            .first()

        // Even with 0 spend, we generate a lazy bill if card is active
        val newBill = CreditCardBill(
            id = 0,
            cardId = card.id,
            billingCycleStartDate = cycleStartMillis,
            billingCycleEndDate = cycleEndMillis,
            dueDateMillis = dueDateMillis,
            totalAmountCents = total,
            paidAmountCents = 0L,
            status = CreditCardBill.BILL_STATUS_OPEN,
            isPaid = false
        )
        val billId = creditCardDao.insertBill(newBill.toEntity())
        newBill.copy(id = billId)
    }
}

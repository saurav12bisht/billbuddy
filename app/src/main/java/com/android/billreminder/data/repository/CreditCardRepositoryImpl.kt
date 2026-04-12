package com.android.billreminder.data.repository

import com.android.billreminder.data.local.dao.CreditCardDao
import com.android.billreminder.data.local.entity.ExpenseWithCategory
import com.android.billreminder.di.IoDispatcher
import com.android.billreminder.domain.model.CreditCard
import com.android.billreminder.domain.model.CreditCardBill
import com.android.billreminder.domain.model.toDomain
import com.android.billreminder.domain.model.toEntity
import com.android.billreminder.domain.repository.CreditCardRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreditCardRepositoryImpl @Inject constructor(
    private val creditCardDao: CreditCardDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : CreditCardRepository {

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
    ): Flow<Long?> =
        creditCardDao.getTotalSpendForCardInCycle(cardId, startMillis, endMillis).flowOn(io)

    override fun getBillsForCard(cardId: Long): Flow<List<CreditCardBill>> =
        creditCardDao.getBillsForCard(cardId)
            .map { entities -> 
                entities.map { entity ->
                    CreditCardBill(
                        id = entity.id,
                        cardId = entity.cardId,
                        billingCycleStartDate = entity.billingCycleStartDate,
                        billingCycleEndDate = entity.billingCycleEndDate,
                        totalAmountCents = entity.totalAmountCents,
                        isPaid = entity.isPaid,
                        paidAt = entity.paidAt
                    )
                }
            }
            .flowOn(io)

    override suspend fun saveBill(bill: CreditCardBill): Long = withContext(io) {
        val entity = com.android.billreminder.data.local.entity.CreditCardBillEntity(
            id = bill.id,
            cardId = bill.cardId,
            billingCycleStartDate = bill.billingCycleStartDate,
            billingCycleEndDate = bill.billingCycleEndDate,
            totalAmountCents = bill.totalAmountCents,
            isPaid = bill.isPaid,
            paidAt = bill.paidAt
        )
        if (entity.id == 0L) {
            creditCardDao.insertBill(entity)
        } else {
            creditCardDao.updateBill(entity)
            entity.id
        }
    }
}

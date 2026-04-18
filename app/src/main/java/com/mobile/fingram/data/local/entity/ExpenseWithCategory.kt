package com.mobile.fingram.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ExpenseWithCategory(
    @Embedded val expense: ExpenseEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity,
    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    val account: AccountEntity,
    @Relation(
        parentColumn = "creditCardId",
        entityColumn = "id"
    )
    val creditCard: CreditCardEntity?
)

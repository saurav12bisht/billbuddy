package com.android.billreminder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credit_card_bills",
    foreignKeys = [
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId")]
)
data class CreditCardBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val billingCycleStartDate: Long,
    val billingCycleEndDate: Long,
    val dueDateMillis: Long,            // Actual due date for this bill
    val totalAmountCents: Long,
    val isPaid: Boolean = false,
    val paidAt: Long? = null,
    val paidFromAccountId: Long? = null, // Which account was used to pay
    val generatedExpenseId: Long? = null // The NORMAL expense entry created when paid
)

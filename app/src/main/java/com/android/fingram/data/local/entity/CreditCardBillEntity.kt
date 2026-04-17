package com.android.fingram.data.local.entity

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
    val dueDateMillis: Long,
    val totalAmountCents: Long,
    val paidAmountCents: Long = 0L,
    val minimumDueCents: Long? = null,
    val status: String = BILL_STATUS_OPEN,
    val isPaid: Boolean = false, // Kept for backward compatibility, though status is preferred
    val paidAt: Long? = null,
    val paidFromAccountId: Long? = null,
    val generatedExpenseId: Long? = null
) {
    companion object {
        const val BILL_STATUS_OPEN = "OPEN"
        const val BILL_STATUS_PARTIALLY_PAID = "PARTIALLY_PAID"
        const val BILL_STATUS_PAID = "PAID"
        const val BILL_STATUS_OVERDUE = "OVERDUE"
    }
}

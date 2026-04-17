package com.android.fingram.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.android.fingram.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getAllActiveCustomers(): Flow<List<CustomerEntity>>

    @Query("""
        SELECT c.*, (COALESCE((
            SELECT SUM(CASE WHEN t.type='CREDIT_GIVEN' THEN t.amountPaise WHEN t.type='PAYMENT_RECEIVED' THEN -t.amountPaise ELSE 0 END)
            FROM transactions t WHERE t.customerId = c.id
        ), 0) + c.openingBalance * CASE WHEN c.openingBalanceType = 'I_OWE' THEN -1 ELSE 1 END) AS balance
        FROM customers c
        WHERE c.isActive = 1
        ORDER BY balance DESC
    """)
    fun getCustomersWithBalance(): Flow<List<CustomerWithBalance>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Int): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone = :phone AND isActive = 1 LIMIT 1")
    suspend fun getCustomerByPhone(phone: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("UPDATE customers SET isActive = :active, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setActive(id: Int, active: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET isActive = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET updatedAt = :timestamp WHERE id = :id")
    suspend fun touch(id: Int, timestamp: Long = System.currentTimeMillis())
}

data class CustomerWithBalance(
    val id: Int,
    val name: String,
    val phone: String,
    val businessName: String,
    val address: String,
    val openingBalance: Long,
    val openingBalanceType: String,
    val photoPath: String?,
    val notes: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val balance: Long
) {
    fun toEntity(): CustomerEntity = CustomerEntity(
        id = id,
        name = name,
        phone = phone,
        businessName = businessName,
        address = address,
        openingBalance = openingBalance,
        openingBalanceType = openingBalanceType,
        photoPath = photoPath,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

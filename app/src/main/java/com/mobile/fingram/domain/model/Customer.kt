package com.mobile.fingram.domain.model

import com.mobile.fingram.data.local.dao.CustomerWithBalance
import com.mobile.fingram.data.local.entity.CustomerEntity

data class Customer(
    val id: Int,
    val name: String,
    val phone: String,
    val businessName: String = "",
    val address: String = "",
    val openingBalance: Long = 0L,
    val openingBalanceType: String = "THEY_OWE",
    val photoPath: String? = null,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
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

fun CustomerEntity.toDomain(): Customer = Customer(
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

fun CustomerWithBalance.toCustomer(): Customer = toEntity().toDomain()

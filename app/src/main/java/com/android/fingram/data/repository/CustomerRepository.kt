package com.android.fingram.data.repository

import com.android.fingram.data.local.dao.CustomerDao
import com.android.fingram.data.local.dao.CustomerWithBalance
import com.android.fingram.data.local.entity.CustomerEntity
import com.android.fingram.domain.model.Customer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import com.android.fingram.di.IoDispatcher
import javax.inject.Inject

class CustomerRepository @Inject constructor(
    private val dao: CustomerDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun getCustomersWithBalance(): Flow<List<CustomerWithBalance>> =
        dao.getCustomersWithBalance().flowOn(io)

    fun getAllActiveCustomers(): Flow<List<com.android.fingram.data.local.entity.CustomerEntity>> =
        dao.getAllActiveCustomers().flowOn(io)

    suspend fun getCustomerById(id: Int) = withContext(io) { dao.getCustomerById(id) }
    suspend fun getCustomerByPhone(phone: String) = withContext(io) { dao.getCustomerByPhone(phone) }

    suspend fun insert(customer: Customer): Long = withContext(io) {
        dao.insert(customer.toCustomerEntity())
    }

    suspend fun update(customer: Customer) = withContext(io) {
        dao.update(customer.toCustomerEntity())
    }

    private fun Customer.toCustomerEntity() = CustomerEntity(
        id = id, name = name, phone = phone, businessName = businessName, address = address,
        openingBalance = openingBalance, openingBalanceType = openingBalanceType, photoPath = photoPath,
        notes = notes, isActive = isActive, createdAt = createdAt, updatedAt = updatedAt
    )

    suspend fun softDelete(id: Int) = withContext(io) {
        dao.softDelete(id)
    }

    suspend fun touch(id: Int) = withContext(io) {
        dao.touch(id)
    }

    suspend fun getAllActiveCustomersList(): List<CustomerEntity> = withContext(io) {
        getAllActiveCustomers().first()
    }
}

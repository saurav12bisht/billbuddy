package com.mobile.fingram.di

import com.mobile.fingram.data.repository.ExpenseRepositoryImpl
import com.mobile.fingram.domain.repository.ExpenseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        expenseRepositoryImpl: ExpenseRepositoryImpl
    ): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindCreditCardRepository(
        creditCardRepositoryImpl: com.mobile.fingram.data.repository.CreditCardRepositoryImpl
    ): com.mobile.fingram.domain.repository.CreditCardRepository
}

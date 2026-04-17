package com.android.fingram.di

import com.android.fingram.data.repository.ExpenseRepositoryImpl
import com.android.fingram.domain.repository.ExpenseRepository
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
        creditCardRepositoryImpl: com.android.fingram.data.repository.CreditCardRepositoryImpl
    ): com.android.fingram.domain.repository.CreditCardRepository
}

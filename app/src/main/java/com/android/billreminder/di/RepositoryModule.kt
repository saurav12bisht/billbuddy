package com.android.billreminder.di

import com.android.billreminder.data.repository.ExpenseRepositoryImpl
import com.android.billreminder.domain.repository.ExpenseRepository
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
        creditCardRepositoryImpl: com.android.billreminder.data.repository.CreditCardRepositoryImpl
    ): com.android.billreminder.domain.repository.CreditCardRepository
}

package com.android.fingram.di

import android.content.Context
import com.android.fingram.data.local.FingramDatabase
import com.android.fingram.data.local.dao.BillDao
import com.android.fingram.data.local.dao.CustomerDao
import com.android.fingram.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FingramDatabase =
        FingramDatabase.getInstance(context)

    @Provides
    fun provideCustomerDao(db: FingramDatabase): CustomerDao = db.customerDao()

    @Provides
    fun provideTransactionDao(db: FingramDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideBillDao(db: FingramDatabase): BillDao = db.billDao()

    @Provides
    fun provideExpenseDao(db: FingramDatabase): com.android.fingram.data.local.dao.ExpenseDao = db.expenseDao()

    @Provides
    fun provideAccountDao(db: FingramDatabase): com.android.fingram.data.local.dao.AccountDao = db.accountDao()

    @Provides
    fun provideCreditCardDao(db: FingramDatabase): com.android.fingram.data.local.dao.CreditCardDao = db.creditCardDao()
}

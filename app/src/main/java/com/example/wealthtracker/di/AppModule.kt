package com.example.wealthtracker.di

import android.content.Context
import androidx.room.Room
import com.example.wealthtracker.data.local.InvestmentDao
import com.example.wealthtracker.data.local.WealthTrackerDatabase
import com.example.wealthtracker.data.local.MIGRATION_1_2
import com.example.wealthtracker.data.repository.DefaultInvestmentRepository
import com.example.wealthtracker.data.repository.InvestmentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindModule {
    @Binds
    @Singleton
    abstract fun bindInvestmentRepository(impl: DefaultInvestmentRepository): InvestmentRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WealthTrackerDatabase =
        Room.databaseBuilder(context, WealthTrackerDatabase::class.java, "wealth_tracker.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideInvestmentDao(db: WealthTrackerDatabase): InvestmentDao = db.investmentDao()
}

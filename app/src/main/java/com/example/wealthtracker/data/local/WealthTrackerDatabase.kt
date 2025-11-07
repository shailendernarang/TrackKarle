package com.example.wealthtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [InvestmentEntity::class],
    version = 2,
    exportSchema = false
)
abstract class WealthTrackerDatabase : RoomDatabase() {
    abstract fun investmentDao(): InvestmentDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE investments ADD COLUMN investmentType TEXT NOT NULL DEFAULT 'Others'")
        db.execSQL("ALTER TABLE investments ADD COLUMN bankName TEXT")
    }
}

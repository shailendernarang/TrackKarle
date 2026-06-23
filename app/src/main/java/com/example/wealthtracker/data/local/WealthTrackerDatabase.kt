package com.example.wealthtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [InvestmentEntity::class, DebtEntity::class],
    version = 4,
    exportSchema = false
)
abstract class WealthTrackerDatabase : RoomDatabase() {
    abstract fun investmentDao(): InvestmentDao
    abstract fun debtDao(): DebtDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE investments ADD COLUMN investmentType TEXT NOT NULL DEFAULT 'Others'")
        db.execSQL("ALTER TABLE investments ADD COLUMN bankName TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE investments ADD COLUMN fdStartDate TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN fdMaturityDate TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN fdRate REAL")
        db.execSQL("ALTER TABLE investments ADD COLUMN fdTenure TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN mfDate TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN ppfFy TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN ppfDate TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN npsTier TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN npsDate TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN goldType TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN goldDate TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN hiPolicyName TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN hiRenewalDate TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN stockName TEXT")
        db.execSQL("ALTER TABLE investments ADD COLUMN stockDate TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS debts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, debtType TEXT NOT NULL, principalAmount REAL NOT NULL, outstandingBalance REAL NOT NULL, interestRate REAL NOT NULL, emiAmount REAL NOT NULL, startDate TEXT NOT NULL DEFAULT '', tenureMonths INTEGER NOT NULL DEFAULT 0, createdAt INTEGER NOT NULL)")
    }
}

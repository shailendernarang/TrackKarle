package com.example.wealthtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [InvestmentEntity::class],
    version = 3,
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

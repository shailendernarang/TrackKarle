package com.example.wealthtracker

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.wealthtracker.data.local.MIGRATION_1_2
import com.example.wealthtracker.data.local.MIGRATION_2_3
import com.example.wealthtracker.data.local.MIGRATION_3_4
import com.example.wealthtracker.data.local.MIGRATION_4_5
import com.example.wealthtracker.data.local.WealthTrackerDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies every Room migration from v1 → v5.
 *
 * Strategy: since exportSchema was disabled for v1–v4, there are no stored schema JSON files.
 * We create old database files manually via SQLiteDatabase, set user_version, then open with
 * Room supplying the required migrations. Room detects the file version and applies only the
 * steps needed.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WealthTrackerDatabase::class.java
    )

    // ── helpers ──────────────────────────────────────────────────────────────────

    private fun createRawDb(name: String, version: Int, setup: SQLiteDatabase.() -> Unit) {
        context.deleteDatabase(name)
        context.getDatabasePath(name).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath(name).absolutePath, null
        ).use { db ->
            db.setup()
            db.version = version
        }
    }

    private fun openWithRoom(
        name: String,
        vararg migrations: androidx.room.migration.Migration
    ): WealthTrackerDatabase =
        Room.databaseBuilder(context, WealthTrackerDatabase::class.java, name)
            .addMigrations(*migrations)
            .build()

    /** Wraps SupportSQLiteDatabase.query() result as android.database.Cursor for easy use. */
    @Suppress("UNCHECKED_CAST")
    private fun rawQuery(db: WealthTrackerDatabase, sql: String): Cursor =
        db.openHelper.writableDatabase.query(sql) as Cursor

    // ── schema helpers ────────────────────────────────────────────────────────────

    private fun SQLiteDatabase.createV1Schema() {
        execSQL(
            "CREATE TABLE investments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "type TEXT NOT NULL, amount REAL NOT NULL, createdAt INTEGER NOT NULL)"
        )
    }

    private fun SQLiteDatabase.createV4Schema() {
        execSQL(
            "CREATE TABLE IF NOT EXISTS investments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "type TEXT NOT NULL, amount REAL NOT NULL, createdAt INTEGER NOT NULL, " +
                "investmentType TEXT NOT NULL DEFAULT 'Others', bankName TEXT, " +
                "fdStartDate TEXT, fdMaturityDate TEXT, fdRate REAL, fdTenure TEXT, " +
                "mfDate TEXT, ppfFy TEXT, ppfDate TEXT, " +
                "npsTier TEXT, npsDate TEXT, goldType TEXT, goldDate TEXT, " +
                "hiPolicyName TEXT, hiRenewalDate TEXT, stockName TEXT, stockDate TEXT)"
        )
        execSQL(
            "CREATE TABLE IF NOT EXISTS debts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, debtType TEXT NOT NULL, " +
                "principalAmount REAL NOT NULL, outstandingBalance REAL NOT NULL, " +
                "interestRate REAL NOT NULL, emiAmount REAL NOT NULL, " +
                "startDate TEXT NOT NULL DEFAULT '', " +
                "tenureMonths INTEGER NOT NULL DEFAULT 0, createdAt INTEGER NOT NULL)"
        )
    }

    // ── tests ─────────────────────────────────────────────────────────────────────

    /**
     * v1 → v2: investmentType added with default 'Others'; bankName nullable.
     * Pre-existing row must survive.
     */
    @Test
    fun migration1To2_addsColumnsAndPreservesExistingRow() {
        val name = "test_1_2"
        createRawDb(name, 1) {
            createV1Schema()
            execSQL("INSERT INTO investments (type, amount, createdAt) VALUES ('FD', 10000.0, 1700000000000)")
        }

        val db = openWithRoom(name, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        try {
            rawQuery(db, "SELECT type, amount, investmentType, bankName FROM investments LIMIT 1").also { c ->
                assertTrue("Row must survive migration", c.moveToFirst())
                assertEquals("FD", c.getString(c.getColumnIndexOrThrow("type")))
                assertEquals(10000.0, c.getDouble(c.getColumnIndexOrThrow("amount")), 0.0)
                assertEquals("Others", c.getString(c.getColumnIndexOrThrow("investmentType")))
                assertTrue("bankName must be null", c.isNull(c.getColumnIndexOrThrow("bankName")))
                c.close()
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    /**
     * v2 → v3: detail columns added (fdRate, stockName, goldType, etc.) — all nullable.
     */
    @Test
    fun migration2To3_addsDetailColumnsNullable() {
        val name = "test_2_3"
        createRawDb(name, 1) {
            createV1Schema()
            execSQL("INSERT INTO investments (type, amount, createdAt) VALUES ('Gold', 50000.0, 1700000000001)")
        }

        val db = openWithRoom(name, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        try {
            rawQuery(db, "SELECT type, amount, stockName, fdRate, goldType FROM investments LIMIT 1").also { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Gold", c.getString(c.getColumnIndexOrThrow("type")))
                assertTrue("stockName must be null", c.isNull(c.getColumnIndexOrThrow("stockName")))
                assertTrue("fdRate must be null", c.isNull(c.getColumnIndexOrThrow("fdRate")))
                assertTrue("goldType must be null", c.isNull(c.getColumnIndexOrThrow("goldType")))
                c.close()
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    /**
     * v3 → v4: debts table created and must be queryable and empty.
     */
    @Test
    fun migration3To4_createsDebtsTable() {
        val name = "test_3_4"
        createRawDb(name, 1) { createV1Schema() }

        val db = openWithRoom(name, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        try {
            rawQuery(db, "SELECT COUNT(*) FROM debts").also { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
                c.close()
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    /**
     * v4 → v5: the critical Stocks → Equity normalisation.
     *
     * - investmentType = 'Stocks' must become 'Equity'
     * - type column (stock display name like "Apple Inc.") must be untouched
     * - records already stored as 'Equity' must not change
     * - records of other types (FD, etc.) must not change
     * - no rows with investmentType = 'Stocks' must remain
     */
    @Test
    fun migration4To5_stocksNormalisedToEquity() {
        val name = "test_4_5"
        createRawDb(name, 4) {
            createV4Schema()
            // Bug case: saved with investmentType = 'Stocks' (the display label)
            execSQL("INSERT INTO investments (type, amount, createdAt, investmentType, stockName) VALUES ('Apple Inc.', 50000.0, 1700000000000, 'Stocks', 'Apple Inc.')")
            // Already correct
            execSQL("INSERT INTO investments (type, amount, createdAt, investmentType, stockName) VALUES ('Infosys', 30000.0, 1700000000001, 'Equity', 'Infosys')")
            // Unrelated type — must be untouched
            execSQL("INSERT INTO investments (type, amount, createdAt, investmentType, bankName) VALUES ('FD', 100000.0, 1700000000002, 'FD', 'HDFC Bank')")
        }

        val db = openWithRoom(name, MIGRATION_4_5)
        try {
            // 'Stocks' → 'Equity'; display name preserved
            rawQuery(db, "SELECT investmentType, stockName FROM investments WHERE stockName = 'Apple Inc.'").also { c ->
                assertTrue(c.moveToFirst())
                assertEquals(
                    "investmentType 'Stocks' must be normalised to 'Equity'",
                    "Equity", c.getString(c.getColumnIndexOrThrow("investmentType"))
                )
                assertEquals("Apple Inc.", c.getString(c.getColumnIndexOrThrow("stockName")))
                c.close()
            }

            // Already 'Equity' — unchanged
            rawQuery(db, "SELECT investmentType FROM investments WHERE stockName = 'Infosys'").also { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Equity", c.getString(0))
                c.close()
            }

            // FD — unchanged
            rawQuery(db, "SELECT investmentType FROM investments WHERE bankName = 'HDFC Bank'").also { c ->
                assertTrue(c.moveToFirst())
                assertEquals("FD", c.getString(0))
                c.close()
            }

            // No 'Stocks' rows remain
            rawQuery(db, "SELECT COUNT(*) FROM investments WHERE investmentType = 'Stocks'").also { c ->
                assertTrue(c.moveToFirst())
                assertEquals("No 'Stocks' rows must remain after migration", 0, c.getInt(0))
                c.close()
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    /**
     * v4 → v5 with no investments: must succeed on an empty table.
     */
    @Test
    fun migration4To5_emptyTable_succeeds() {
        val name = "test_4_5_empty"
        createRawDb(name, 4) { createV4Schema() }

        val db = openWithRoom(name, MIGRATION_4_5)
        try {
            rawQuery(db, "SELECT COUNT(*) FROM investments").also { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
                c.close()
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    /**
     * Full chain v1 → v5: a database from the very first install must open cleanly
     * and all data must survive all 4 migrations in sequence.
     */
    @Test
    fun fullMigrationChain_v1ToV5_dataIntact() {
        val name = "test_full_chain"
        createRawDb(name, 1) {
            createV1Schema()
            execSQL("INSERT INTO investments (type, amount, createdAt) VALUES ('FD', 25000.0, 1680000000000)")
            execSQL("INSERT INTO investments (type, amount, createdAt) VALUES ('Gold', 75000.0, 1680000000001)")
        }

        val db = openWithRoom(name, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        try {
            // Both rows survived
            rawQuery(db, "SELECT COUNT(*) FROM investments").also { c ->
                assertTrue(c.moveToFirst())
                assertEquals(2, c.getInt(0))
                c.close()
            }

            // Amount preserved
            rawQuery(db, "SELECT amount FROM investments WHERE type = 'FD'").also { c ->
                assertTrue(c.moveToFirst())
                assertEquals(25000.0, c.getDouble(0), 0.0)
                c.close()
            }

            // Debts table accessible
            rawQuery(db, "SELECT COUNT(*) FROM debts").also { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
                c.close()
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}

package com.example.wealthtracker.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.sqlite.db.SupportSQLiteOpenHelper
import kotlinx.coroutines.runBlocking
import com.example.wealthtracker.data.local.InvestmentDao
import com.example.wealthtracker.data.local.WealthTrackerDatabase
import com.example.wealthtracker.data.local.MIGRATION_1_2
import com.example.wealthtracker.data.local.MIGRATION_2_3
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
    private const val ENC_PREFS = "enc_prefs"
    private const val DB_KEY = "db_key"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WealthTrackerDatabase {
        // Try to create SQLCipher SupportFactory via reflection if available (secure flavor)
        val factory: SupportSQLiteOpenHelper.Factory? = runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                ENC_PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            var keyB64 = prefs.getString(DB_KEY, null)
            if (keyB64 == null) {
                val bytes = ByteArray(32)
                java.security.SecureRandom().nextBytes(bytes)
                keyB64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                prefs.edit().putString(DB_KEY, keyB64).apply()
            }
            val passphrase: ByteArray = android.util.Base64.decode(keyB64, android.util.Base64.NO_WRAP)
            val clazz = Class.forName("net.sqlcipher.database.SupportFactory")
            val ctor = clazz.getConstructor(ByteArray::class.java)
            val instance = ctor.newInstance(passphrase)
            instance as SupportSQLiteOpenHelper.Factory
        }.getOrNull()

        val builder = Room.databaseBuilder(
            context,
            WealthTrackerDatabase::class.java,
            if (factory != null) "wealth_tracker_encrypted.db" else "wealth_tracker.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        if (factory != null) builder.openHelperFactory(factory)
        val db = builder.build()

        // Log backend selection (encrypted/plain)
        Log.d("DB", "backend=${if (factory != null) "encrypted" else "plain"}")

        // One-time offline migration from old plain DB to new encrypted DB (secure flavor only)
        if (factory != null) {
            val migPrefs = context.getSharedPreferences("db_migration_flags", Context.MODE_PRIVATE)
            val alreadyMigrated = migPrefs.getBoolean("migrated_to_encrypted", false)
            if (!alreadyMigrated) {
                val oldMain = context.getDatabasePath("wealth_tracker.db")
                if (oldMain.exists()) {
                    runBlocking {
                        // Open old DB without encryption and copy rows into encrypted DB
                        val oldDb = Room.databaseBuilder(context, WealthTrackerDatabase::class.java, "wealth_tracker.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build()
                        var copied = false
                        try {
                            val oldDao = oldDb.investmentDao()
                            val rows = runCatching { oldDao.getAllOnce() }.getOrDefault(emptyList())
                            if (rows.isNotEmpty()) {
                                val newDao = db.investmentDao()
                                rows.forEach { newDao.insert(it.copy(id = 0)) }
                                // Log migration copy count
                                Log.d("DB", "db_migration_copied rows=${rows.size}")
                                copied = true
                            }
                        } finally {
                            oldDb.close()
                            // Remove old unencrypted DB files only if we copied rows
                            if (copied) {
                                listOf("wealth_tracker.db", "wealth_tracker.db-shm", "wealth_tracker.db-wal").forEach { name ->
                                    kotlin.runCatching { context.getDatabasePath(name).delete() }
                                }
                                Log.d("DB", "db_migration_cleanup deleted old plain DB files")
                            } else {
                                Log.d("DB", "db_migration_noop: no rows to copy; leaving old plain DB files intact")
                            }
                        }
                    }
                }
                // Set flag only when encrypted DB now has data (or old had no rows but we still want to avoid re-check loops)
                // To be safe, set the flag regardless to avoid repeated attempts; the cleanup above is conditional.
                Log.d("DB", "db_migration_flag_set")
                migPrefs.edit().putBoolean("migrated_to_encrypted", true).apply()
            }

            // Reverse sync safeguard: if user later installs Play (plain) build, ensure plain DB has data.
            runCatching {
                val plainPath = context.getDatabasePath("wealth_tracker.db")
                if (plainPath.exists()) {
                    // Open both DBs: encrypted (db) and plain
                    val plainDb = Room.databaseBuilder(context, WealthTrackerDatabase::class.java, "wealth_tracker.db")
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                        .build()
                    try {
                        val encDao = db.investmentDao()
                        val plainDao = plainDb.investmentDao()
                        val encRows = runBlocking { runCatching { encDao.getAllOnce() }.getOrDefault(emptyList()) }
                        val plainRows = runBlocking { runCatching { plainDao.getAllOnce() }.getOrDefault(emptyList()) }
                        if (encRows.isNotEmpty()) {
                            val plainKeys = plainRows.map { Triple(it.createdAt, it.amount, it.type) }.toHashSet()
                            val missing = encRows.filter { Triple(it.createdAt, it.amount, it.type) !in plainKeys }
                            if (missing.isNotEmpty()) {
                                runBlocking {
                                    missing.forEach { plainDao.insert(it.copy(id = 0)) }
                                }
                                Log.d("DB", "reverse_sync_added missing enc->plain rows=${missing.size} (enc=${encRows.size}, plain_before=${plainRows.size})")
                            } else {
                                Log.d("DB", "reverse_sync_noop (all present) enc=${encRows.size} plain=${plainRows.size}")
                            }
                        } else {
                            Log.d("DB", "reverse_sync_noop (enc empty)")
                        }
                    } finally {
                        plainDb.close()
                    }
                }
            }
        } else {
            Log.d("DB", "backend=plain")
            // Plain backend: ensure we pull any newer data from encrypted DB if present.
            runCatching {
                val encPath = context.getDatabasePath("wealth_tracker_encrypted.db")
                if (encPath.exists()) {
                    // Open encrypted DB with factory and compare (build factory via reflection, same as above)
                    val encFactory: SupportSQLiteOpenHelper.Factory? = runCatching {
                        val masterKey = MasterKey.Builder(context)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build()
                        val prefs = EncryptedSharedPreferences.create(
                            context,
                            ENC_PREFS,
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )
                        var keyB64 = prefs.getString(DB_KEY, null)
                        if (keyB64 == null) {
                            val bytes = ByteArray(32)
                            java.security.SecureRandom().nextBytes(bytes)
                            keyB64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                            prefs.edit().putString(DB_KEY, keyB64).apply()
                        }
                        val passphrase: ByteArray = android.util.Base64.decode(keyB64, android.util.Base64.NO_WRAP)
                        val clazz = Class.forName("net.sqlcipher.database.SupportFactory")
                        val ctor = clazz.getConstructor(ByteArray::class.java)
                        val instance = ctor.newInstance(passphrase)
                        instance as SupportSQLiteOpenHelper.Factory
                    }.getOrNull()
                    if (encFactory == null) {
                        Log.d("DB", "plain_pull_skip: enc factory unavailable")
                        return@runCatching
                    }
                    val encDb = Room.databaseBuilder(context, WealthTrackerDatabase::class.java, "wealth_tracker_encrypted.db")
                        .openHelperFactory(encFactory)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                        .build()
                    try {
                        val plainDao = db.investmentDao()
                        val encDao = encDb.investmentDao()
                        val plainRows = runBlocking { runCatching { plainDao.getAllOnce() }.getOrDefault(emptyList()) }
                        val encRows = runBlocking { runCatching { encDao.getAllOnce() }.getOrDefault(emptyList()) }
                        if (encRows.isNotEmpty()) {
                            val plainKeys = plainRows.map { Triple(it.createdAt, it.amount, it.type) }.toHashSet()
                            val missing = encRows.filter { Triple(it.createdAt, it.amount, it.type) !in plainKeys }
                            if (missing.isNotEmpty()) {
                                runBlocking { missing.forEach { plainDao.insert(it.copy(id = 0)) } }
                                Log.d("DB", "plain_pull_added missing enc->plain rows=${missing.size} (enc=${encRows.size}, plain_before=${plainRows.size})")
                            } else {
                                Log.d("DB", "plain_pull_noop (all present) enc=${encRows.size} plain=${plainRows.size}")
                            }
                        } else {
                            Log.d("DB", "plain_pull_noop (enc empty)")
                        }
                    } finally {
                        encDb.close()
                    }
                }
            }
        }
        return db
    }

    @Provides
    @Singleton
    fun provideInvestmentDao(db: WealthTrackerDatabase): InvestmentDao = db.investmentDao()
}

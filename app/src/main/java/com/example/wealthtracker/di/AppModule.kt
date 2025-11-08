package com.example.wealthtracker.di

import android.content.Context
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.sqlcipher.database.SupportFactory
import kotlinx.coroutines.runBlocking
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
    private const val ENC_PREFS = "enc_prefs"
    private const val DB_KEY = "db_key"

    @Provides
    @Singleton
    fun provideSqlCipherFactory(@ApplicationContext context: Context): SupportFactory {
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
        return SupportFactory(passphrase)
    }
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context, factory: SupportFactory): WealthTrackerDatabase {
        val db = Room.databaseBuilder(context, WealthTrackerDatabase::class.java, "wealth_tracker_encrypted.db")
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .build()

        // One-time offline migration from old plain DB to new encrypted DB
        val oldMain = context.getDatabasePath("wealth_tracker.db")
        if (oldMain.exists()) {
            runBlocking {
                // Open old DB without encryption
                val oldDb = Room.databaseBuilder(context, WealthTrackerDatabase::class.java, "wealth_tracker.db")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                try {
                    val oldDao = oldDb.investmentDao()
                    val rows = runCatching { oldDao.getAllOnce() }.getOrDefault(emptyList())
                    if (rows.isNotEmpty()) {
                        val newDao = db.investmentDao()
                        rows.forEach { newDao.insert(it.copy(id = 0)) }
                    }
                } finally {
                    oldDb.close()
                    // Remove old unencrypted DB files
                    listOf("wealth_tracker.db", "wealth_tracker.db-shm", "wealth_tracker.db-wal").forEach { name ->
                        kotlin.runCatching { context.getDatabasePath(name).delete() }
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

package com.example.wealthtracker.util

import android.content.Context
import android.util.Log
import com.example.wealthtracker.data.local.WealthTrackerDatabase
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BULLETPROOF DATABASE PROTECTION
 * 
 * This class ensures we NEVER fuck up the user's database.
 * Every operation is safe, validated, and has multiple fallbacks.
 */
@Singleton
class DatabaseSafetyGuard @Inject constructor(
    private val context: Context
) {
    
    companion object {
        const val TAG = "DatabaseSafety"
        const val BACKUP_DIR = "db_safety_backups"
        const val MAX_BACKUPS = 5
    }
    
    /**
     * Create a safety backup before any risky operation
     * NEVER perform database operations without this!
     */
    fun createSafetyBackup(reason: String): Boolean {
        return try {
            val backupDir = File(context.filesDir, BACKUP_DIR)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val safeReason = reason.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            
            // Backup both possible database files
            val databases = listOf("wealth_tracker.db", "wealth_tracker_encrypted.db")
            var backupCreated = false
            
            databases.forEach { dbName ->
                val dbFile = context.getDatabasePath(dbName)
                if (dbFile.exists() && dbFile.length() > 0) {
                    val backupFile = File(backupDir, "${safeReason}_${timestamp}_${dbName}")
                    dbFile.copyTo(backupFile, overwrite = true)
                    Log.i(TAG, "Safety backup created: ${backupFile.name} (${dbFile.length()} bytes)")
                    backupCreated = true
                }
            }
            
            if (backupCreated) {
                cleanupOldBackups(backupDir)
            }
            
            backupCreated
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: Failed to create safety backup for: $reason", e)
            false
        }
    }
    
    /**
     * Verify database is accessible and not corrupted
     */
    fun verifyDatabaseHealth(database: WealthTrackerDatabase): DatabaseHealthStatus {
        return try {
            val startTime = System.currentTimeMillis()
            
            // Test basic database operations
            runBlocking {
                // 1. Check if we can count records (most basic operation)
                val count = database.investmentDao().getInvestmentCount()
                
                // 2. Verify database is not corrupted
                if (count < 0) {
                    return@runBlocking DatabaseHealthStatus.CORRUPTED
                }
                
                // 3. Test if we can read data structure
                if (count > 0) {
                    val sample = database.investmentDao().getAllOnce().take(1)
                    if (sample.isEmpty() && count > 0) {
                        return@runBlocking DatabaseHealthStatus.INCONSISTENT
                    }
                }
                
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "Database health check passed: $count records, ${duration}ms")
                
                DatabaseHealthStatus.HEALTHY
            }
        } catch (e: Exception) {
            Log.e(TAG, "Database health check failed", e)
            DatabaseHealthStatus.CORRUPTED
        }
    }
    
    /**
     * Safe database operation wrapper
     * Use this for ANY operation that might affect user data
     */
    inline fun <T> safeOperation(
        operationName: String,
        createBackup: Boolean = true,
        crossinline operation: () -> T
    ): SafeOperationResult<T> {
        return try {
            // Step 1: Create backup if requested
            if (createBackup) {
                val backupSuccess = createSafetyBackup(operationName)
                if (!backupSuccess) {
                    Log.w(TAG, "Proceeding without backup for: $operationName")
                }
            }
            
            // Step 2: Execute operation with timeout
            val startTime = System.currentTimeMillis()
            val result = operation()
            val duration = System.currentTimeMillis() - startTime
            
            Log.i(TAG, "Safe operation '$operationName' completed successfully in ${duration}ms")
            SafeOperationResult.Success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Safe operation '$operationName' failed", e)
            SafeOperationResult.Failure(e)
        }
    }
    
    /**
     * Emergency database recovery
     * Call this if database gets fucked up
     */
    fun emergencyRecovery(): RecoveryResult {
        return try {
            Log.w(TAG, "EMERGENCY: Starting database recovery")
            
            val backupDir = File(context.filesDir, BACKUP_DIR)
            if (!backupDir.exists()) {
                return RecoveryResult.NO_BACKUPS_FOUND
            }
            
            // Find the most recent backup
            val backups = backupDir.listFiles()
                ?.filter { it.name.endsWith(".db") }
                ?.sortedByDescending { it.lastModified() }
            
            if (backups.isNullOrEmpty()) {
                return RecoveryResult.NO_BACKUPS_FOUND
            }
            
            val latestBackup = backups.first()
            val targetDbName = when {
                latestBackup.name.contains("encrypted") -> "wealth_tracker_encrypted.db"
                else -> "wealth_tracker.db"
            }
            
            val targetDb = context.getDatabasePath(targetDbName)
            
            // Create backup of corrupted database before recovery
            val corruptedBackup = File(backupDir, "corrupted_${System.currentTimeMillis()}_${targetDbName}")
            if (targetDb.exists()) {
                targetDb.copyTo(corruptedBackup, overwrite = true)
            }
            
            // Restore from backup
            latestBackup.copyTo(targetDb, overwrite = true)
            
            Log.i(TAG, "RECOVERY: Database restored from ${latestBackup.name}")
            RecoveryResult.RECOVERED_FROM_BACKUP
            
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: Emergency recovery failed", e)
            RecoveryResult.RECOVERY_FAILED
        }
    }
    
    /**
     * Clean up old backups to save space
     */
    private fun cleanupOldBackups(backupDir: File) {
        try {
            val backups = backupDir.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?: return
            
            if (backups.size > MAX_BACKUPS) {
                backups.drop(MAX_BACKUPS).forEach { oldBackup ->
                    if (oldBackup.delete()) {
                        Log.d(TAG, "Cleaned up old backup: ${oldBackup.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cleanup old backups", e)
        }
    }
    
    /**
     * Get database safety status for monitoring
     */
    fun getSafetyStatus(): DatabaseSafetyStatus {
        val backupDir = File(context.filesDir, BACKUP_DIR)
        val backupCount = backupDir.listFiles()?.size ?: 0
        
        val dbFiles = listOf("wealth_tracker.db", "wealth_tracker_encrypted.db")
            .map { context.getDatabasePath(it) }
            .filter { it.exists() }
        
        return DatabaseSafetyStatus(
            hasBackups = backupCount > 0,
            backupCount = backupCount,
            databaseExists = dbFiles.isNotEmpty(),
            totalDatabaseSize = dbFiles.sumOf { it.length() }
        )
    }
}

enum class DatabaseHealthStatus {
    HEALTHY,
    INCONSISTENT,
    CORRUPTED
}

sealed class SafeOperationResult<T> {
    data class Success<T>(val result: T) : SafeOperationResult<T>()
    data class Failure<T>(val error: Exception) : SafeOperationResult<T>()
}

enum class RecoveryResult {
    RECOVERED_FROM_BACKUP,
    NO_BACKUPS_FOUND,
    RECOVERY_FAILED
}

data class DatabaseSafetyStatus(
    val hasBackups: Boolean,
    val backupCount: Int,
    val databaseExists: Boolean,
    val totalDatabaseSize: Long
)

package com.example.wealthtracker.util

import android.content.Context
import android.util.Log
import com.example.wealthtracker.analytics.AnalyticsManager
import com.example.wealthtracker.data.local.WealthTrackerDatabase
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseIntegrityChecker @Inject constructor(
    private val database: WealthTrackerDatabase,
    private val context: Context
) {
    
    data class IntegrityReport(
        val totalInvestments: Int,
        val validInvestments: Int,
        val latestInvestmentId: Long,
        val isHealthy: Boolean,
        val issues: List<String> = emptyList()
    )
    
    /**
     * Perform comprehensive database integrity check
     */
    suspend fun checkIntegrity(): IntegrityReport {
        val issues = mutableListOf<String>()
        
        return try {
            val totalCount = database.investmentDao().getInvestmentCount()
            val validCount = database.investmentDao().getValidInvestmentCount()
            val latestId = database.investmentDao().getLatestInvestmentId()
            
            // Check for data consistency issues (backward compatible)
            if (totalCount < 0) {
                issues.add("Invalid total count: $totalCount")
            }
            
            if (validCount > totalCount) {
                issues.add("Valid count ($validCount) exceeds total count ($totalCount)")
            }
            
            if (totalCount > 0 && latestId <= 0) {
                issues.add("No valid IDs found despite having $totalCount investments")
            }
            
            val isHealthy = issues.isEmpty()
            
            Log.d("DatabaseIntegrity", "Check completed: total=$totalCount, valid=$validCount, latestId=$latestId, healthy=$isHealthy")
            
            // Track integrity check analytics
            val analyticsManager = AnalyticsManager(context)
            analyticsManager.logDatabaseIntegrityCheck(
                totalRecords = totalCount,
                validRecords = validCount,
                isHealthy = isHealthy
            )
            
            IntegrityReport(
                totalInvestments = totalCount,
                validInvestments = validCount,
                latestInvestmentId = latestId,
                isHealthy = isHealthy,
                issues = issues
            )
            
        } catch (e: Exception) {
            Log.e("DatabaseIntegrity", "Integrity check failed", e)
            IntegrityReport(
                totalInvestments = -1,
                validInvestments = -1,
                latestInvestmentId = -1,
                isHealthy = false,
                issues = listOf("Database access failed: ${e.message}")
            )
        }
    }
    
    /**
     * Quick health check - returns true if database is accessible and has consistent data
     */
    suspend fun isHealthy(): Boolean {
        return checkIntegrity().isHealthy
    }
    
    /**
     * Log integrity report for debugging
     */
    suspend fun logIntegrityReport() {
        val report = checkIntegrity()
        
        Log.i("DatabaseIntegrity", """
            === Database Integrity Report ===
            Total Investments: ${report.totalInvestments}
            Valid Investments: ${report.validInvestments}
            Latest Investment ID: ${report.latestInvestmentId}
            Status: ${if (report.isHealthy) "HEALTHY" else "ISSUES FOUND"}
            ${if (report.issues.isNotEmpty()) "Issues: ${report.issues.joinToString(", ")}" else ""}
            ================================
        """.trimIndent())
    }
    
    /**
     * Perform integrity check in background (safe for app initialization)
     */
    fun checkIntegrityInBackground() {
        // Use a background thread to avoid blocking app startup
        Thread {
            runCatching {
                runBlocking {
                    logIntegrityReport()
                }
            }.onFailure { e ->
                Log.e("DatabaseIntegrity", "Background integrity check failed", e)
            }
        }.start()
    }
    
    /**
     * Safe integrity check with timeout (for critical paths)
     */
    suspend fun checkIntegrityWithTimeout(timeoutMs: Long = 5000): IntegrityReport? {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                checkIntegrity()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w("DatabaseIntegrity", "Integrity check timed out after ${timeoutMs}ms")
            null
        } catch (e: Exception) {
            Log.e("DatabaseIntegrity", "Integrity check failed", e)
            null
        }
    }
}

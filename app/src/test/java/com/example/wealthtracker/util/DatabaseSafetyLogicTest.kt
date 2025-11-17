package com.example.wealthtracker.util

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Simple unit tests for database safety logic
 * Tests core data structures and logic without complex mocking
 */
class DatabaseSafetyLogicTest {

    @Test
    fun `integrity report data structure works correctly`() {
        // Test healthy report
        val healthyReport = DatabaseIntegrityChecker.IntegrityReport(
            totalInvestments = 100,
            validInvestments = 100,
            latestInvestmentId = 100L,
            isHealthy = true,
            issues = emptyList()
        )
        
        assertTrue(healthyReport.isHealthy)
        assertEquals(100, healthyReport.totalInvestments)
        assertEquals(100, healthyReport.validInvestments)
        assertEquals(100L, healthyReport.latestInvestmentId)
        assertTrue(healthyReport.issues.isEmpty())
    }

    @Test
    fun `integrity report detects issues correctly`() {
        // Test unhealthy report
        val unhealthyReport = DatabaseIntegrityChecker.IntegrityReport(
            totalInvestments = 100,
            validInvestments = 120, // More valid than total - impossible!
            latestInvestmentId = 100L,
            isHealthy = false,
            issues = listOf("Data inconsistency detected", "Invalid count")
        )
        
        assertFalse(unhealthyReport.isHealthy)
        assertEquals(100, unhealthyReport.totalInvestments)
        assertEquals(120, unhealthyReport.validInvestments)
        assertEquals(2, unhealthyReport.issues.size)
        assertTrue(unhealthyReport.issues.contains("Data inconsistency detected"))
    }

    @Test
    fun `safe operation result success type works correctly`() {
        val successResult = SafeOperationResult.Success("test_result")
        
        assertTrue(successResult is SafeOperationResult.Success)
        assertEquals("test_result", successResult.result)
    }

    @Test
    fun `safe operation result failure type works correctly`() {
        val testException = RuntimeException("test_error")
        val failureResult = SafeOperationResult.Failure<String>(testException)
        
        assertTrue(failureResult is SafeOperationResult.Failure)
        assertEquals("test_error", failureResult.error.message)
        assertEquals(testException, failureResult.error)
    }

    @Test
    fun `database health status enum values exist and are accessible`() {
        val healthy = DatabaseHealthStatus.HEALTHY
        val inconsistent = DatabaseHealthStatus.INCONSISTENT
        val corrupted = DatabaseHealthStatus.CORRUPTED
        
        assertNotNull(healthy)
        assertNotNull(inconsistent)
        assertNotNull(corrupted)
        
        assertEquals("HEALTHY", healthy.name)
        assertEquals("INCONSISTENT", inconsistent.name)
        assertEquals("CORRUPTED", corrupted.name)
    }

    @Test
    fun `recovery result enum values exist and are accessible`() {
        val recovered = RecoveryResult.RECOVERED_FROM_BACKUP
        val noBackups = RecoveryResult.NO_BACKUPS_FOUND
        val failed = RecoveryResult.RECOVERY_FAILED
        
        assertNotNull(recovered)
        assertNotNull(noBackups)
        assertNotNull(failed)
        
        assertEquals("RECOVERED_FROM_BACKUP", recovered.name)
        assertEquals("NO_BACKUPS_FOUND", noBackups.name)
        assertEquals("RECOVERY_FAILED", failed.name)
    }

    @Test
    fun `database safety status data structure works correctly`() {
        val status = DatabaseSafetyStatus(
            hasBackups = true,
            backupCount = 3,
            databaseExists = true,
            totalDatabaseSize = 2048L
        )
        
        assertTrue(status.hasBackups)
        assertEquals(3, status.backupCount)
        assertTrue(status.databaseExists)
        assertEquals(2048L, status.totalDatabaseSize)
    }

    @Test
    fun `database safety status handles empty state correctly`() {
        val emptyStatus = DatabaseSafetyStatus(
            hasBackups = false,
            backupCount = 0,
            databaseExists = false,
            totalDatabaseSize = 0L
        )
        
        assertFalse(emptyStatus.hasBackups)
        assertEquals(0, emptyStatus.backupCount)
        assertFalse(emptyStatus.databaseExists)
        assertEquals(0L, emptyStatus.totalDatabaseSize)
    }

    @Test
    fun `integrity report with empty database is valid`() {
        val emptyDbReport = DatabaseIntegrityChecker.IntegrityReport(
            totalInvestments = 0,
            validInvestments = 0,
            latestInvestmentId = 0L,
            isHealthy = true,
            issues = emptyList()
        )
        
        assertTrue(emptyDbReport.isHealthy)
        assertEquals(0, emptyDbReport.totalInvestments)
        assertEquals(0, emptyDbReport.validInvestments)
        assertEquals(0L, emptyDbReport.latestInvestmentId)
    }

    @Test
    fun `safe operation result can handle different data types`() {
        // Test with String
        val stringResult = SafeOperationResult.Success("string_value")
        assertTrue(stringResult is SafeOperationResult.Success)
        assertEquals("string_value", stringResult.result)
        
        // Test with Int
        val intResult = SafeOperationResult.Success(42)
        assertTrue(intResult is SafeOperationResult.Success)
        assertEquals(42, intResult.result)
        
        // Test with Boolean
        val boolResult = SafeOperationResult.Success(true)
        assertTrue(boolResult is SafeOperationResult.Success)
        assertEquals(true, boolResult.result)
    }
}

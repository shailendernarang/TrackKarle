package com.example.wealthtracker.util

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators

object BiometricUtils {
    
    /**
     * Check if device lock (PIN/Pattern/Password/Biometric) is available on this device
     */
    fun isDeviceLockAvailable(context: Context): Boolean {
        val keyguardManager = context.getSystemService(KeyguardManager::class.java) ?: return false
        
        // First check if device has any security set up
        if (!keyguardManager.isDeviceSecure) {
            return false
        }
        
        // Check if biometric authentication is supported
        val biometricManager = BiometricManager.from(context)
        val authenticators = getCompatibleAuthenticators()
        
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> true // Device supports it but not set up
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false // No biometric hardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false // Hardware unavailable
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> true // Needs update but supported
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> false // Not supported
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> false // Unknown status
            else -> false
        }
    }
    
    /**
     * Check if device lock is currently set up and ready to use
     */
    fun isDeviceLockReady(context: Context): Boolean {
        val keyguardManager = context.getSystemService(KeyguardManager::class.java) ?: return false
        
        if (!keyguardManager.isDeviceSecure) {
            return false
        }
        
        val biometricManager = BiometricManager.from(context)
        val authenticators = getCompatibleAuthenticators()
        
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }
    
    /**
     * Get the appropriate authenticators for the current Android version
     */
    fun getCompatibleAuthenticators(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Authenticators.DEVICE_CREDENTIAL
        } else {
            Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
        }
    }
    
    /**
     * Get user-friendly status message for device lock availability
     */
    fun getDeviceLockStatusMessage(context: Context): String {
        val keyguardManager = context.getSystemService(KeyguardManager::class.java) ?: return "No screen lock set up"
        
        if (!keyguardManager.isDeviceSecure) {
            return "No screen lock set up"
        }
        
        val biometricManager = BiometricManager.from(context)
        val authenticators = getCompatibleAuthenticators()
        
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Device lock available"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Set up biometric authentication"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Biometric hardware not available"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware unavailable"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Biometric authentication not supported"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Unknown biometric status"
            else -> "Device lock not available"
        }
    }
}

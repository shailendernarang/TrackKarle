package com.example.wealthtracker.data

import android.content.Context
import android.content.SharedPreferences
import com.example.wealthtracker.data.local.InvestmentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Reminder(
    val id: String,
    val investmentId: Long,
    val investmentType: String,
    val investmentName: String,
    val amount: Double,
    val maturityDate: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    val snoozeCount: Int = 0,
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val lastShownAt: Long = 0
)

enum class ReminderStatus {
    ACTIVE,      // Show on dashboard
    SNOOZED,     // Remind later
    DISMISSED,   // Got it - moved to history
    ARCHIVED     // Permanently removed
}

class ReminderManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("reminders_prefs", Context.MODE_PRIVATE)
    
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()
    
    // Expose filtered StateFlows for UI observation
    val activeReminders: StateFlow<List<Reminder>> = MutableStateFlow(emptyList())
    val snoozedReminders: StateFlow<List<Reminder>> = MutableStateFlow(emptyList())
    val dismissedReminders: StateFlow<List<Reminder>> = MutableStateFlow(emptyList())
    
    init {
        loadReminders()
    }
    
    // Load reminders from SharedPreferences
    private fun loadReminders() {
        val jsonString = prefs.getString("reminders_json", null) ?: return
        
        try {
            val jsonArray = JSONArray(jsonString)
            val remindersList = mutableListOf<Reminder>()
            
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                remindersList.add(
                    Reminder(
                        id = json.getString("id"),
                        investmentId = json.getLong("investmentId"),
                        investmentType = json.getString("investmentType"),
                        investmentName = json.getString("investmentName"),
                        amount = json.getDouble("amount"),
                        maturityDate = json.getString("maturityDate"),
                        message = json.getString("message"),
                        createdAt = json.getLong("createdAt"),
                        snoozeCount = json.optInt("snoozeCount", 0),
                        status = ReminderStatus.valueOf(json.optString("status", "ACTIVE")),
                        lastShownAt = json.optLong("lastShownAt", 0)
                    )
                )
            }
            
            _reminders.value = remindersList
            updateFilteredFlows()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Save reminders to SharedPreferences
    private fun saveReminders() {
        val jsonArray = JSONArray()
        
        _reminders.value.forEach { reminder ->
            val json = JSONObject().apply {
                put("id", reminder.id)
                put("investmentId", reminder.investmentId)
                put("investmentType", reminder.investmentType)
                put("investmentName", reminder.investmentName)
                put("amount", reminder.amount)
                put("maturityDate", reminder.maturityDate)
                put("message", reminder.message)
                put("createdAt", reminder.createdAt)
                put("snoozeCount", reminder.snoozeCount)
                put("status", reminder.status.name)
                put("lastShownAt", reminder.lastShownAt)
            }
            jsonArray.put(json)
        }
        
        prefs.edit().putString("reminders_json", jsonArray.toString()).apply()
    }
    
    // Generate reminders from investments with maturity dates
    fun syncRemindersFromInvestments(investments: List<InvestmentEntity>) {
        val existingIds = _reminders.value.map { it.investmentId }.toSet()
        val newReminders = mutableListOf<Reminder>()
        
        investments.forEach { investment ->
            // Only create for investments not already having reminders
            if (investment.id !in existingIds) {
                val maturityDate = when (investment.investmentType) {
                    "FD" -> investment.fdMaturityDate
                    "Health Insurance" -> investment.hiRenewalDate
                    else -> null // Only FD and Health Insurance have maturity/renewal dates in current schema
                }
                
                if (!maturityDate.isNullOrBlank()) {
                    val daysUntil = calculateDaysUntilMaturity(maturityDate)
                    
                    // Create reminder if maturity is within 90 days
                    if (daysUntil in 0..90) {
                        val name = when (investment.investmentType) {
                            "FD" -> investment.bankName ?: "Fixed Deposit"
                            "PPF" -> "PPF Account"
                            "Health Insurance" -> investment.hiPolicyName ?: "Health Insurance"
                            else -> investment.investmentType
                        }
                        
                        val message = when {
                            daysUntil <= 7 -> "Maturing in $daysUntil days!"
                            daysUntil <= 30 -> "Maturing in ${daysUntil / 7} weeks"
                            else -> "Maturing in ${daysUntil / 30} months"
                        }
                        
                        newReminders.add(
                            Reminder(
                                id = UUID.randomUUID().toString(),
                                investmentId = investment.id,
                                investmentType = investment.investmentType,
                                investmentName = name,
                                amount = investment.amount,
                                maturityDate = maturityDate,
                                message = message
                            )
                        )
                    }
                }
            }
        }
        
        if (newReminders.isNotEmpty()) {
            _reminders.value = _reminders.value + newReminders
            saveReminders()
            updateFilteredFlows()
        }
    }
    
    // Update filtered flows whenever reminders change
    private fun updateFilteredFlows() {
        (activeReminders as MutableStateFlow).value = _reminders.value
            .filter { it.status == ReminderStatus.ACTIVE }
            .sortedBy { calculateDaysUntilMaturity(it.maturityDate) }
        
        (snoozedReminders as MutableStateFlow).value = _reminders.value
            .filter { it.status == ReminderStatus.SNOOZED }
            .sortedBy { calculateDaysUntilMaturity(it.maturityDate) }
        
        (dismissedReminders as MutableStateFlow).value = _reminders.value
            .filter { it.status == ReminderStatus.DISMISSED }
            .sortedByDescending { it.lastShownAt }
    }
    
    // Get active reminders for dashboard (deprecated - use activeReminders flow)
    fun getActiveReminders(): List<Reminder> {
        return activeReminders.value
    }
    
    // Get snoozed reminders (deprecated - use snoozedReminders flow)
    fun getSnoozedReminders(): List<Reminder> {
        return snoozedReminders.value
    }
    
    // Get dismissed reminders (deprecated - use dismissedReminders flow)
    fun getDismissedReminders(): List<Reminder> {
        return dismissedReminders.value
    }
    
    // Mark reminder as "Got it" (dismiss)
    fun dismissReminder(reminderId: String) {
        _reminders.value = _reminders.value.map { reminder ->
            if (reminder.id == reminderId) {
                reminder.copy(
                    status = ReminderStatus.DISMISSED,
                    lastShownAt = System.currentTimeMillis()
                )
            } else {
                reminder
            }
        }
        saveReminders()
        updateFilteredFlows()
    }
    
    // Snooze reminder (max 3 times)
    fun snoozeReminder(reminderId: String) {
        _reminders.value = _reminders.value.map { reminder ->
            if (reminder.id == reminderId && reminder.snoozeCount < 3) {
                reminder.copy(
                    status = ReminderStatus.SNOOZED,
                    snoozeCount = reminder.snoozeCount + 1,
                    lastShownAt = System.currentTimeMillis()
                )
            } else {
                reminder
            }
        }
        saveReminders()
        updateFilteredFlows()
    }
    
    // Permanently delete reminder
    fun archiveReminder(reminderId: String) {
        _reminders.value = _reminders.value.filterNot { it.id == reminderId }
        saveReminders()
        updateFilteredFlows()
    }
    
    // Reactivate a snoozed reminder
    fun reactivateReminder(reminderId: String) {
        _reminders.value = _reminders.value.map { reminder ->
            if (reminder.id == reminderId) {
                reminder.copy(status = ReminderStatus.ACTIVE)
            } else {
                reminder
            }
        }
        saveReminders()
        updateFilteredFlows()
    }
    
    // Check if it's time to show snoozed reminders again (after 24 hours)
    fun refreshSnoozedReminders() {
        val now = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000
        
        _reminders.value = _reminders.value.map { reminder ->
            if (reminder.status == ReminderStatus.SNOOZED && 
                now - reminder.lastShownAt > dayInMillis) {
                reminder.copy(status = ReminderStatus.ACTIVE)
            } else {
                reminder
            }
        }
        saveReminders()
        updateFilteredFlows()
    }
    
    private fun calculateDaysUntilMaturity(maturityDate: String): Int {
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val maturity = sdf.parse(maturityDate) ?: return Int.MAX_VALUE
            val today = Calendar.getInstance().time
            val diffInMillis = maturity.time - today.time
            (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            Int.MAX_VALUE
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ReminderManager? = null
        
        fun getInstance(context: Context): ReminderManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReminderManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

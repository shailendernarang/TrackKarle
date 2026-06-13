package com.example.wealthtracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.room.Room
import com.example.wealthtracker.MainActivity
import com.ss.wealthtracker.R
import com.example.wealthtracker.data.local.WealthTrackerDatabase
import com.example.wealthtracker.data.local.MIGRATION_1_2
import com.example.wealthtracker.data.local.MIGRATION_2_3
import com.example.wealthtracker.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PortfolioWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all widget instances
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Create RemoteViews for the widget layout
            val views = RemoteViews(context.packageName, R.layout.widget_portfolio)

            // Set click intent to open the app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            // Load data asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Create database instance directly
                    val database = Room.databaseBuilder(
                        context,
                        WealthTrackerDatabase::class.java,
                        "wealth_tracker.db"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
                    
                    val investments = database.investmentDao().getAllOnce()
                    
                    val totalAmount = investments.sumOf { it.amount }
                    val totalCount = investments.size
                    
                    // Group by investment type and get top 3
                    val investmentsByType = investments
                        .groupBy { it.investmentType }
                        .map { (type, invList) -> 
                            type to invList.sumOf { it.amount }
                        }
                        .sortedByDescending { it.second }
                    
                    val top3Types = investmentsByType.take(3)
                    val maxAmount = top3Types.firstOrNull()?.second ?: 1.0

                    withContext(Dispatchers.Main) {
                        // Update basic info
                        views.setTextViewText(R.id.widget_total_amount, FormatUtils.formatINRShort(totalAmount))
                        views.setTextViewText(R.id.widget_total_count, "$totalCount Investments")
                        views.setTextViewText(R.id.widget_last_updated, getCurrentTime())
                        
                        // Update top 3 investment types with mini bars
                        updateMiniChart(views, top3Types, maxAmount)
                        
                        // Update bottom info
                        val diversificationText = when {
                            investmentsByType.size >= 5 -> "Well diversified portfolio"
                            investmentsByType.size >= 3 -> "Moderately diversified"
                            investmentsByType.size >= 2 -> "Consider diversifying"
                            else -> "Add more investment types"
                        }
                        views.setTextViewText(R.id.widget_bottom_info, diversificationText)
                        
                        // Update the widget
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    
                    database.close()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // Show error state
                        views.setTextViewText(R.id.widget_total_amount, "₹0")
                        views.setTextViewText(R.id.widget_total_count, "0 Investments")
                        views.setTextViewText(R.id.widget_bottom_info, "Tap to open app")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PortfolioWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
        
        private fun getCurrentTime(): String {
            val now = java.util.Calendar.getInstance()
            val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = now.get(java.util.Calendar.MINUTE)
            return String.format("%02d:%02d", hour, minute)
        }
        
        private fun updateMiniChart(views: RemoteViews, top3Types: List<Pair<String, Double>>, maxAmount: Double) {
            val colors = listOf(
                android.graphics.Color.parseColor("#2196F3"), // Blue
                android.graphics.Color.parseColor("#4CAF50"), // Green  
                android.graphics.Color.parseColor("#FF9800")  // Orange
            )
            
            // Update each bar
            for (i in 0..2) {
                val nameId = when (i) {
                    0 -> R.id.widget_type1_name
                    1 -> R.id.widget_type2_name
                    else -> R.id.widget_type3_name
                }
                val barId = when (i) {
                    0 -> R.id.widget_type1_bar
                    1 -> R.id.widget_type2_bar
                    else -> R.id.widget_type3_bar
                }
                val amountId = when (i) {
                    0 -> R.id.widget_type1_amount
                    1 -> R.id.widget_type2_amount
                    else -> R.id.widget_type3_amount
                }
                
                if (i < top3Types.size) {
                    val (type, amount) = top3Types[i]
                    val shortType = getShortTypeName(type)
                    
                    views.setTextViewText(nameId, shortType)
                    views.setTextViewText(amountId, FormatUtils.formatINRShort(amount))
                    
                    // Set progress based on relative amount (0..100)
                    val progress = ((amount / maxAmount) * 100.0).toInt().coerceIn(1, 100)
                    views.setProgressBar(barId, 100, progress, false)
                } else {
                    // Hide unused bars
                    views.setTextViewText(nameId, "")
                    views.setTextViewText(amountId, "")
                    views.setProgressBar(barId, 100, 0, false)
                }
            }
        }
        
        private fun getShortTypeName(type: String): String {
            return when (type.lowercase()) {
                "fixed deposit" -> "FD"
                "mutual fund" -> "MF"
                "public provident fund" -> "PPF"
                "national pension system" -> "NPS"
                "health insurance" -> "HI"
                "stocks" -> "Stock"
                "gold" -> "Gold"
                "others" -> "Other"
                else -> type.take(5)
            }
        }
    }
}

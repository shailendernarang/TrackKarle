package com.example.wealthtracker.ui.charts

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

object ChartUtils {
    
    @Composable
    fun getDarkModeAwareColors(): List<Int> {
        val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
        return if (isDarkTheme) {
            // Dark mode colors - more vibrant and visible on dark backgrounds
            listOf(
                Color.parseColor("#BB86FC"), // Purple
                Color.parseColor("#03DAC6"), // Teal
                Color.parseColor("#CF6679"), // Pink
                Color.parseColor("#FFB74D"), // Orange
                Color.parseColor("#81C784"), // Green
                Color.parseColor("#64B5F6"), // Blue
                Color.parseColor("#F06292"), // Pink
                Color.parseColor("#A1C181")  // Light Green
            )
        } else {
            // Light mode colors - standard Material colors
            listOf(
                Color.parseColor("#2196F3"), // Blue
                Color.parseColor("#4CAF50"), // Green
                Color.parseColor("#FF9800"), // Orange
                Color.parseColor("#9C27B0"), // Purple
                Color.parseColor("#F44336"), // Red
                Color.parseColor("#00BCD4"), // Cyan
                Color.parseColor("#795548"), // Brown
                Color.parseColor("#607D8B")  // Blue Grey
            )
        }
    }
    
    @Composable
    fun getTextColor(): Int {
        return MaterialTheme.colorScheme.onSurface.toArgb()
    }
    
    @Composable
    fun getBackgroundColor(): Int {
        return MaterialTheme.colorScheme.surface.toArgb()
    }

    // bindPieData has been removed - use SafePieChart from ComposePieChart.kt instead
    // This prevents any accidental usage of the old MPAndroidChart pie chart

    fun createBarChart(context: Context, onSurface: Int): BarChart =
        BarChart(context).apply {
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(true)
            xAxis.textColor = onSurface
            axisLeft.textColor = onSurface
            xAxis.granularity = 1f
            xAxis.labelRotationAngle = -20f
            setFitBars(true)
            setTouchEnabled(false)
            setExtraOffsets(12f, 12f, 12f, 12f)
        }

    fun bindBarData(
        chart: BarChart,
        labels: List<String>,
        values: List<Float>,
        colors: List<Int>,
        valueTextColor: Int,
        animate: Boolean
    ) {
        // Safety checks to prevent crashes
        if (labels.isEmpty() || values.isEmpty() || colors.isEmpty()) {
            chart.clear()
            return
        }
        
        try {
            // Filter out invalid values
            val validValues = values.filter { it.isFinite() && it >= 0 }
            if (validValues.isEmpty()) {
                chart.clear()
                return
            }
            
            val entries = validValues.mapIndexed { idx, v -> 
                BarEntry(idx.toFloat(), v.coerceAtLeast(0f)) 
            }
            
            val set = BarDataSet(entries, null).apply { 
                this.colors = colors.take(validValues.size)
            }
            
            chart.data = BarData(set).apply {
                setDrawValues(true)
                setValueTextSize(9f)
                setValueTextColor(valueTextColor)
                barWidth = 0.6f
            }
            
            chart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                    val i = value.toInt()
                    return if (i in labels.indices) labels[i] else ""
                }
            }
            
            if (animate) chart.animateY(700, Easing.EaseInOutQuad)
            chart.invalidate()
        } catch (e: Exception) {
            chart.clear()
            android.util.Log.e("ChartUtils", "Error binding bar data", e)
        }
    }
}

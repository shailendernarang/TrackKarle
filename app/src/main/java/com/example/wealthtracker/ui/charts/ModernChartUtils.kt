package com.example.wealthtracker.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
// Simplified chart implementation using Compose Canvas
// Vico integration can be added later when dependencies are properly configured
import androidx.compose.ui.graphics.Brush

object ModernChartUtils {
    
    @Composable
    fun getDarkModeAwareColors(): List<Color> {
        val isDarkTheme = isSystemInDarkTheme()
        return if (isDarkTheme) {
            // Dark mode colors - more vibrant and visible on dark backgrounds
            listOf(
                Color(0xFFBB86FC), // Purple
                Color(0xFF03DAC6), // Teal
                Color(0xFFCF6679), // Pink
                Color(0xFFFFB74D), // Orange
                Color(0xFF81C784), // Green
                Color(0xFF64B5F6), // Blue
                Color(0xFFF06292), // Pink
                Color(0xFFA1C181)  // Light Green
            )
        } else {
            // Light mode colors - standard Material colors
            listOf(
                Color(0xFF2196F3), // Blue
                Color(0xFF4CAF50), // Green
                Color(0xFFFF9800), // Orange
                Color(0xFF9C27B0), // Purple
                Color(0xFFF44336), // Red
                Color(0xFF00BCD4), // Cyan
                Color(0xFF795548), // Brown
                Color(0xFF607D8B)  // Blue Grey
            )
        }
    }
    
    // EnhancedPieChart has been removed - use SafePieChart from ComposePieChart.kt instead
    // This prevents any accidental usage of the old MPAndroidChart pie chart implementation
    
    @Composable
    fun EnhancedColumnChart(
        labels: List<String>,
        values: List<Float>,
        modifier: Modifier = Modifier,
        showGrid: Boolean = true,
        animationEnabled: Boolean = true
    ) {
        val colors = getDarkModeAwareColors()
        
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { context ->
                com.github.mikephil.charting.charts.BarChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    axisRight.isEnabled = false
                    xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                    xAxis.setDrawGridLines(showGrid)
                    axisLeft.setDrawGridLines(showGrid)
                    setFitBars(true)
                    setTouchEnabled(false)
                    setExtraOffsets(12f, 12f, 12f, 12f)
                }
            },
            update = { chart ->
                val entries = values.mapIndexed { index, value ->
                    com.github.mikephil.charting.data.BarEntry(index.toFloat(), value)
                }
                val dataSet = com.github.mikephil.charting.data.BarDataSet(entries, null).apply {
                    this.colors = colors.map { it.toArgb() }
                }
                chart.data = com.github.mikephil.charting.data.BarData(dataSet).apply {
                    setDrawValues(true)
                    setValueTextSize(9f)
                    barWidth = 0.6f
                }
                chart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                        val i = value.toInt()
                        return if (i in labels.indices) labels[i] else ""
                    }
                }
                if (animationEnabled) chart.animateY(700)
                chart.invalidate()
            },
            modifier = modifier.height(200.dp)
        )
    }
    
    @Composable
    fun EnhancedLineChart(
        data: List<Pair<String, Float>>,
        modifier: Modifier = Modifier,
        showPoints: Boolean = true,
        showArea: Boolean = false
    ) {
        val colors = getDarkModeAwareColors()
        
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { context ->
                com.github.mikephil.charting.charts.LineChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    axisRight.isEnabled = false
                    xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                    xAxis.setDrawGridLines(true)
                    axisLeft.setDrawGridLines(true)
                    setTouchEnabled(true)
                    setExtraOffsets(12f, 12f, 12f, 12f)
                }
            },
            update = { chart ->
                val entries = data.mapIndexed { index, (_, value) ->
                    com.github.mikephil.charting.data.Entry(index.toFloat(), value)
                }
                val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, null).apply {
                    color = colors[0].toArgb()
                    lineWidth = 3f
                    setDrawCircles(showPoints)
                    setDrawFilled(showArea)
                    if (showArea) {
                        fillColor = colors[0].toArgb()
                        fillAlpha = 100
                    }
                }
                chart.data = com.github.mikephil.charting.data.LineData(dataSet).apply {
                    setDrawValues(false)
                }
                chart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                        val i = value.toInt()
                        return if (i in data.indices) data[i].first else ""
                    }
                }
                chart.animateX(700)
                chart.invalidate()
            },
            modifier = modifier.height(200.dp)
        )
    }
    
    @Composable
    private fun CustomDonutChart(
        data: List<Pair<String, Double>>,
        colors: List<Color>,
        modifier: Modifier = Modifier
    ) {
        // Simple placeholder - not needed since we use the main pie chart
        Box(modifier = modifier.size(200.dp)) {
            androidx.compose.material3.Text(
                text = "Chart",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
    
    @Composable
    private fun PieChartLegend(
        data: List<Pair<String, Double>>,
        colors: List<Color>,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            data.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(colors[index % colors.size])
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        androidx.compose.material3.Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        androidx.compose.material3.Text(
                            text = com.example.wealthtracker.util.FormatUtils.formatINRShort(value),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

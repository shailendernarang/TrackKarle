package com.example.wealthtracker.ui.charts

import android.content.Context
import android.graphics.Color
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

object ChartUtils {
    fun createPieChart(context: Context, onSurface: Int): PieChart =
        PieChart(context).apply {
            description.isEnabled = false
            legend.isEnabled = false
            setUsePercentValues(false)
            setDrawEntryLabels(false)
            setHoleColor(Color.TRANSPARENT)
            setTouchEnabled(true)
            setExtraOffsets(8f, 8f, 8f, 8f)
            setCenterTextColor(onSurface)
            setCenterTextSize(14f)
            setHighlightPerTapEnabled(true)
        }

    fun bindPieData(
        chart: PieChart,
        data: List<Pair<String, Double>>,
        colors: List<Int>,
        animate: Boolean
    ) {
        if (chart.data == null) {
            val entries = data.map { (label, value) -> PieEntry(value.toFloat(), label) }
            val set = PieDataSet(entries, null).apply {
                this.colors = colors
                sliceSpace = 2f
                selectionShift = 6f
                yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
                xValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
            }
            chart.data = PieData(set).apply { setDrawValues(false) }
            if (animate) chart.animateY(700, Easing.EaseInOutQuad)
            chart.invalidate()
        } else {
            val set = chart.data.getDataSetByIndex(0) as PieDataSet
            val newEntries = data.map { (label, value) -> PieEntry(value.toFloat(), label) }
            set.values = newEntries
            set.colors = data.mapIndexed { idx, _ -> colors[idx % colors.size] }
            chart.data.setDrawValues(false)
            set.yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
            set.xValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    }

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
        val entries = values.mapIndexed { idx, v -> BarEntry(idx.toFloat(), v) }
        val set = BarDataSet(entries, null).apply { this.colors = colors }
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
    }
}

package com.example.wealthtracker.ui.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wealthtracker.util.FormatUtils
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SafePieChart(
    data: List<Pair<String, Double>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true,
    animationEnabled: Boolean = true,
    centerText: String? = null,
    onSliceClick: ((String, Double) -> Unit)? = null
) {
    // Safety checks
    if (data.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No data to display",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Filter and validate data
    val validData = data.filter { (label, value) ->
        !label.isBlank() && value > 0 && value.isFinite()
    }

    if (validData.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No valid data to display",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Calculate percentages
    val total = validData.sumOf { it.second }
    val dataWithPercentages = validData.mapIndexed { index, (label, value) ->
        val percentage = (value / total * 100).toFloat()
        val color = colors.getOrElse(index) { colors[index % colors.size] }
        PieSlice(label, value, percentage, color)
    }

    Column(modifier = modifier) {
        // Pie Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            AnimatedPieChart(
                slices = dataWithPercentages,
                animationEnabled = animationEnabled,
                centerText = centerText,
                onSliceClick = onSliceClick
            )
        }

        // Legend below chart
        if (showLegend) {
            Spacer(modifier = Modifier.height(8.dp))
            PieChartLegend(
                slices = dataWithPercentages,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AnimatedPieChart(
    slices: List<PieSlice>,
    animationEnabled: Boolean,
    centerText: String?,
    onSliceClick: ((String, Double) -> Unit)? = null
) {
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(slices) {
        if (animationEnabled) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, animationSpec = tween(1000))
        } else {
            animationProgress.snapTo(1f)
        }
    }

    var selectedSlice by remember { mutableStateOf<PieSlice?>(null) }
    
    // Only add touch handling if callback is provided
    val canvasModifier = if (onSliceClick != null) {
        Modifier
            .fillMaxSize()
            .pointerInput(slices) {
                detectTapGestures { offset ->
                    val center = Offset(size.width.toFloat() / 2f, size.height.toFloat() / 2f)
                    val radius = (kotlin.math.min(size.width.toFloat(), size.height.toFloat()) / 2f) * 0.8f
                    
                    // Check if tap is within the pie chart circle
                    val distance = kotlin.math.sqrt(
                        (offset.x - center.x) * (offset.x - center.x) + 
                        (offset.y - center.y) * (offset.y - center.y)
                    ).toFloat()
                    
                    if (distance <= radius) {
                        // Calculate angle from center to tap point
                        val angle = kotlin.math.atan2(
                            offset.y - center.y, 
                            offset.x - center.x
                        ) * 180 / kotlin.math.PI
                        
                        // Normalize angle to 0-360 and adjust for starting at -90 degrees
                        val normalizedAngle = ((angle + 90 + 360) % 360).toFloat()
                        
                        // Find which slice was tapped
                        var currentAngle = 0f
                        for (slice in slices) {
                            val sliceAngle = slice.percentage / 100f * 360f
                            if (normalizedAngle >= currentAngle && normalizedAngle < currentAngle + sliceAngle) {
                                selectedSlice = slice
                                onSliceClick.invoke(slice.label, slice.value)
                                break
                            }
                            currentAngle += sliceAngle
                        }
                    }
                }
            }
    } else {
        Modifier.fillMaxSize()
    }
    
    Canvas(modifier = canvasModifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (kotlin.math.min(size.width, size.height) / 2f) * 0.8f
        val strokeWidth = radius * 0.3f
        val innerRadius = radius - strokeWidth

        var startAngle = -90f
        val progress = animationProgress.value

        slices.forEach { slice ->
            val sweepAngle = (slice.percentage / 100f * 360f) * progress
            
            // Highlight selected slice by making it slightly larger
            val isSelected = selectedSlice == slice
            val currentRadius = if (isSelected) radius * 1.05f else radius
            val currentStrokeWidth = if (isSelected) strokeWidth * 1.1f else strokeWidth
            
            // Draw pie slice
            drawArc(
                color = if (isSelected) slice.color.copy(alpha = 0.9f) else slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - currentRadius,
                    center.y - currentRadius
                ),
                size = Size(currentRadius * 2, currentRadius * 2),
                style = Stroke(width = currentStrokeWidth)
            )

            startAngle += slice.percentage / 100f * 360f
        }

        // Draw center circle (hole)
        drawCircle(
            color = Color.Transparent,
            radius = innerRadius,
            center = center
        )
    }

    // Center text
    if (centerText != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PieChartLegend(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier
) {
    // Use horizontal scroll for compact legend layout
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        slices.forEach { slice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(slice.color)
                )
                
                // Label and percentage
                Column {
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = "${slice.percentage.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class PieSlice(
    val label: String,
    val value: Double,
    val percentage: Float,
    val color: Color
)

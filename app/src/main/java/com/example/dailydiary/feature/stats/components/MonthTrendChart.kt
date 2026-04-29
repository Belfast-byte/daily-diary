package com.example.dailydiary.feature.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.feature.stats.DayMood

@Composable
fun MonthTrendChart(
    trend: List<DayMood>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "近30天心情趋势",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (trend.isEmpty()) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val maxScore = 3f
                val minScore = -2f
                val range = maxScore - minScore

                val points = trend.mapIndexedNotNull { index, dayMood ->
                    if (dayMood.mood != null) {
                        val x = (index.toFloat() / (trend.size - 1).coerceAtLeast(1)) * chartWidth
                        val y = (1 - (dayMood.mood.sentimentScore - minScore) / range) * chartHeight
                        Offset(x, y) to dayMood.mood.color
                    } else null
                }

                // Connecting lines
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = points[i].second.copy(alpha = 0.3f),
                        start = points[i].first,
                        end = points[i + 1].first,
                        strokeWidth = 2f
                    )
                }

                // Dots
                points.forEach { (offset, color) ->
                    drawCircle(color = color, radius = 5f, center = offset)
                }
            }

            // Legend row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Mood.entries.filter { it.sentimentScore > 0 }.forEach { mood ->
                    Text(text = mood.label, style = MaterialTheme.typography.labelSmall, color = mood.color)
                }
                Mood.entries.filter { it.sentimentScore < 0 }.forEach { mood ->
                    Text(text = mood.label, style = MaterialTheme.typography.labelSmall, color = mood.color)
                }
            }
        }
    }
}

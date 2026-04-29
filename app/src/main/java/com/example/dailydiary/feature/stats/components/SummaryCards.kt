package com.example.dailydiary.feature.stats.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailydiary.domain.model.Mood

@Composable
fun SummaryCards(
    totalDays: Int,
    streakDays: Int,
    monthlyCount: Int,
    topMood: Mood?,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth()
    ) {
        item { StatCard("总记录", totalDays.toString(), "天") }
        item { StatCard("连续记录", streakDays.toString(), "天") }
        item { StatCard("本月记录", monthlyCount.toString(), "条") }
        item {
            topMood?.let { TopMoodCard(it) }
                ?: StatCard("最常心情", "-", "")
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String) {
    Card(
        modifier = Modifier.padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${label} · ${unit}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TopMoodCard(mood: Mood) {
    Card(
        modifier = Modifier.padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = mood.color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = mood.label.take(1),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = mood.color
            )
            Text(
                text = mood.label,
                style = MaterialTheme.typography.labelSmall,
                color = mood.color,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

package com.example.dailydiary.feature.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dailydiary.domain.model.ActivityTag

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSelector(
    tags: List<ActivityTag>,
    selectedTagIds: Set<Long>,
    onTagToggled: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Header bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
                        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "🏷 活动标签",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Tag chips
            if (tags.isEmpty()) {
                Text(
                    text = "暂无标签，请先在设置中添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        TagChip(
                            tag = tag,
                            isSelected = tag.id in selectedTagIds,
                            onClick = { onTagToggled(tag.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: ActivityTag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipColor = try {
        Color(android.graphics.Color.parseColor(tag.color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val bgColor = if (isSelected) chipColor.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surface

    val borderColor = if (isSelected) chipColor.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(chipColor, RoundedCornerShape(4.dp))
            )
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

package com.example.dailydiary.domain.model

import androidx.compose.ui.graphics.Color

enum class Mood(
    val id: String,
    val label: String,
    val sentimentScore: Int,
    val color: Color
) {
    VERY_HAPPY("very_happy", "很开心", 3, Color(0xFFFFB347)),
    HAPPY("happy", "开心", 2, Color(0xFFFFD700)),
    CALM("calm", "平静", 1, Color(0xFF87CEEB)),
    LOW("low", "低落", -1, Color(0xFFB0C4DE)),
    SAD("sad", "难过", -2, Color(0xFF6A5ACD)),
    ANXIOUS("anxious", "焦虑", -2, Color(0xFFDDA0DD)),
    ANGRY("angry", "生气", -2, Color(0xFFE74C3C));

    companion object {
        fun fromId(id: String): Mood = entries.first { it.id == id }
    }
}

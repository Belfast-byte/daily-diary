package com.example.dailydiary.core.database

import com.example.dailydiary.domain.model.ActivityTag

object SeedDefaults {
    val defaultTags = listOf(
        ActivityTag(name = "工作", color = "#FF6B6B", sortOrder = 0),
        ActivityTag(name = "学习", color = "#4ECDC4", sortOrder = 1),
        ActivityTag(name = "运动", color = "#45B7D1", sortOrder = 2),
        ActivityTag(name = "家人", color = "#96CEB4", sortOrder = 3),
        ActivityTag(name = "朋友", color = "#FFEAA7", sortOrder = 4),
        ActivityTag(name = "睡眠", color = "#DDA0DD", sortOrder = 5),
        ActivityTag(name = "饮食", color = "#FF8C69", sortOrder = 6),
        ActivityTag(name = "旅行", color = "#87CEEB", sortOrder = 7)
    )
}

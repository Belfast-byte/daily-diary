# 安卓日记 App

这是一个本地优先的 Android 日记 App 规划文档集。产品目标是让用户每天快速记录心情和文字日记，并能安全地回看、搜索、统计和导出自己的数据。

## MVP 范围

- 今日记录：选择心情、写日记、选择活动标签。
- 日历回看：按日期查看是否记录和当天心情。
- 历史列表：按时间倒序查看日记，支持关键词搜索。
- 心情统计：展示近 7 天和近 30 天趋势。
- 隐私保护：启动后使用生物识别或设备凭据解锁。
- 每日提醒：用户可设置固定提醒时间。
- 本地导出：导出 JSON 和 CSV。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Room
- SQLCipher for Android
- DataStore Preferences
- Hilt
- AndroidX Biometric
- WorkManager
- JUnit 与 Compose UI Test

## 文档索引

- [产品规格](docs/00-product-spec.md)
- [技术架构](docs/01-architecture.md)
- [数据模型](docs/02-data-model.md)
- [安全与隐私](docs/03-security-privacy.md)
- [分步开发计划](docs/04-development-plan.md)
- [测试与发布](docs/05-testing-release.md)

## 核心原则

- 本地优先：核心功能离线可用。
- 隐私优先：日记内容默认加密保存。
- 快速记录：用户可以在 10 秒内完成一次心情记录。
- 故障显式：关键失败必须暴露错误，不做静默降级。

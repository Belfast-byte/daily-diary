# 技术架构

## 技术选型

| 领域 | 选型 | 说明 |
|---|---|---|
| 语言 | Kotlin | Android 原生开发主流选择。 |
| UI | Jetpack Compose | 声明式 UI，适合快速迭代表单、日历和统计页面。 |
| 设计系统 | Material 3 | 使用官方组件和主题体系。 |
| 本地数据库 | Room | 结构化存储日记、心情、标签、附件元数据。 |
| 数据库加密 | SQLCipher for Android | 加密本地日记数据库。 |
| 设置存储 | DataStore Preferences | 存储提醒时间、隐私锁开关、主题等轻量配置。 |
| 依赖注入 | Hilt | 管理 Repository、UseCase、Database 等依赖。 |
| 解锁 | AndroidX Biometric | 支持生物识别和设备凭据。 |
| 后台任务 | WorkManager | 管理每日提醒和导出任务。 |
| 测试 | JUnit、Room test、Compose UI test | 覆盖业务逻辑、数据库迁移和关键页面。 |

官方参考：

- Compose Material 3: https://developer.android.com/jetpack/androidx/releases/compose-material3
- Room: https://developer.android.com/room
- DataStore: https://developer.android.com/datastore
- Hilt: https://developer.android.com/training/dependency-injection/hilt-android
- Biometric: https://developer.android.com/jetpack/androidx/releases/biometric
- WorkManager: https://developer.android.com/develop/background-work/background-tasks/persistent
- SQLCipher for Android: https://www.zetetic.net/sqlcipher/sqlcipher-for-android

## 架构风格

采用单 Activity、Compose Navigation、MVVM 和单向数据流。

```text
UI Screen
  -> ViewModel
  -> UseCase
  -> Repository
  -> Room DAO / DataStore / System Service
```

UI 只负责渲染状态和提交用户事件。ViewModel 不直接访问数据库。Repository 不依赖 UI 类型。UseCase 承载跨 Repository 的业务规则。

## 模块建议

初期可以使用单模块 `app`，但包结构按模块化边界组织，避免后续拆分困难。

```text
app/
  core/
    database/
    datastore/
    security/
    notification/
    export/
    design/
  feature/
    today/
    calendar/
    history/
    stats/
    settings/
  domain/
    model/
    repository/
    usecase/
```

当代码增长后再拆为 `:core:*`、`:domain`、`:feature:*`，不在 MVP 初期提前复杂化。

## 页面结构

- `TodayScreen`：今日记录入口。
- `CalendarScreen`：月历和日期详情入口。
- `HistoryScreen`：时间线和搜索。
- `StatsScreen`：心情趋势和连续记录。
- `SettingsScreen`：隐私锁、提醒、导出。
- `UnlockScreen`：启动解锁。

底部导航包含今日、日历、历史、统计、设置。

## 状态管理

每个页面暴露一个不可变 `UiState`。

```kotlin
data class TodayUiState(
    val selectedMood: Mood?,
    val content: String,
    val tags: List<ActivityTag>,
    val isSaving: Boolean,
    val error: String?
)
```

页面事件通过显式方法提交，例如 `selectMood`、`updateContent`、`saveEntry`。失败状态必须进入 `error` 并展示。

## 错误处理原则

- 数据库打开失败：阻止进入主界面并显示错误。
- 解锁失败：保持锁定状态并显示失败原因。
- 保存失败：保留用户输入并显示错误。
- 导出失败：显示错误和失败路径。
- 提醒调度失败：显示设置未保存。

不做静默重试，不返回假成功。

# 数据模型

## 实体概览

```text
DiaryEntry
Mood
ActivityTag
DiaryEntryTagCrossRef
Attachment
AppSettings
```

`AppSettings` 存储在 DataStore，其余结构化数据存储在 Room 加密数据库。

## DiaryEntry

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键。 |
| entryDate | LocalDate | 日记日期，每天唯一。 |
| moodId | String | 心情枚举 ID。 |
| content | String | 日记正文，可为空。 |
| createdAt | Instant | 创建时间。 |
| updatedAt | Instant | 更新时间。 |

约束：

- `entryDate` 唯一。
- `moodId` 必填。
- `content` 可以为空字符串。

## Mood

MVP 中心情用代码枚举，不建数据库表。

| id | 名称 | 语义分 |
|---|---|---|
| very_happy | 很开心 | 3 |
| happy | 开心 | 2 |
| calm | 平静 | 1 |
| low | 低落 | -1 |
| sad | 难过 | -2 |
| anxious | 焦虑 | -2 |
| angry | 生气 | -2 |

语义分只用于趋势统计，不展示为用户可见分数。

## ActivityTag

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键。 |
| name | String | 标签名称。 |
| color | String | 十六进制颜色。 |
| sortOrder | Int | 展示顺序。 |
| isArchived | Boolean | 是否归档。 |

默认标签：

- 工作
- 学习
- 运动
- 家人
- 朋友
- 睡眠
- 饮食
- 旅行

## DiaryEntryTagCrossRef

| 字段 | 类型 | 说明 |
|---|---|---|
| entryId | Long | 日记 ID。 |
| tagId | Long | 标签 ID。 |

组合主键：`entryId + tagId`。

## Attachment

MVP 只保留结构，暂不开放 UI。

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键。 |
| entryId | Long | 日记 ID。 |
| type | String | photo、audio 等。 |
| localPath | String | App 私有目录路径。 |
| createdAt | Instant | 创建时间。 |

## AppSettings

存储在 DataStore。

| Key | 类型 | 默认值 |
|---|---|---|
| privacy_lock_enabled | Boolean | true |
| reminder_enabled | Boolean | false |
| reminder_time | String | 21:00 |
| theme_mode | String | system |
| first_launch_done | Boolean | false |

## 主要查询

- 按日期读取单条日记。
- 保存或更新当天日记。
- 查询月范围内的日记摘要。
- 按关键词搜索正文。
- 按心情筛选。
- 统计日期范围内的心情分布。
- 统计连续记录天数。

## 迁移策略

- 所有 Room schema 导出到版本目录。
- 每次变更实体必须增加迁移测试。
- 破坏性迁移只允许在开发阶段使用，发布后禁止。

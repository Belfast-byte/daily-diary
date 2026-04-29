# Stage 4: Statistics Screen Design

## Goal

Build StatsScreen showing mood trends: 7-day distribution chart, 30-day trend line, total recording days, consecutive days streak, monthly count, and top mood. Uses Compose Canvas for all charts — no external charting library.

## Architecture

StatsViewModel (@HiltViewModel) queries DiaryRepository for date ranges, computes streak from recorded dates, and exposes StatsUiState via StateFlow. Charts are stateless Canvas composables receiving data as parameters.

**Tech Stack:** Kotlin, Jetpack Compose, Compose Canvas, Hilt ViewModel

---

## Component Tree

```
StatsScreen
├── SummaryCards (2×2 grid)
│   ├── StatCard("总记录", totalDays)
│   ├── StatCard("连续天数", streakDays)
│   ├── StatCard("本月记录", monthlyCount)
│   └── StatCard("最常心情", topMood emoji+label)
├── WeekDistributionChart (horizontal bars, Canvas)
│   └── MoodBar × N (mood color bar + label + count)
└── MonthTrendChart (line chart, Canvas)
    └── Day dots with connecting lines, mood-colored
```

## Data Flow

```
StatsUiState:
  totalDays: Int, streakDays: Int
  monthlyCount: Int, topMood: Mood?
  weekDistribution: List<MoodCount>     // from getMoodDistribution
  monthTrend: List<DayMood>             // each day's mood from getEntriesInRange
  isLoading: Boolean, errorMessage: String?

Init: viewModelScope.launch { loadStats() }
  → getRecordedDatesInRange(all time) → compute total+streak
  → getEntriesInRange(monthStart, monthEnd) → monthlyCount
  → getMoodDistribution(7daysAgo, today) → weekDistribution + topMood
  → getEntriesInRange(30daysAgo, today) → monthTrend
```

## UI Design

### Summary Cards
- 2×2 Compose LazyVerticalGrid
- Each card: rounded surface, large number, small label below
- Top mood card shows mood emoji + label in mood color

### Week Distribution Chart
- Compose Canvas with horizontal bars
- Each bar: mood color fill, width proportional to count
- Right side: mood label + count number
- Bar height ~28dp, 8dp gap

### Month Trend Chart
- Compose Canvas with dot+line rendering
- X axis: 30 day positions, Y axis: mood sentiment score (-2 to +3)
- Each day with entry: colored dot at (x, y=moodScore), connected by lines
- Days without entry: no dot, line breaks
- Simple axis labels: 5-day intervals on X, mood names on Y

## States

| State | Behavior |
|-------|----------|
| < 3 total entries | Show guidance text "记录更多天后查看统计趋势" |
| Loading | CircularProgressIndicator |
| Error | Retry button + error message |
| Normal | Full charts + cards |

## File Structure

```
feature/stats/
├── StatsScreen.kt              (MODIFY — replace placeholder)
├── StatsViewModel.kt           (NEW)
├── StatsUiState.kt             (NEW)
└── components/
    ├── SummaryCards.kt          (NEW)
    ├── WeekDistributionChart.kt (NEW)
    └── MonthTrendChart.kt       (NEW)
```

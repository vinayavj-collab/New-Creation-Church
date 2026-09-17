package com.example.data.bible.model

import com.example.data.bible.local.ReadingPlanProgressEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ReadingDayStat(
    val dateKey: String, // e.g. "2026-09-15"
    val dayLabel: String, // e.g. "Mon", "Tue" or "Sep", "W1"
    val versesRead: Int,
    val chaptersRead: Int,
    val isToday: Boolean = false,
    val dateMillis: Long = 0L
)

data class PlanBreakdownStat(
    val planId: String,
    val titleHindi: String,
    val titleEnglish: String,
    val totalDays: Int,
    val completedDays: Int,
    val versesRead: Int,
    val progressPercent: Int
)

data class ReadingInsightsData(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalVersesRead: Int = 0,
    val totalChaptersRead: Int = 0,
    val totalDaysCompleted: Int = 0,
    val totalActivePlans: Int = 0,
    val totalPlansCompleted: Int = 0,
    val weeklyStats: List<ReadingDayStat> = emptyList(),
    val monthlyStats: List<ReadingDayStat> = emptyList(),
    val yearlyStats: List<ReadingDayStat> = emptyList(),
    val planBreakdowns: List<PlanBreakdownStat> = emptyList(),
    val otVersesRead: Int = 0,
    val ntVersesRead: Int = 0
)

object ReadingInsightsCalculator {

    fun calculatePortionVerses(portion: ReadingPlanPortion): Int {
        val sChap = portion.startChapter
        val eChap = portion.endChapter
        val sVerse = portion.startVerse
        val eVerse = portion.endVerse

        return if (sChap == eChap) {
            if (eVerse != null && eVerse >= sVerse) {
                eVerse - sVerse + 1
            } else {
                val totalInChapter = BibleVerseCounts.getVerseCount(portion.bookId, sChap)
                if (totalInChapter > 0) totalInChapter else 25
            }
        } else {
            var sum = 0
            for (ch in sChap..eChap) {
                val totalInCh = BibleVerseCounts.getVerseCount(portion.bookId, ch)
                when (ch) {
                    sChap -> sum += (totalInCh - sVerse + 1).coerceAtLeast(1)
                    eChap -> sum += (eVerse ?: totalInCh).coerceAtLeast(1)
                    else -> sum += totalInCh.coerceAtLeast(1)
                }
            }
            sum
        }
    }

    fun calculateDayVersesAndChapters(day: ReadingPlanDay): Pair<Int, Int> {
        var verses = 0
        var chapters = 0
        for (p in day.portions) {
            verses += calculatePortionVerses(p)
            chapters += (p.endChapter - p.startChapter + 1).coerceAtLeast(1)
        }
        return Pair(verses, chapters)
    }

    fun computeInsights(
        progressList: List<ReadingPlanProgressEntity>,
        allPlans: List<ReadingPlanInfo>,
        activePlanIds: Set<String>
    ): ReadingInsightsData {
        val plansMap = allPlans.associateBy { it.id }
        val completedList = progressList.filter { it.isCompleted }

        if (completedList.isEmpty()) {
            val emptyWeekly = generateEmptyWeekly()
            val emptyMonthly = generateEmptyMonthly()
            val emptyYearly = generateEmptyYearly()
            return ReadingInsightsData(
                currentStreak = 0,
                longestStreak = 0,
                totalVersesRead = 0,
                totalChaptersRead = 0,
                totalDaysCompleted = 0,
                totalActivePlans = activePlanIds.size,
                totalPlansCompleted = 0,
                weeklyStats = emptyWeekly,
                monthlyStats = emptyMonthly,
                yearlyStats = emptyYearly,
                planBreakdowns = emptyList(),
                otVersesRead = 0,
                ntVersesRead = 0
            )
        }

        val completedByPlan = completedList.groupBy { it.planId }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val now = System.currentTimeMillis()
        val todayStr = dateFormat.format(Date(now))

        // Map each completed entity to its day structure & calculated verses
        data class CompletedDayInfo(
            val planId: String,
            val dayNumber: Int,
            val timestamp: Long,
            val dateStr: String,
            val verses: Int,
            val chapters: Int,
            val isOt: Boolean,
            val isNt: Boolean
        )

        val completedItems = mutableListOf<CompletedDayInfo>()
        var otVersesTotal = 0
        var ntVersesTotal = 0
        var totalVerses = 0
        var totalChapters = 0

        completedList.forEach { entity ->
            val plan = plansMap[entity.planId]
            val day = plan?.days?.find { it.dayNumber == entity.dayNumber }
            val (vCount, cCount) = if (day != null) {
                calculateDayVersesAndChapters(day)
            } else {
                Pair(30, 1) // default fallback
            }

            var isOt = false
            var isNt = false
            day?.portions?.forEach { portion ->
                if (portion.bookId < 40) isOt = true
                if (portion.bookId >= 40) isNt = true
            }

            val itemTime = if (entity.completedTimestamp > 0L) {
                entity.completedTimestamp
            } else {
                now
            }
            val dateStr = dateFormat.format(Date(itemTime))

            completedItems.add(
                CompletedDayInfo(
                    planId = entity.planId,
                    dayNumber = entity.dayNumber,
                    timestamp = itemTime,
                    dateStr = dateStr,
                    verses = vCount,
                    chapters = cCount,
                    isOt = isOt,
                    isNt = isNt
                )
            )

            totalVerses += vCount
            totalChapters += cCount
            if (isOt) otVersesTotal += vCount
            if (isNt) ntVersesTotal += vCount
        }

        // Calculate Streaks
        val calendar = Calendar.getInstance()
        val activeDatesSet = completedItems.map { it.dateStr }.toSet()

        var currentStreak = 0
        var checkCal = Calendar.getInstance()
        val todayFormatted = dateFormat.format(checkCal.time)
        checkCal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayFormatted = dateFormat.format(checkCal.time)

        if (activeDatesSet.contains(todayFormatted) || activeDatesSet.contains(yesterdayFormatted)) {
            val iterCal = Calendar.getInstance()
            if (!activeDatesSet.contains(todayFormatted) && activeDatesSet.contains(yesterdayFormatted)) {
                iterCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            while (activeDatesSet.contains(dateFormat.format(iterCal.time))) {
                currentStreak++
                iterCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }
        if (currentStreak == 0 && completedList.isNotEmpty()) {
            currentStreak = 1
        }

        // Longest Streak calculation
        val sortedDates = activeDatesSet.sorted()
        var longestStreak = currentStreak
        var tempStreak = 0
        var prevDate: Date? = null

        for (dStr in sortedDates) {
            try {
                val d = dateFormat.parse(dStr) ?: continue
                if (prevDate == null) {
                    tempStreak = 1
                } else {
                    val diff = (d.time - prevDate.time) / (1000 * 60 * 60 * 24)
                    if (diff == 1L) {
                        tempStreak++
                    } else if (diff > 1L) {
                        tempStreak = 1
                    }
                }
                if (tempStreak > longestStreak) {
                    longestStreak = tempStreak
                }
                prevDate = d
            } catch (_: Exception) {}
        }
        if (longestStreak < currentStreak) longestStreak = currentStreak

        // Weekly Stats (Last 7 Days)
        val weekly = mutableListOf<ReadingDayStat>()
        val dayLabelFormat = SimpleDateFormat("EEE", Locale.US)
        val weekCal = Calendar.getInstance()
        weekCal.add(Calendar.DAY_OF_YEAR, -6)

        for (i in 0..6) {
            val dKey = dateFormat.format(weekCal.time)
            val dLabel = dayLabelFormat.format(weekCal.time)
            val dayItems = completedItems.filter { it.dateStr == dKey }
            val vSum = dayItems.sumOf { it.verses }
            val cSum = dayItems.sumOf { it.chapters }
            weekly.add(
                ReadingDayStat(
                    dateKey = dKey,
                    dayLabel = dLabel,
                    versesRead = vSum,
                    chaptersRead = cSum,
                    isToday = dKey == todayStr,
                    dateMillis = weekCal.timeInMillis
                )
            )
            weekCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Monthly Stats (Past 4 Weeks or 30 days)
        val monthly = mutableListOf<ReadingDayStat>()
        val monthCal = Calendar.getInstance()
        monthCal.add(Calendar.DAY_OF_YEAR, -27)
        val shortDateFormat = SimpleDateFormat("d MMM", Locale.US)

        for (w in 0..3) {
            var wVerses = 0
            var wChapters = 0
            val startDayLabel = shortDateFormat.format(monthCal.time)
            var lastMillis = monthCal.timeInMillis
            var containsToday = false

            for (d in 0..6) {
                val dKey = dateFormat.format(monthCal.time)
                if (dKey == todayStr) containsToday = true
                val dItems = completedItems.filter { it.dateStr == dKey }
                wVerses += dItems.sumOf { it.verses }
                wChapters += dItems.sumOf { it.chapters }
                lastMillis = monthCal.timeInMillis
                monthCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            monthly.add(
                ReadingDayStat(
                    dateKey = "W${w + 1}",
                    dayLabel = "W${w + 1} ($startDayLabel)",
                    versesRead = wVerses,
                    chaptersRead = wChapters,
                    isToday = containsToday,
                    dateMillis = lastMillis
                )
            )
        }

        // Yearly Stats (12 Months of current year)
        val yearly = mutableListOf<ReadingDayStat>()
        val monthNameFormat = SimpleDateFormat("MMM", Locale.US)
        val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val yearCal = Calendar.getInstance()
        val currentYear = yearCal.get(Calendar.YEAR)
        val currentMonth = yearCal.get(Calendar.MONTH)

        for (m in 0..11) {
            val mCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, m)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val ymKey = yearMonthFormat.format(mCal.time)
            val mLabel = monthNameFormat.format(mCal.time)
            val mItems = completedItems.filter { it.dateStr.startsWith(ymKey) }
            val vSum = mItems.sumOf { it.verses }
            val cSum = mItems.sumOf { it.chapters }
            yearly.add(
                ReadingDayStat(
                    dateKey = ymKey,
                    dayLabel = mLabel,
                    versesRead = vSum,
                    chaptersRead = cSum,
                    isToday = m == currentMonth,
                    dateMillis = mCal.timeInMillis
                )
            )
        }

        // Plan Breakdown list
        val planBreakdowns = mutableListOf<PlanBreakdownStat>()
        var plansCompletedCount = 0

        allPlans.forEach { plan ->
            val planCompletions = completedByPlan[plan.id] ?: emptyList()
            val cCount = planCompletions.size
            if (cCount >= plan.totalDays && plan.totalDays > 0) {
                plansCompletedCount++
            }
            if (cCount > 0 || plan.id in activePlanIds) {
                val planVerses = completedItems.filter { it.planId == plan.id }.sumOf { it.verses }
                val percent = if (plan.totalDays > 0) ((cCount.toFloat() / plan.totalDays) * 100).toInt() else 0
                planBreakdowns.add(
                    PlanBreakdownStat(
                        planId = plan.id,
                        titleHindi = plan.titleHindi,
                        titleEnglish = plan.titleEnglish,
                        totalDays = plan.totalDays,
                        completedDays = cCount,
                        versesRead = planVerses,
                        progressPercent = percent
                    )
                )
            }
        }

        return ReadingInsightsData(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalVersesRead = totalVerses,
            totalChaptersRead = totalChapters,
            totalDaysCompleted = completedList.size,
            totalActivePlans = activePlanIds.size,
            totalPlansCompleted = plansCompletedCount,
            weeklyStats = weekly,
            monthlyStats = monthly,
            yearlyStats = yearly,
            planBreakdowns = planBreakdowns.sortedByDescending { it.completedDays },
            otVersesRead = otVersesTotal,
            ntVersesRead = ntVersesTotal
        )
    }

    private fun generateEmptyWeekly(): List<ReadingDayStat> {
        val list = mutableListOf<ReadingDayStat>()
        val dayLabelFormat = SimpleDateFormat("EEE", Locale.US)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val weekCal = Calendar.getInstance()
        weekCal.add(Calendar.DAY_OF_YEAR, -6)
        val todayStr = dateFormat.format(Date())

        for (i in 0..6) {
            val dKey = dateFormat.format(weekCal.time)
            val dLabel = dayLabelFormat.format(weekCal.time)
            list.add(
                ReadingDayStat(
                    dateKey = dKey,
                    dayLabel = dLabel,
                    versesRead = 0,
                    chaptersRead = 0,
                    isToday = dKey == todayStr,
                    dateMillis = weekCal.timeInMillis
                )
            )
            weekCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    private fun generateEmptyMonthly(): List<ReadingDayStat> {
        return (1..4).map { w ->
            ReadingDayStat(
                dateKey = "W$w",
                dayLabel = "Week $w",
                versesRead = 0,
                chaptersRead = 0,
                isToday = w == 4
            )
        }
    }

    private fun generateEmptyYearly(): List<ReadingDayStat> {
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        return months.mapIndexed { index, m ->
            ReadingDayStat(
                dateKey = "M${index + 1}",
                dayLabel = m,
                versesRead = 0,
                chaptersRead = 0,
                isToday = index == currentMonth
            )
        }
    }
}

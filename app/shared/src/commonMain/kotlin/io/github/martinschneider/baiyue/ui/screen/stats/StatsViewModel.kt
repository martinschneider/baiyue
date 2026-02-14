package io.github.martinschneider.baiyue.ui.screen.stats

import io.github.martinschneider.baiyue.data.model.ClimbDate
import io.github.martinschneider.baiyue.data.model.ClimbProgress
import io.github.martinschneider.baiyue.data.model.Mountain
import io.github.martinschneider.baiyue.data.model.MountainType
import io.github.martinschneider.baiyue.data.repository.MountainRepository

data class HIndexData(val value: Int, val mountains: List<HIndexMountain>)

data class HIndexMountain(val mountain: Mountain, val count: Int, val countsTowardsHIndex: Boolean)

data class StatsData(
    val progress: ClimbProgress,
    val firstPeak: StatEntry?,
    val latestPeak: StatEntry?,
    val mostVisited: StatEntry?,
    val hIndex: HIndexData,
    val mostInADay: RecordStat?,
    val mostInAWeek: RecordStat?,
    val mostInAMonth: RecordStat?,
    val mostInAYear: RecordStat?,
    val progressTimeline: ProgressTimeline,
)

data class StatEntry(val mountain: Mountain, val detail: String)

data class RecordMountain(val mountain: Mountain, val date: String)

data class RecordStat(val period: String, val count: Int, val mountains: List<RecordMountain>)

/** A point on the progress-over-time chart. */
data class TimelinePoint(val date: String, val baiyue: Int, val xiaobaiyue: Int, val oldXiaobaiyue: Int)

data class ProgressTimeline(val points: List<TimelinePoint>)

class StatsViewModel(private val repository: MountainRepository) {

    val climbDates = repository.climbDates
    val climbedPeaks = repository.climbedPeaks

    fun computeStats(
        climbDates: Map<Long, List<ClimbDate>>,
        climbedPeaks: Map<Long, Boolean>
    ): StatsData {
        val progress = repository.getProgress()
        val allEntries = climbDates.values.flatten()

        val empty = StatsData(
            progress, null, null, null, HIndexData(0, emptyList()), null, null, null, null,
            ProgressTimeline(emptyList())
        )
        if (allEntries.isEmpty()) return empty

        // First peak (earliest date)
        val earliestEntry = allEntries.minByOrNull { it.date }
        val firstPeak = earliestEntry?.let {
            repository.getMountainByOsmId(it.osmId)?.let { m -> StatEntry(m, it.date) }
        }

        // Latest peak (most recent date)
        val latestEntry = allEntries.maxByOrNull { it.date }
        val latestPeak = latestEntry?.let {
            repository.getMountainByOsmId(it.osmId)?.let { m -> StatEntry(m, it.date) }
        }

        // Most visited: peak with most climb date entries
        val visitCounts = allEntries.groupBy { it.osmId }
        val mostVisitedEntry = visitCounts.maxByOrNull { it.value.size }
        val mostVisited = mostVisitedEntry?.let { (osmId, dates) ->
            repository.getMountainByOsmId(osmId)?.let { m -> StatEntry(m, "${dates.size}x") }
        }

        // H-index: max h such that at least h mountains climbed at least h times
        val hIndex = computeHIndex(visitCounts)

        // Most peaks in a day
        val byDay = allEntries.groupBy { it.date.take(10) }
        val mostInADay = findBestPeriod(byDay)

        // Most peaks in a month
        val byMonth = allEntries.groupBy { it.date.take(7) }
        val mostInAMonth = findBestPeriod(byMonth)

        // Most peaks in a year
        val byYear = allEntries.groupBy { it.date.take(4) }
        val mostInAYear = findBestPeriod(byYear)

        // Most peaks in a week (ISO Mon-Sun)
        val byWeek = allEntries.groupBy { isoWeekKey(it.date) }
        val mostInAWeek = findBestPeriod(byWeek)

        // Progress over time
        val progressTimeline = computeProgressTimeline(allEntries)

        return StatsData(
            progress = progress,
            firstPeak = firstPeak,
            latestPeak = latestPeak,
            mostVisited = mostVisited,
            hIndex = hIndex,
            mostInADay = mostInADay,
            mostInAWeek = mostInAWeek,
            mostInAMonth = mostInAMonth,
            mostInAYear = mostInAYear,
            progressTimeline = progressTimeline,
        )
    }

    private fun computeHIndex(visitCounts: Map<Long, List<ClimbDate>>): HIndexData {
        val sorted = visitCounts.entries.sortedByDescending { it.value.size }
        var h = 0
        for ((i, entry) in sorted.withIndex()) {
            if (entry.value.size >= i + 1) h = i + 1 else break
        }
        if (h == 0) return HIndexData(0, emptyList())

        // Top h mountains count towards the h-index (highlighted)
        val topH = sorted.take(h).mapNotNull { (osmId, dates) ->
            repository.getMountainByOsmId(osmId)?.let {
                HIndexMountain(it, dates.size, countsTowardsHIndex = true)
            }
        }

        // Remaining mountains with >= h visits, plus up to 5 extra with fewer visits
        val remaining = sorted.drop(h)
        val extraMountains = remaining
            .filter { it.value.size >= 1 }
            .take(5)
            .mapNotNull { (osmId, dates) ->
                repository.getMountainByOsmId(osmId)?.let {
                    HIndexMountain(it, dates.size, countsTowardsHIndex = false)
                }
            }

        return HIndexData(h, topH + extraMountains)
    }

    private fun computeProgressTimeline(allEntries: List<ClimbDate>): ProgressTimeline {
        // For each climb date, record when a mountain was first climbed and what type it is
        // Group by osmId, take earliest date per mountain
        val firstClimbPerMountain = allEntries
            .groupBy { it.osmId }
            .mapValues { (_, dates) -> dates.minByOrNull { it.date }!!.date }

        // Build sorted list of (date, mountain) pairs
        val events = firstClimbPerMountain.entries
            .map { (osmId, date) -> date to osmId }
            .sortedBy { it.first }

        if (events.isEmpty()) return ProgressTimeline(emptyList())

        val points = mutableListOf<TimelinePoint>()
        var baiyue = 0
        var xiaobaiyue = 0
        var oldXiaobaiyue = 0

        for ((date, osmId) in events) {
            val mountain = repository.getMountainByOsmId(osmId) ?: continue
            when (mountain.type) {
                MountainType.BAIYUE -> baiyue++
                MountainType.XIAOBAIYUE -> xiaobaiyue++
                MountainType.XIAOBAIYUE_OLD -> oldXiaobaiyue++
            }
            points.add(TimelinePoint(date, baiyue, xiaobaiyue, oldXiaobaiyue))
        }

        return ProgressTimeline(points)
    }

    private fun findBestPeriod(grouped: Map<String, List<ClimbDate>>): RecordStat? {
        val best = grouped.maxByOrNull { it.value.map { cd -> cd.osmId }.distinct().size }
            ?: return null
        val distinctOsmIds = best.value.map { it.osmId }.distinct()
        if (distinctOsmIds.isEmpty()) return null
        val mountains = distinctOsmIds.mapNotNull { osmId ->
            val mountain = repository.getMountainByOsmId(osmId) ?: return@mapNotNull null
            val date = best.value.filter { it.osmId == osmId }.minByOrNull { it.date }?.date?.take(10) ?: ""
            RecordMountain(mountain, date)
        }.sortedBy { it.date }
        return RecordStat(
            period = best.key,
            count = distinctOsmIds.size,
            mountains = mountains,
        )
    }

    private fun isoWeekKey(dateStr: String): String {
        if (dateStr.length < 10) return dateStr
        val year = dateStr.substring(0, 4).toIntOrNull() ?: return dateStr
        val month = dateStr.substring(5, 7).toIntOrNull() ?: return dateStr
        val day = dateStr.substring(8, 10).toIntOrNull() ?: return dateStr

        val daysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        if (isLeap) daysInMonth[2] = 29
        var dayOfYear = day
        for (m in 1 until month) dayOfYear += daysInMonth[m]

        val y0 = year - 1
        val jan1DayOfWeek = (1 + 5 * (y0 % 4) + 4 * (y0 % 100) + 6 * (y0 % 400)) % 7
        val jan1Iso = if (jan1DayOfWeek == 0) 7 else jan1DayOfWeek
        val currentIso = ((jan1Iso + dayOfYear - 2) % 7) + 1
        val weekNum = (dayOfYear - currentIso + 10) / 7

        return when {
            weekNum < 1 -> "${year - 1}-W53"
            weekNum > 52 -> {
                val p = jan1Iso
                val pPrev = if (isLeap) (p + 6) % 7 + 1 else p
                if (p == 4 || pPrev == 4) "$year-W$weekNum"
                else "${year + 1}-W01"
            }
            else -> "$year-W${weekNum.toString().padStart(2, '0')}"
        }
    }
}

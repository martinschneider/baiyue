package io.github.martinschneider.baiyue.ui.screen.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.martinschneider.baiyue.ui.theme.BaiyuePrimary
import io.github.martinschneider.baiyue.ui.theme.XiaobaiyuePrimary
import org.koin.compose.koinInject

private val OldXiaobaiyueColor = Color(0xFF9E9E9E)

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = koinInject(),
    onMountainClick: (Long) -> Unit = {}
) {
    val climbDates by viewModel.climbDates.collectAsState()
    val climbedPeaks by viewModel.climbedPeaks.collectAsState()

    val stats = remember(climbDates, climbedPeaks) {
        viewModel.computeStats(climbDates, climbedPeaks)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Statistics", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // Progress card with bars
        ProgressCard(stats)

        // Progress over time chart
        if (stats.progressTimeline.points.isNotEmpty()) {
            ProgressChartCard(stats.progressTimeline)
        }

        // Highlights card
        HighlightsCard(stats, onMountainClick)

        // Records card
        RecordsCard(stats, onMountainClick)
    }
}

// --- Progress Card ---

@Composable
private fun ProgressCard(stats: StatsData) {
    StatsCard(title = "Progress") {
        ProgressBarRow(
            "百岳 Baiyue",
            stats.progress.baiyueClimbed,
            stats.progress.baiyueTotal,
            BaiyuePrimary
        )
        Spacer(Modifier.height(8.dp))
        ProgressBarRow(
            "小百岳 Xiaobaiyue",
            stats.progress.xiaobaiyueClimbed,
            stats.progress.xiaobaiyueTotal,
            XiaobaiyuePrimary
        )
        Spacer(Modifier.height(8.dp))
        ProgressBarRow(
            "舊小百岳 Old Xiaobaiyue",
            stats.progress.oldXiaobaiyueClimbed,
            stats.progress.oldXiaobaiyueTotal,
            OldXiaobaiyueColor
        )
    }
}

@Composable
private fun ProgressBarRow(label: String, climbed: Int, total: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp)
            Text("$climbed / $total", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) climbed.toFloat() / total else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
    }
}

// --- Progress Chart Card ---

@Composable
private fun ProgressChartCard(timeline: ProgressTimeline) {
    var showBaiyue by remember { mutableStateOf(true) }
    var showXiaobaiyue by remember { mutableStateOf(true) }
    var showOldXiaobaiyue by remember { mutableStateOf(true) }

    StatsCard(title = "Progress Over Time") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = showBaiyue,
                onClick = { showBaiyue = !showBaiyue },
                label = { Text("百岳", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BaiyuePrimary,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = showXiaobaiyue,
                onClick = { showXiaobaiyue = !showXiaobaiyue },
                label = { Text("小百岳", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = XiaobaiyuePrimary,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = showOldXiaobaiyue,
                onClick = { showOldXiaobaiyue = !showOldXiaobaiyue },
                label = { Text("舊小百岳", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OldXiaobaiyueColor,
                    selectedLabelColor = Color.White
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        ProgressTimelineChart(
            timeline = timeline,
            showBaiyue = showBaiyue,
            showXiaobaiyue = showXiaobaiyue,
            showOldXiaobaiyue = showOldXiaobaiyue,
        )
    }
}

// --- Highlights Card ---

@Composable
private fun HighlightsCard(stats: StatsData, onMountainClick: (Long) -> Unit) {
    StatsCard(title = "Highlights") {
        StatEntryRow("First peak", stats.firstPeak, onMountainClick)
        StatEntryRow("Latest peak", stats.latestPeak, onMountainClick)
        StatEntryRow("Most visited", stats.mostVisited, onMountainClick)
        HIndexRow(stats.hIndex, onMountainClick)
    }
}

@Composable
private fun StatEntryRow(label: String, entry: StatEntry?, onMountainClick: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (entry != null) it.clickable { onMountainClick(entry.mountain.osmId) } else it }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp)
        Text(
            text = entry?.let {
                "${it.mountain.chinese} ${it.mountain.english} (${it.detail})"
            } ?: "-",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HIndexRow(hIndex: HIndexData, onMountainClick: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("H-index", fontSize = 14.sp)
                IconButton(
                    onClick = { showInfo = !showInfo },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "H-index info",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = hIndex.value.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (hIndex.mountains.isNotEmpty()) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        AnimatedVisibility(visible = showInfo) {
            Text(
                text = "The h-index is the number of mountains you have visited h times. For example, if you have visited 3 different mountains at least 3 times each, your h-index will be 3.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }
        AnimatedVisibility(visible = expanded && hIndex.mountains.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)) {
                hIndex.mountains.forEachIndexed { index, hm ->
                    val prevCountsTowards = if (index > 0) hIndex.mountains[index - 1].countsTowardsHIndex else true
                    if (!hm.countsTowardsHIndex && prevCountsTowards) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMountainClick(hm.mountain.osmId) }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${hm.mountain.chinese} ${hm.mountain.english}",
                            fontSize = 13.sp,
                            fontWeight = if (hm.countsTowardsHIndex) FontWeight.Medium else FontWeight.Normal,
                            color = if (hm.countsTowardsHIndex) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${hm.count}x",
                            fontSize = 12.sp,
                            fontWeight = if (hm.countsTowardsHIndex) FontWeight.Medium else FontWeight.Normal,
                            color = if (hm.countsTowardsHIndex) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// --- Records Card ---

@Composable
private fun RecordsCard(stats: StatsData, onMountainClick: (Long) -> Unit) {
    StatsCard(title = "Records") {
        ExpandableRecordRow("Most peaks in a day", stats.mostInADay, onMountainClick)
        ExpandableRecordRow("Most peaks in a week", stats.mostInAWeek, onMountainClick)
        ExpandableRecordRow("Most peaks in a month", stats.mostInAMonth, onMountainClick)
        ExpandableRecordRow("Most peaks in a year", stats.mostInAYear, onMountainClick)
    }
}

@Composable
private fun ExpandableRecordRow(label: String, record: RecordStat?, onMountainClick: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (record != null) it.clickable { expanded = !expanded } else it }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = record?.let { "${it.count} (${it.period})" } ?: "-",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (record != null) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded && record != null) {
            Column(modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)) {
                record?.mountains?.forEach { rm ->
                    MountainWithDateRow(rm, onMountainClick)
                }
            }
        }
    }
}

// --- Shared Components ---

@Composable
private fun StatsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun MountainWithDateRow(rm: RecordMountain, onMountainClick: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMountainClick(rm.mountain.osmId) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${rm.mountain.chinese} ${rm.mountain.english}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = rm.date,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

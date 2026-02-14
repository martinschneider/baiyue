package io.github.martinschneider.baiyue.ui.screen.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.martinschneider.baiyue.data.model.ClimbDate
import io.github.martinschneider.baiyue.data.model.HikeDescription
import io.github.martinschneider.baiyue.data.model.HikeWaypoint
import io.github.martinschneider.baiyue.data.model.Mountain
import io.github.martinschneider.baiyue.platform.OrganicMaps

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailContent(
    mountain: Mountain,
    isClimbed: Boolean,
    hasPhoto: Boolean,
    onToggleClimbed: () -> Unit,
    onNavigateToMaps: (String) -> Unit,
    onNavigateToOrganicMaps: (String) -> Unit,
    onCopyToClipboard: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onPhotoClick: () -> Unit,
    onDeletePhoto: () -> Unit,
    climbDates: List<ClimbDate> = emptyList(),
    onAddClimbDate: (String) -> Unit = {},
    onDeleteClimbDate: (Long) -> Unit = {},
    onViewPhoto: () -> Unit = {},
    onDownloadGpx: (String) -> Unit = {},
    onShowPeakOnMap: () -> Unit = {},
    onShowTrackOnMap: ((List<List<Double>>) -> Unit)? = null,
    peakMapContent: @Composable (() -> Unit)? = null,
    trackMapContent: @Composable ((List<List<Double>>) -> Unit)? = null,
    hikeDescription: HikeDescription? = null,
    googleMapsUrl: String,
    organicMapsUrl: String,
    coordinatesString: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header card
        DetailCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mountain.english,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { onCopyToClipboard(mountain.english) }
                        )
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("${mountain.elevationInt} m", fontSize = 16.sp)
                        Text(
                            text = mountain.regionEnum.displayName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    var showUnclimbDialog by remember { mutableStateOf(false) }
                    if (showUnclimbDialog) {
                        AlertDialog(
                            onDismissRequest = { showUnclimbDialog = false },
                            title = { Text("Mark as not climbed?") },
                            text = { Text("Do you also want to delete the ${climbDates.size} recorded climb date(s)?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showUnclimbDialog = false
                                    climbDates.forEach { onDeleteClimbDate(it.id) }
                                    onToggleClimbed()
                                }) { Text("Delete dates") }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showUnclimbDialog = false
                                    onToggleClimbed()
                                }) { Text("Keep dates") }
                            }
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isClimbed,
                            onClick = {
                                if (isClimbed && climbDates.isNotEmpty()) {
                                    showUnclimbDialog = true
                                } else {
                                    onToggleClimbed()
                                }
                            },
                            label = { Text("Climbed") },
                            leadingIcon = if (isClimbed) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                        IconButton(
                            onClick = {
                                if (hasPhoto) onViewPhoto()
                                else onPhotoClick()
                            }
                        ) {
                            Icon(
                                if (hasPhoto) Icons.Filled.CameraAlt else Icons.Outlined.CameraAlt,
                                contentDescription = if (hasPhoto) "View photo" else "Add photo",
                                modifier = Modifier.size(28.dp),
                                tint = if (hasPhoto) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (peakMapContent != null) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        peakMapContent()
                        Spacer(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onShowPeakOnMap() }
                        )
                    }
                }
            }
        }

        // Climb Dates card
        DetailCard(title = "Climbing History") {
            var deleteClimbDateId by remember { mutableStateOf<Long?>(null) }
            climbDates.forEach { climbDate ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(climbDate.date, fontSize = 14.sp)
                    IconButton(onClick = { deleteClimbDateId = climbDate.id }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove date",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            deleteClimbDateId?.let { id ->
                AlertDialog(
                    onDismissRequest = { deleteClimbDateId = null },
                    title = { Text("Delete climb date?") },
                    text = { Text("Are you sure you want to remove this date?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDeleteClimbDate(id)
                                deleteClimbDateId = null
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteClimbDateId = null }) { Text("Cancel") }
                    }
                )
            }
            var showDatePicker by remember { mutableStateOf(false) }
            TextButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add date")
            }
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    onAddClimbDate(formatEpochMillisToDate(millis))
                                }
                                showDatePicker = false
                            }
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }

        // Navigation card
        DetailCard(title = "Location") {
            Text(
                text = coordinatesString,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { onCopyToClipboard(coordinatesString) }
                )
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onNavigateToMaps(googleMapsUrl) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Maps")
                }
                OutlinedButton(
                    onClick = { onNavigateToOrganicMaps(organicMapsUrl) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Organic Maps")
                }
            }
        }

        // Hike description sections (Xiaobaiyue only)
        hikeDescription?.let { hike ->
            if (hike.intro.isNotBlank()) {
                DetailCard(title = "Description") {
                    Text(hike.intro, fontSize = 14.sp)
                }
            }

            if (hike.route.isNotBlank()) {
                DetailCard(title = "Route") {
                    hike.start?.let { start ->
                        val startLabel = if (start.name == "Start") "Start" else "Start: ${start.name}"
                        CoordinateRow(
                            icon = { Icon(Icons.Default.LocationOn, contentDescription = "Start", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                            label = startLabel,
                            onNavigateToMaps = { onNavigateToMaps("https://maps.google.com/?q=${start.lat},${start.lng}") },
                            onNavigateToOrganicMaps = { onNavigateToOrganicMaps(OrganicMaps.encodeUrl(start.lat, start.lng, start.name)) },
                            onCopyToClipboard = { onCopyToClipboard("${start.lat}, ${start.lng}") }
                        )
                    }

                    WaypointText(
                        text = hike.route,
                        waypoints = hike.waypoints,
                        onNavigateToMaps = onNavigateToMaps,
                        onNavigateToOrganicMaps = onNavigateToOrganicMaps,
                        onCopyToClipboard = onCopyToClipboard
                    )

                    hike.end?.let { end ->
                        val endLabel = if (end.name == "End") "End" else "End: ${end.name}"
                        CoordinateRow(
                            icon = { Icon(Icons.Default.Flag, contentDescription = "End", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                            label = endLabel,
                            onNavigateToMaps = { onNavigateToMaps("https://maps.google.com/?q=${end.lat},${end.lng}") },
                            onNavigateToOrganicMaps = { onNavigateToOrganicMaps(OrganicMaps.encodeUrl(end.lat, end.lng, end.name)) },
                            onCopyToClipboard = { onCopyToClipboard("${end.lat}, ${end.lng}") }
                        )
                    }
                }
            }

            if (hike.transport.isNotBlank()) {
                DetailCard(title = "Public Transport") {
                    Text(hike.transport, fontSize = 14.sp)
                }
            }

            if (hike.gpxFilename != null) {
                DetailCard(title = "GPX Track") {
                    hike.stats?.let { stats ->
                        Text(stats, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (hike.track.isNotEmpty()) {
                        if (trackMapContent != null) {
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                trackMapContent(hike.track)
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { onShowTrackOnMap?.invoke(hike.track) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        ElevationChart(
                            track = hike.track,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onDownloadGpx(hike.gpxFilename) }) {
                        Text("Open GPX")
                    }
                }
            }
        }

        // External resources card
        if (mountain.descriptions.isNotEmpty() || mountain.hikingBijiUrl != null) {
            DetailCard(title = "External Resources") {
                if (mountain.descriptions.isNotEmpty()) {
                    mountain.descriptions.forEach { desc ->
                        TextButton(onClick = { onOpenUrl(desc.url) }) {
                            Text(desc.name, fontSize = 14.sp)
                        }
                    }
                } else {
                    mountain.hikingBijiUrl?.let { url ->
                        TextButton(onClick = { onOpenUrl(url) }) {
                            Text("健行筆記 Hiking Biji", fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DetailCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}

private fun formatEpochMillisToDate(millis: Long): String {
    val days = millis / 86400000L
    var y = 1970
    var remaining = days
    while (true) {
        val daysInYear = if (isLeapYear(y)) 366L else 365L
        if (remaining < daysInYear) break
        remaining -= daysInYear
        y++
    }
    val monthDays = if (isLeapYear(y))
        intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    else
        intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var m = 0
    while (m < 12 && remaining >= monthDays[m]) {
        remaining -= monthDays[m]
        m++
    }
    val d = remaining + 1
    return "%04d-%02d-%02d".format(y, m + 1, d.toInt())
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CoordinateRow(
    icon: @Composable () -> Unit,
    label: String,
    onNavigateToMaps: () -> Unit,
    onNavigateToOrganicMaps: () -> Unit,
    onCopyToClipboard: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu = true }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon()
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Open in Maps") },
                onClick = { showMenu = false; onNavigateToMaps() }
            )
            DropdownMenuItem(
                text = { Text("Open in Organic Maps") },
                onClick = { showMenu = false; onNavigateToOrganicMaps() }
            )
            DropdownMenuItem(
                text = { Text("Copy location") },
                onClick = { showMenu = false; onCopyToClipboard() }
            )
        }
    }
}

@Composable
private fun WaypointText(
    text: String,
    waypoints: List<HikeWaypoint>,
    onNavigateToMaps: (String) -> Unit,
    onNavigateToOrganicMaps: (String) -> Unit,
    onCopyToClipboard: (String) -> Unit
) {
    val waypointMap = waypoints.associateBy { it.number }
    val paragraphs = text.split("\n\n")
    for (paragraph in paragraphs) {
        if (paragraph.isBlank()) continue
        WaypointParagraph(paragraph.trim(), waypointMap, onNavigateToMaps, onNavigateToOrganicMaps, onCopyToClipboard)
    }
}

@Composable
private fun WaypointParagraph(
    paragraph: String,
    waypointMap: Map<Int, HikeWaypoint>,
    onNavigateToMaps: (String) -> Unit,
    onNavigateToOrganicMaps: (String) -> Unit,
    onCopyToClipboard: (String) -> Unit
) {
    val pattern = Regex("""\{\{(\d+)\}\}""")
    val matches = pattern.findAll(paragraph).toList()

    if (matches.isEmpty()) {
        Text(paragraph, fontSize = 14.sp)
        return
    }

    val inlineContentMap = mutableMapOf<String, InlineTextContent>()
    var selectedWaypointNumber by remember { mutableStateOf<Int?>(null) }

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        for (match in matches) {
            append(paragraph.substring(lastIndex, match.range.first))
            val number = match.groupValues[1].toInt()
            val contentId = "wpt_$number"
            appendInlineContent(contentId, "[$number]")
            lastIndex = match.range.last + 1
        }
        append(paragraph.substring(lastIndex))
    }

    for (match in matches) {
        val number = match.groupValues[1].toInt()
        val contentId = "wpt_$number"
        val waypoint = waypointMap[number]
        inlineContentMap[contentId] = InlineTextContent(
            placeholder = Placeholder(
                width = 20.sp,
                height = 20.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable {
                        if (waypoint != null) selectedWaypointNumber = number
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 11.sp
                )
                if (waypoint != null && selectedWaypointNumber == number) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { selectedWaypointNumber = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text("${waypoint.name} (${waypoint.lat}, ${waypoint.lng})") },
                            onClick = {},
                            enabled = false
                        )
                        DropdownMenuItem(
                            text = { Text("Open in Maps") },
                            onClick = {
                                selectedWaypointNumber = null
                                onNavigateToMaps("https://maps.google.com/?q=${waypoint.lat},${waypoint.lng}")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open in Organic Maps") },
                            onClick = {
                                selectedWaypointNumber = null
                                onNavigateToOrganicMaps(OrganicMaps.encodeUrl(waypoint.lat, waypoint.lng, waypoint.name))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy location") },
                            onClick = {
                                selectedWaypointNumber = null
                                onCopyToClipboard("${waypoint.lat}, ${waypoint.lng}")
                            }
                        )
                    }
                }
            }
        }
    }

    Text(
        text = annotatedString,
        inlineContent = inlineContentMap,
        fontSize = 14.sp
    )
}

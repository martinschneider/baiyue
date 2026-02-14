package io.github.martinschneider.baiyue.ui.screen.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.martinschneider.baiyue.data.model.Mountain
import io.github.martinschneider.baiyue.ui.theme.BaiyuePrimary
import io.github.martinschneider.baiyue.ui.theme.XiaobaiyuePrimary
import org.koin.compose.koinInject

@Composable
fun ListScreen(
    viewModel: ListViewModel = koinInject(),
    onMountainClick: (Long) -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val climbed by viewModel.climbedPeaks.collectAsState()
    val photos by viewModel.photoPeaks.collectAsState()

    val mountains = remember(currentTab, searchQuery, sortOption, climbed) {
        viewModel.getFilteredSortedList(currentTab, searchQuery, sortOption)
    }

    // Per-tab scroll state: restore from ViewModel on tab switch
    val (savedIndex, savedOffset) = remember(currentTab) {
        viewModel.getScrollPosition(currentTab)
    }
    val listState = rememberLazyListState(savedIndex, savedOffset)

    // Save scroll position when it changes
    val currentTabCapture = currentTab
    DisposableEffect(currentTabCapture, listState) {
        onDispose {
            viewModel.saveScrollPosition(
                currentTabCapture,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }
    }

    val tabColor = if (currentTab == ListTab.BAIYUE) BaiyuePrimary else XiaobaiyuePrimary

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row
        PrimaryTabRow(
            selectedTabIndex = if (currentTab == ListTab.BAIYUE) 0 else 1
        ) {
            Tab(
                selected = currentTab == ListTab.BAIYUE,
                onClick = { viewModel.selectTab(ListTab.BAIYUE) },
                text = {
                    Text(
                        "百岳 Baiyue",
                        color = if (currentTab == ListTab.BAIYUE) BaiyuePrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (currentTab == ListTab.BAIYUE) FontWeight.Bold
                            else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = currentTab == ListTab.XIAOBAIYUE,
                onClick = { viewModel.selectTab(ListTab.XIAOBAIYUE) },
                text = {
                    Text(
                        "小百岳 Xiaobaiyue",
                        color = if (currentTab == ListTab.XIAOBAIYUE) XiaobaiyuePrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (currentTab == ListTab.XIAOBAIYUE) FontWeight.Bold
                            else FontWeight.Normal
                    )
                }
            )
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )

        // Sort chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortOption.entries.forEach { option ->
                FilterChip(
                    selected = sortOption == option,
                    onClick = { viewModel.setSortOption(option) },
                    label = {
                        Text(
                            when (option) {
                                SortOption.RANK -> "#"
                                SortOption.ELEVATION_ASC -> "↑ Elev"
                                SortOption.ELEVATION_DESC -> "↓ Elev"
                                SortOption.NAME -> "Name"
                            },
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Mountain list
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(mountains, key = { it.osmId }) { mountain ->
                MountainRow(
                    mountain = mountain,
                    isClimbed = climbed[mountain.osmId] == true,
                    accentColor = tabColor,
                    onToggleClimbed = { viewModel.toggleClimbed(mountain.osmId) },
                    onClick = { onMountainClick(mountain.osmId) }
                )
            }
        }

    }
}

@Composable
private fun MountainRow(
    mountain: Mountain,
    isClimbed: Boolean,
    accentColor: Color,
    onToggleClimbed: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isClimbed,
            onCheckedChange = { onToggleClimbed() },
            modifier = Modifier.size(32.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = accentColor
            )
        )

        Spacer(Modifier.width(8.dp))

        // Rank number
        Text(
            text = mountain.displayId ?: "-",
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mountain.chinese,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                text = mountain.english,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "${mountain.elevationInt}m",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    HorizontalDivider()
}

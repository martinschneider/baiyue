package io.github.martinschneider.baiyue.ui.screen.list

import io.github.martinschneider.baiyue.data.model.Mountain
import io.github.martinschneider.baiyue.data.model.MountainType
import io.github.martinschneider.baiyue.data.repository.MountainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ListTab { BAIYUE, XIAOBAIYUE }
enum class SortOption { RANK, ELEVATION_ASC, ELEVATION_DESC, NAME }

class ListViewModel(private val repository: MountainRepository) {
    private val _currentTab = MutableStateFlow(ListTab.BAIYUE)
    val currentTab: StateFlow<ListTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.RANK)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Per-tab scroll position: first visible item index and scroll offset
    private val _scrollPositions = mutableMapOf<ListTab, Pair<Int, Int>>()

    val climbedPeaks = repository.climbedPeaks
    val photoPeaks = repository.photoPeaks

    fun saveScrollPosition(tab: ListTab, firstVisibleIndex: Int, scrollOffset: Int) {
        _scrollPositions[tab] = firstVisibleIndex to scrollOffset
    }

    fun getScrollPosition(tab: ListTab): Pair<Int, Int> =
        _scrollPositions[tab] ?: (0 to 0)

    fun selectTab(tab: ListTab) { _currentTab.value = tab }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortOption(sort: SortOption) { _sortOption.value = sort }

    fun getBaiyueList(): List<Mountain> = repository.getBaiyue()
    fun getXiaobaiyueList(): List<Mountain> = repository.getXiaobaiyue()

    fun getFilteredSortedList(
        tab: ListTab,
        query: String,
        sort: SortOption
    ): List<Mountain> {
        val base = when (tab) {
            ListTab.BAIYUE -> repository.getBaiyue()
            ListTab.XIAOBAIYUE -> repository.getXiaobaiyue()
        }

        val filtered = if (query.isBlank()) base else base.filter {
            it.chinese.contains(query, ignoreCase = true) ||
            it.english.contains(query, ignoreCase = true)
        }

        return when (sort) {
            SortOption.RANK -> {
                if (tab == ListTab.BAIYUE) {
                    filtered.sortedBy { it.id ?: Int.MAX_VALUE }
                } else {
                    filtered.sortedWith(compareBy<Mountain> { xiaobaiyueSortKey(it) })
                }
            }
            SortOption.ELEVATION_ASC -> filtered.sortedBy { it.elevation }
            SortOption.ELEVATION_DESC -> filtered.sortedByDescending { it.elevation }
            SortOption.NAME -> filtered.sortedBy { it.english }
        }
    }

    fun toggleClimbed(osmId: Long) {
        repository.toggleClimbed(osmId)
    }

    fun init() {
        repository.loadClimbedStatus()
    }

    /**
     * Sort key for xiaobaiyue: numeric part first, then suffix.
     * "1" -> (1, ""), "6a" -> (6, "a"), "21" -> (21, "")
     */
    private fun xiaobaiyueSortKey(mountain: Mountain): Comparable<*> {
        val id = mountain.xiaobaiyueId ?: return Int.MAX_VALUE as Comparable<*>
        val num = id.takeWhile { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
        val suffix = id.dropWhile { it.isDigit() }
        // Pack into a single comparable: sort by number, then suffix (empty before "a")
        return "${"${num}".padStart(5, '0')}$suffix"
    }
}

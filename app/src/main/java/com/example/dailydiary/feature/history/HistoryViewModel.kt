package com.example.dailydiary.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onMoodFilterSelected(mood: Mood?) {
        _uiState.update { it.copy(selectedMoodFilter = mood) }
        applyFilters()
    }

    fun onToggleSearch() {
        _uiState.update { it.copy(isSearchExpanded = !it.isSearchExpanded) }
        if (!_uiState.value.isSearchExpanded) {
            _uiState.update { it.copy(searchQuery = "") }
            loadEntries()
        }
    }

    private fun loadEntries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val entries = repository.getAllEntries()
                val enriched = entries.map { entry ->
                    val mood = Mood.fromId(entry.moodId)
                    val tags = repository.getTagsForEntry(entry.id)
                    DiaryEntryWithMeta(entry = entry, mood = mood, tags = tags)
                }
                _uiState.update { it.copy(entries = enriched, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "加载失败") }
            }
        }
    }

    private fun applyFilters() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val entries = when {
                    state.searchQuery.isNotBlank() && state.selectedMoodFilter != null -> {
                        val bySearch = repository.searchByContent(state.searchQuery)
                        bySearch.filter { it.moodId == state.selectedMoodFilter!!.id }
                    }
                    state.searchQuery.isNotBlank() -> {
                        repository.searchByContent(state.searchQuery)
                    }
                    state.selectedMoodFilter != null -> {
                        repository.getByMood(state.selectedMoodFilter.id)
                    }
                    else -> repository.getAllEntries()
                }
                val enriched = entries.map { entry ->
                    val mood = Mood.fromId(entry.moodId)
                    val tags = repository.getTagsForEntry(entry.id)
                    DiaryEntryWithMeta(entry = entry, mood = mood, tags = tags)
                }
                _uiState.update { it.copy(entries = enriched, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "查询失败") }
            }
        }
    }
}

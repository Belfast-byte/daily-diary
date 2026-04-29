package com.example.dailydiary.feature.today

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
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.seedDefaultTagsIfNeeded()
            } catch (_: Exception) {
                // non-critical; user can still use the app
            }
        }

        val today = LocalDate.now()

        viewModelScope.launch {
            // one-shot: load initial state including tags
            val entry = repository.getEntryByDate(today)
            if (entry != null) {
                val mood = try {
                    Mood.fromId(entry.moodId)
                } catch (_: Exception) {
                    null
                }
                val tags = repository.getTagsForEntry(entry.id)
                _uiState.update {
                    it.copy(
                        selectedMood = mood,
                        content = entry.content,
                        isExistingEntry = true,
                        selectedTagIds = tags.map { t -> t.id }.toSet()
                    )
                }
            }

            // reactive: observe subsequent changes (e.g., from calendar edits)
            repository.observeEntryByDate(today).collect { updatedEntry ->
                _uiState.update { state ->
                    if (updatedEntry != null) {
                        val mood = try {
                            Mood.fromId(updatedEntry.moodId)
                        } catch (_: Exception) {
                            null
                        }
                        state.copy(
                            selectedMood = mood,
                            content = updatedEntry.content,
                            isExistingEntry = true
                        )
                    } else state
                }
            }
        }

        viewModelScope.launch {
            repository.observeAllTags().collect { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
        }
    }

    fun onMoodSelected(mood: Mood) {
        _uiState.update { it.copy(selectedMood = mood) }
    }

    fun onContentChanged(text: String) {
        _uiState.update { it.copy(content = text) }
    }

    fun onTagToggled(tagId: Long) {
        _uiState.update { state ->
            val newSet = state.selectedTagIds.toMutableSet()
            if (newSet.contains(tagId)) newSet.remove(tagId) else newSet.add(tagId)
            state.copy(selectedTagIds = newSet)
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving) return
        val mood = state.selectedMood ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                repository.saveEntry(
                    date = LocalDate.now(),
                    mood = mood,
                    content = state.content,
                    tagIds = state.selectedTagIds.toList()
                )
                _uiState.update { it.copy(isSaving = false, hasSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "保存失败"
                    )
                }
            }
        }
    }
}

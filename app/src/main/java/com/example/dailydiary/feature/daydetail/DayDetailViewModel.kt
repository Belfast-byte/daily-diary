package com.example.dailydiary.feature.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailydiary.domain.model.ActivityTag
import com.example.dailydiary.domain.model.DiaryEntry
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DayDetailUiState(
    val entry: DiaryEntry? = null,
    val mood: Mood? = null,
    val tags: List<ActivityTag> = emptyList(),
    val isEditing: Boolean = false,
    val editContent: String = "",
    val editMood: Mood? = null,
    val editTagIds: Set<Long> = emptySet(),
    val availableTags: List<ActivityTag> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailUiState())
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    private var entryObserverJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeAllTags().collect { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
        }
    }

    fun load(date: LocalDate) {
        entryObserverJob?.cancel()
        entryObserverJob = viewModelScope.launch {
            repository.observeEntryByDate(date).collect { entry ->
                val mood = entry?.let { Mood.fromId(it.moodId) }
                val tags = entry?.let { repository.getTagsForEntry(it.id) } ?: emptyList()
                _uiState.update { state ->
                    state.copy(entry = entry, mood = mood, tags = tags)
                }
            }
        }
    }

    fun toggleEdit() {
        val state = _uiState.value
        if (state.isEditing) {
            _uiState.update { it.copy(isEditing = false) }
        } else {
            _uiState.update {
                it.copy(
                    isEditing = true,
                    editContent = it.entry?.content ?: "",
                    editMood = it.mood,
                    editTagIds = it.tags.map { t -> t.id }.toSet()
                )
            }
        }
    }

    fun onEditContentChanged(text: String) {
        _uiState.update { it.copy(editContent = text) }
    }

    fun onEditMoodSelected(mood: Mood) {
        _uiState.update { it.copy(editMood = mood) }
    }

    fun onEditTagToggled(tagId: Long) {
        _uiState.update { state ->
            val newSet = state.editTagIds.toMutableSet()
            if (newSet.contains(tagId)) newSet.remove(tagId) else newSet.add(tagId)
            state.copy(editTagIds = newSet)
        }
    }

    fun onSave() {
        val state = _uiState.value
        val mood = state.editMood ?: return
        val entry = state.entry ?: return
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                repository.saveEntry(
                    date = entry.entryDate,
                    mood = mood,
                    content = state.editContent,
                    tagIds = state.editTagIds.toList()
                )
                _uiState.update { it.copy(isSaving = false, isEditing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "保存失败")
                }
            }
        }
    }
}

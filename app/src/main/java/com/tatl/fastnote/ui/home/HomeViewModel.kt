package com.tatl.fastnote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tatl.fastnote.data.db.NoteEntity
import com.tatl.fastnote.data.repository.NoteRepository
import com.tatl.fastnote.data.user.LanguageManager
import com.tatl.fastnote.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Represents a single item in the timeline list.
 * Can be either a date header or a note entry.
 */
sealed class TimelineItem {
    data class DateHeader(
        val dateKey: String,
        val displayText: String // Localized: "Hôm nay" / "Today" / "今日" / "Heute" / "Сегодня"
    ) : TimelineItem()

    data class NoteItem(
        val note: NoteEntity,
        val timeText: String // "08:00", "14:30"
    ) : TimelineItem()
}

/**
 * Feedback for edit operations — shown as Snackbar/Toast
 */
sealed class EditFeedback {
    data object None : EditFeedback()
    data class Success(val message: String) : EditFeedback()
    data class Error(val message: String) : EditFeedback()
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repository: NoteRepository) : ViewModel() {

    // --- Search ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Expanded note (accordion) ---
    private val _expandedNoteId = MutableStateFlow<Long?>(null)
    val expandedNoteId: StateFlow<Long?> = _expandedNoteId.asStateFlow()

    fun toggleExpand(noteId: Long) {
        _expandedNoteId.value = if (_expandedNoteId.value == noteId) null else noteId
    }

    // --- Notes flow (reacts to search query) ---
    private val filteredNotes: StateFlow<List<NoteEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allNotes
            } else {
                repository.searchNotes(query.trim())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Notes grouped into a flat timeline list with date headers.
     * Reacts to both search query AND language changes — khi đổi ngôn ngữ,
     * tên thứ/ngày tháng trong timeline header sẽ tự động rebuild.
     */
    val timelineItems: StateFlow<List<TimelineItem>> = combine(
        filteredNotes,
        LanguageManager.currentLanguage
    ) { noteList, _ ->
        buildTimelineList(noteList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun buildTimelineList(notes: List<NoteEntity>): List<TimelineItem> {
        if (notes.isEmpty()) return emptyList()

        val result = mutableListOf<TimelineItem>()
        var lastDateKey = ""

        // Notes are already ordered by createdAt DESC from DAO
        for (note in notes) {
            val dateKey = DateUtils.getDateKey(note.createdAt)

            // Insert date header when day changes
            if (dateKey != lastDateKey) {
                val headerText = DateUtils.formatRelativeDay(note.createdAt)
                result.add(TimelineItem.DateHeader(dateKey, headerText))
                lastDateKey = dateKey
            }

            // Insert note with time
            val timeText = DateUtils.formatTimeOnly(note.createdAt)
            result.add(TimelineItem.NoteItem(note, timeText))
        }

        return result
    }

    // --- Actions ---

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            try {
                repository.deleteNote(note)
            } catch (e: Exception) {
                _editFeedback.value = EditFeedback.Error("Không thể xóa ghi chú")
            }
        }
    }

    // --- Edit feedback ---
    private val _editFeedback = MutableStateFlow<EditFeedback>(EditFeedback.None)
    val editFeedback: StateFlow<EditFeedback> = _editFeedback.asStateFlow()

    fun clearFeedback() {
        _editFeedback.value = EditFeedback.None
    }

    /**
     * Validate and save edited note content.
     * Returns false if validation fails.
     */
    fun updateNoteContent(note: NoteEntity, newContent: String): Boolean {
        // Sanitize input
        val sanitized = newContent.trim()

        // Validate: not empty
        if (sanitized.isBlank()) {
            _editFeedback.value = EditFeedback.Error("Nội dung không được để trống")
            return false
        }

        // Validate: reasonable length (max 50,000 chars)
        if (sanitized.length > 50_000) {
            _editFeedback.value = EditFeedback.Error("Nội dung quá dài (tối đa 50.000 ký tự)")
            return false
        }

        viewModelScope.launch {
            try {
                // Also update title from first 10 words of new content
                val newTitle = sanitized.split(" ")
                    .take(10)
                    .joinToString(" ")
                    .let { if (it.length > 50) it.take(50) + "..." else it }

                repository.updateNote(
                    note.copy(
                        content = sanitized,
                        title = newTitle
                    )
                )
                _editFeedback.value = EditFeedback.Success("Đã lưu thay đổi")
            } catch (e: Exception) {
                _editFeedback.value = EditFeedback.Error("Lỗi khi lưu: ${e.localizedMessage ?: "Không xác định"}")
            }
        }
        return true
    }

    /**
     * Create a new note manually with title and content.
     */
    fun createNote(title: String, content: String): Boolean {
        val sanitizedContent = content.trim()
        val sanitizedTitle = title.trim()

        if (sanitizedContent.isBlank() && sanitizedTitle.isBlank()) {
            _editFeedback.value = EditFeedback.Error("Nội dung không được để trống")
            return false
        }

        viewModelScope.launch {
            try {
                repository.insertNote(
                    title = sanitizedTitle.ifBlank { "Ghi chú" },
                    content = sanitizedContent
                )
                _editFeedback.value = EditFeedback.Success("Đã tạo ghi chú mới")
            } catch (e: Exception) {
                _editFeedback.value = EditFeedback.Error("Lỗi khi tạo ghi chú: ${e.localizedMessage ?: "Không xác định"}")
            }
        }
        return true
    }

    /**
     * Export today's notes as formatted text for AI consumption
     */
    suspend fun getTodayNotesText(): String {
        return repository.exportTodayAsText()
    }

    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
    }
}

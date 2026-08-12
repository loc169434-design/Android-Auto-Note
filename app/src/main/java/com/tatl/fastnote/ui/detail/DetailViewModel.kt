package com.tatl.fastnote.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tatl.fastnote.data.db.NoteEntity
import com.tatl.fastnote.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: NoteRepository,
    private val noteId: Long
) : ViewModel() {

    private val _note = MutableStateFlow<NoteEntity?>(null)
    val note: StateFlow<NoteEntity?> = _note.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getNoteById(noteId).collect { entity ->
                _note.value = entity
            }
        }
    }

    fun updateNote(title: String, content: String) {
        val currentNote = _note.value ?: return
        viewModelScope.launch {
            repository.updateNote(currentNote.copy(title = title, content = content))
            _isSaved.value = true
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            repository.deleteNoteById(noteId)
        }
    }

    class Factory(
        private val repository: NoteRepository,
        private val noteId: Long
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(repository, noteId) as T
        }
    }
}

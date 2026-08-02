package com.example.androidautonote.data.repository

import com.example.androidautonote.data.db.NoteDao
import com.example.androidautonote.data.db.NoteEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    val noteCount: Flow<Int> = noteDao.getNoteCount()

    fun getNoteById(noteId: Long): Flow<NoteEntity?> = noteDao.getNoteById(noteId)

    suspend fun getNoteByIdOnce(noteId: Long): NoteEntity? = noteDao.getNoteByIdOnce(noteId)

    fun getRecentNotes(limit: Int = 5): Flow<List<NoteEntity>> = noteDao.getRecentNotes(limit)

    fun getNotesCountToday(): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return noteDao.getNotesCountToday(calendar.timeInMillis)
    }

    suspend fun insertNote(title: String, content: String): Long {
        val note = NoteEntity(
            title = title,
            content = content
        )
        return noteDao.insert(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.update(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.delete(note)
    }

    suspend fun deleteNoteById(noteId: Long) {
        noteDao.deleteById(noteId)
    }
}

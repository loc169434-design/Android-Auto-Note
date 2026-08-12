package com.tatl.fastnote.data.repository

import com.tatl.fastnote.data.db.NoteDao
import com.tatl.fastnote.data.db.NoteEntity
import com.tatl.fastnote.util.DateUtils
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

    /**
     * Search notes by title or content
     */
    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    /**
     * Get all notes from today (for AI share export)
     */
    suspend fun getNotesToday(): List<NoteEntity> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return noteDao.getNotesToday(calendar.timeInMillis)
    }

    /**
     * Export today's notes as formatted text for AI consumption
     */
    suspend fun exportTodayAsText(): String {
        val todayNotes = getNotesToday()
        if (todayNotes.isEmpty()) return ""

        return buildString {
            appendLine("📋 Ghi chú trong ngày hôm nay:")
            appendLine("═".repeat(40))
            appendLine()
            for (note in todayNotes) {
                val time = DateUtils.formatTimeOnly(note.createdAt)
                appendLine("🕐 $time")
                appendLine(note.content)
                appendLine()
                appendLine("─".repeat(30))
                appendLine()
            }
        }.trimEnd()
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

package com.tatl.fastnote.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteByIdOnce(noteId: Long): NoteEntity?

    @Query("SELECT COUNT(*) FROM notes")
    fun getNoteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE createdAt >= :startOfDay")
    fun getNotesCountToday(startOfDay: Long): Flow<Int>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentNotes(limit: Int): Flow<List<NoteEntity>>

    /**
     * Full-text search across title and content
     */
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    /**
     * Get all notes created today (for AI share)
     */
    @Query("SELECT * FROM notes WHERE createdAt >= :startOfDay ORDER BY createdAt ASC")
    suspend fun getNotesToday(startOfDay: Long): List<NoteEntity>
}

package com.example.androidautonote

import android.app.Application
import com.example.androidautonote.data.db.AppDatabase
import com.example.androidautonote.data.repository.NoteRepository

class AutoNoteApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val noteRepository: NoteRepository by lazy { NoteRepository(database.noteDao()) }

    companion object {
        lateinit var instance: AutoNoteApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}

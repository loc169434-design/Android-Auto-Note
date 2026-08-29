package com.tatl.fastnote

import android.app.Application
import com.tatl.fastnote.data.db.AppDatabase
import com.tatl.fastnote.data.repository.NoteRepository
import com.tatl.fastnote.data.user.LanguageManager
import com.tatl.fastnote.data.user.UserManager
import com.tatl.fastnote.util.ThemePreferences

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
        ThemePreferences.init(this)
        UserManager.init(this)
        LanguageManager.init(this)
        com.tatl.fastnote.sync.GoogleDriveSyncWorker.schedulePeriodicSync(this)
    }
}

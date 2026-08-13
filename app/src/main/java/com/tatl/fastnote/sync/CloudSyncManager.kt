package com.tatl.fastnote.sync

import android.util.Log
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tatl.fastnote.auth.AuthManager
import com.tatl.fastnote.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Handles bidirectional sync between local ghichu.txt and Firebase Firestore.
 *
 * Firestore structure:
 *   users/{uid}/notes/{timestamp_key} = {
 *     header:    "Thứ ba, ngày 12-08-2026 lúc 09.30"
 *     content:   "nội dung ghi chú..."
 *     createdAt: server timestamp
 *   }
 *
 * All operations are fire-and-forget on a background coroutine.
 * No UI blocking, no exceptions propagated to caller.
 */
object CloudSyncManager {

    private const val TAG = "CloudSyncManager"
    private val db get() = Firebase.firestore

    // ── Push single entry to cloud ─────────────────────────────────────────────

    /**
     * Push one NoteEntry to Firestore after a local save.
     * Key = header string (unique per entry, matches local timestamp).
     */
    suspend fun pushEntry(entry: FileHelper.NoteEntry) {
        val uid = AuthManager.uid ?: return
        try {
            val docId = entry.header.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            db.collection("users").document(uid)
                .collection("notes").document(docId)
                .set(mapOf(
                    "header"    to entry.header,
                    "content"   to entry.content,
                    "fullLine"  to entry.fullLine,
                    "syncedAt"  to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ), SetOptions.merge())
                .await()
            Log.d(TAG, "Pushed entry: ${entry.header}")
        } catch (e: Exception) {
            Log.w(TAG, "pushEntry failed (will retry next launch): ${e.message}")
        }
    }

    // ── Pull + merge all cloud entries to local file ───────────────────────────

    /**
     * Download all notes from Firestore and merge with local file.
     * Called once on app start after login (or reinstall).
     * Uses timestamp key to avoid duplicates.
     */
    suspend fun syncFromCloud(context: android.content.Context) = withContext(Dispatchers.IO) {
        val uid = AuthManager.uid ?: return@withContext
        try {
            val snapshot = db.collection("users").document(uid)
                .collection("notes")
                .orderBy("syncedAt")
                .get()
                .await()

            val localEntries = FileHelper.parseEntries(context).map { it.header }.toHashSet()
            var added = 0

            // Cloud entries NOT in local file → append to file
            for (doc in snapshot.documents) {
                val header  = doc.getString("header")  ?: continue
                val content = doc.getString("content") ?: continue
                if (header !in localEntries) {
                    FileHelper.appendNote(context, content)
                    added++
                }
            }

            Log.d(TAG, "syncFromCloud: added $added missing entries")
        } catch (e: Exception) {
            Log.w(TAG, "syncFromCloud failed: ${e.message}")
        }
    }

    // ── Push entire local file (on reinstall / first login) ───────────────────

    /**
     * Upload all local entries that are not yet in Firestore.
     * Called once after login on a fresh install that already has data.
     */
    suspend fun pushAllLocal(context: android.content.Context) = withContext(Dispatchers.IO) {
        val uid = AuthManager.uid ?: return@withContext
        try {
            val local = FileHelper.parseEntries(context)
            if (local.isEmpty()) return@withContext

            val snapshot = db.collection("users").document(uid)
                .collection("notes").get().await()
            val cloudKeys = snapshot.documents.mapNotNull { it.getString("header") }.toHashSet()

            var pushed = 0
            for (entry in local) {
                if (entry.header !in cloudKeys) {
                    pushEntry(entry)
                    pushed++
                }
            }
            Log.d(TAG, "pushAllLocal: pushed $pushed entries")
        } catch (e: Exception) {
            Log.w(TAG, "pushAllLocal failed: ${e.message}")
        }
    }
}

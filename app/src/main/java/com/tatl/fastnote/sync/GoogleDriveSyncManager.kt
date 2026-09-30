package com.tatl.fastnote.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.tatl.fastnote.AutoNoteApplication
import com.tatl.fastnote.util.FileHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Google Drive appDataFolder Sync Manager (Bản Đặc Tả V38 - Phần 7).
 *
 * Triết lý & Kỹ thuật:
 *  1. Scope: https://www.googleapis.com/auth/drive.appdata (Non-sensitive, duyệt tự động)
 *  2. Vị trí lưu trữ: appDataFolder (hoàn toàn tàng hình trên Google Drive người dùng)
 *  3. REST API v3 qua OkHttp siêu nhẹ, không phụ thuộc Google Drive SDK cồng kềnh
 *  4. Thuật toán Append-Merge:
 *     - Sử dụng Header "- Thứ..., ngày DD-MM-YYYY lúc HH.MM:" làm Khóa chính
 *     - Hợp nhất 2 chiều (Local <-> Drive)
 *     - Luôn xếp thứ tự mới nhất lên đầu (cuộn ngược vô tận)
 */
object GoogleDriveSyncManager {

    private const val TAG = "GoogleDriveSyncManager"
    const val DRIVE_APPDATA_SCOPE_URI = "https://www.googleapis.com/auth/drive.appdata"
    val DRIVE_APPDATA_SCOPE = Scope(DRIVE_APPDATA_SCOPE_URI)
    private const val OAUTH_SCOPE_STRING = "oauth2:$DRIVE_APPDATA_SCOPE_URI"

    sealed interface SyncStatus {
        object Idle : SyncStatus
        data class Syncing(val messageResId: Int = com.tatl.fastnote.R.string.str_sync_syncing) : SyncStatus
        data class Success(val messageResId: Int = com.tatl.fastnote.R.string.str_sync_success, val count: Int = 0, val timestamp: Long = System.currentTimeMillis()) : SyncStatus
        data class Error(val messageResId: Int = com.tatl.fastnote.R.string.str_sync_failed, val rawMessage: String? = null) : SyncStatus
    }

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private const val DRIVE_FILE_NAME = "ghichu.txt"
    private const val DRIVE_API_FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"

    // ── Blacklist: header đã xóa cố ý (mini tombstone — lưu trong SharedPreferences) ──────
    private const val PREFS_NAME = "sync_prefs"
    private const val KEY_BLACKLIST = "deleted_headers_v1"   // JSON: [{"h":"...","t":123}]
    private const val BLACKLIST_TTL_MS = 30L * 24 * 60 * 60 * 1000  // 30 ngày

    /**
     * Thêm các header đã bị xóa vào blacklist.
     * Gọi từ doSave() sau khi stripOrphanHeaders trả về có strippedHeaders.
     */
    fun addToBlacklist(context: android.content.Context, headers: List<String>) {
        if (headers.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val existing = parseBlacklistJson(prefs.getString(KEY_BLACKLIST, "[]") ?: "[]")
        val now = System.currentTimeMillis()
        val updated = existing.toMutableList()
        for (h in headers) {
            if (updated.none { it.first == h }) updated.add(Pair(h, now))
        }
        prefs.edit().putString(KEY_BLACKLIST, serializeBlacklistJson(updated)).apply()
        Log.d(TAG, "Blacklisted ${headers.size} deleted header(s)")
    }

    private fun getBlacklist(context: android.content.Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return parseBlacklistJson(prefs.getString(KEY_BLACKLIST, "[]") ?: "[]").map { it.first }.toSet()
    }

    private fun cleanupBlacklist(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val all = parseBlacklistJson(prefs.getString(KEY_BLACKLIST, "[]") ?: "[]")
        val cutoff = System.currentTimeMillis() - BLACKLIST_TTL_MS
        val fresh = all.filter { it.second > cutoff }
        if (fresh.size < all.size) {
            prefs.edit().putString(KEY_BLACKLIST, serializeBlacklistJson(fresh)).apply()
            Log.d(TAG, "Cleaned up ${all.size - fresh.size} expired blacklist entries")
        }
    }

    private fun parseBlacklistJson(json: String): List<Pair<String, Long>> {
        return try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).mapNotNull {
                val obj = arr.optJSONObject(it) ?: return@mapNotNull null
                Pair(obj.optString("h", ""), obj.optLong("t", 0L))
            }.filter { it.first.isNotBlank() }
        } catch (e: Exception) { emptyList() }
    }

    private fun serializeBlacklistJson(list: List<Pair<String, Long>>): String {
        val arr = org.json.JSONArray()
        for ((h, t) in list) arr.put(org.json.JSONObject().put("h", h).put("t", t))
        return arr.toString()
    }


    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Lấy OAuth2 Access Token từ Google Sign-In Account để gọi Drive REST API
     */
    suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
            val androidAccount = account.account ?: return@withContext null
            GoogleAuthUtil.getToken(context, androidAccount, OAUTH_SCOPE_STRING)
        } catch (e: Exception) {
            Log.w(TAG, "getAccessToken failed: ${e.message}")
            null
        }
    }

    /**
     * Tìm fileId của ghichu.txt trong appDataFolder
     */
    private suspend fun findAppDataFileId(token: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$DRIVE_API_FILES_URL?spaces=appDataFolder&q=name%3D%27$DRIVE_FILE_NAME%27+and+trashed%3Dfalse&fields=files(id,name)"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    Log.w(TAG, "findAppDataFileId error: ${response.code} ${response.message} - body: $body")
                    return@withContext null
                }
                if (body == null) return@withContext null
                val json = JSONObject(body)
                val files = json.optJSONArray("files") ?: return@withContext null
                if (files.length() > 0) {
                    return@withContext files.getJSONObject(0).optString("id")
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "findAppDataFileId exception", e)
            null
        }
    }

    /**
     * Tải nội dung văn bản thô của ghichu.txt từ Drive appDataFolder
     */
    private suspend fun downloadDriveContent(token: String, fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$DRIVE_API_FILES_URL/$fileId?alt=media"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext response.body?.string()
                } else {
                    Log.w(TAG, "downloadDriveContent error: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "downloadDriveContent exception", e)
            null
        }
    }

    /**
     * Tạo file ghichu.txt mới trong appDataFolder (Multipart upload)
     */
    private suspend fun createDriveFile(token: String, content: String): String? = withContext(Dispatchers.IO) {
        try {
            val metadataJson = JSONObject().apply {
                put("name", DRIVE_FILE_NAME)
                put("parents", org.json.JSONArray().put("appDataFolder"))
            }.toString()

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    DRIVE_FILE_NAME,
                    content.toRequestBody("text/plain; charset=UTF-8".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$DRIVE_UPLOAD_URL?uploadType=multipart")
                .addHeader("Authorization", "Bearer $token")
                .post(multipartBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val resBody = response.body?.string()
                if (response.isSuccessful) {
                    if (resBody == null) return@withContext null
                    val json = JSONObject(resBody)
                    val newId = json.optString("id")
                    Log.d(TAG, "Created new ghichu.txt on Drive with id: $newId")
                    return@withContext newId
                } else {
                    Log.w(TAG, "createDriveFile failed: ${response.code} ${response.message} - body: $resBody")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "createDriveFile exception", e)
            null
        }
    }

    /**
     * Cập nhật nội dung file ghichu.txt trên Drive
     */
    private suspend fun updateDriveFile(token: String, fileId: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBody = content.toRequestBody("text/plain; charset=UTF-8".toMediaType())
            val request = Request.Builder()
                .url("$DRIVE_UPLOAD_URL/$fileId?uploadType=media")
                .addHeader("Authorization", "Bearer $token")
                .patch(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val ok = response.isSuccessful
                if (ok) {
                    Log.d(TAG, "Updated ghichu.txt on Drive successfully")
                } else {
                    val resBody = response.body?.string()
                    Log.w(TAG, "updateDriveFile failed: ${response.code} ${response.message} - body: $resBody")
                }
                ok
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateDriveFile exception", e)
            false
        }
    }

    private val DATE_PARSER_REGEX = Regex("""(\d{1,2})[-/](\d{1,2})[-/](\d{4}).*?(\d{1,2})[\.:](\d{2})""")

    /**
     * Trích xuất timestamp (ms) từ header ngày tháng để sắp xếp theo thời gian chính xác.
     */
    fun parseTimestampFromHeader(header: String): Long {
        return try {
            val match = DATE_PARSER_REGEX.find(header) ?: return 0L
            val (dStr, mStr, yStr, hStr, minStr) = match.destructured
            val cal = java.util.Calendar.getInstance()
            cal.set(
                yStr.toInt(),
                mStr.toInt() - 1,
                dStr.toInt(),
                hStr.toInt(),
                minStr.toInt(),
                0
            )
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Phân tách nội dung text phẳng thành danh sách các khối ghi chú độc lập
     */
    private fun parseRawTextToBlocks(rawText: String): List<FileHelper.NoteEntry> {
        if (rawText.isBlank()) return emptyList()
        val lines = rawText.lines()
        val entries = mutableListOf<FileHelper.NoteEntry>()

        var currentHeader: String? = null
        val currentContent = StringBuilder()

        fun flush() {
            val h = currentHeader ?: ""
            val c = currentContent.toString().trim()
            if (h.isNotEmpty() || c.isNotEmpty()) {
                entries.add(
                    FileHelper.NoteEntry(
                        header = h,
                        content = c,
                        fullLine = if (h.isNotEmpty()) "- $h: $c" else c
                    )
                )
            }
            currentHeader = null
            currentContent.clear()
        }

        for (line in lines) {
            val trimmed = line.trim()
            val match = FileHelper.DATE_HEADER_REGEX.find(trimmed)
            if (match != null) {
                flush()
                val matchEnd = match.range.last + 1
                val rawHeader = match.value.trimStart('-', ' ').trimEnd(':').trim()
                currentHeader = rawHeader
                val contentAfter = trimmed.substring(matchEnd).trimStart()
                if (contentAfter.isNotEmpty()) {
                    currentContent.append(contentAfter)
                }
            } else if (trimmed.isNotEmpty()) {
                if (currentContent.isNotEmpty()) currentContent.append("\n")
                currentContent.append(line)
            }
        }

        flush()
        return entries
    }

    /**
     * Thuật toán Append-Merge hai chiều giữa Local và Drive (V38 Phần 7.4).
     *
     * Thực hiện:
     *  1. Lấy Access Token từ tài khoản Google
     *  2. Tải ghichu.txt từ appDataFolder trên Drive (nếu có)
     *  3. Đối chiếu các nhãn thời gian:
     *     - Nhãn nào trên Drive có mà dưới máy chưa có: Append vào máy & Room DB
     *     - Nhãn nào dưới máy có mà trên Drive chưa có: Append lên Drive
     *  4. Cập nhật lại cả 2 phía đồng nhất theo thứ tự thời gian chuẩn xác
     */
    suspend fun sync(context: Context): Boolean = withContext(Dispatchers.IO) {
        // Chỉ người dùng ĐÃ MUA Premium mới được đồng bộ đám mây (V38 Phần 7 & 9)
        val isPrem = com.tatl.fastnote.billing.PremiumManager.isPremium(context)
        if (!isPrem) {
            Log.d(TAG, "Sync skipped: User is not Premium (Cloud Sync is a Premium privilege).")
            _syncStatus.value = SyncStatus.Idle
            return@withContext false
        }

        _syncStatus.value = SyncStatus.Syncing(com.tatl.fastnote.R.string.str_sync_connecting)
        try {
            val token = getAccessToken(context)
            if (token == null) {
                Log.d(TAG, "No Google access token (user not logged in with Google)")
                _syncStatus.value = SyncStatus.Idle
                return@withContext false
            }

            _syncStatus.value = SyncStatus.Syncing(com.tatl.fastnote.R.string.str_sync_syncing)

            // 1. Đọc dữ liệu local (ưu tiên raw.txt, fallback ghichu_clean.txt)
            val localRaw = FileHelper.readRawFile(context).ifBlank { FileHelper.readGuidiFile(context) ?: "" }
            val localEntries = parseRawTextToBlocks(localRaw)
            val localHeaderMap = localEntries.associateBy { it.header }

            // 2. Tìm file trên Drive
            val fileId = findAppDataFileId(token)

            if (fileId == null) {
                // Trên Drive chưa có file -> nếu local có thì tạo mới trên Drive
                if (localRaw.isNotBlank()) {
                    createDriveFile(token, localRaw)
                    Log.d(TAG, "Uploaded initial local notes to Drive appDataFolder")
                }
                _syncStatus.value = SyncStatus.Success(com.tatl.fastnote.R.string.str_sync_success, 0, System.currentTimeMillis())
                scheduleResetSyncStatus()
                return@withContext true
            }

            // 3. Tải nội dung từ Drive
            val driveRaw = downloadDriveContent(token, fileId) ?: ""
            val driveEntries = parseRawTextToBlocks(driveRaw)
            val driveHeaderMap = driveEntries.associateBy { it.header }

            // 4. Tìm các bản ghi lệch và bản ghi đã bị chỉnh sửa nội dung
            val missingOnLocal = driveEntries.filter { it.header !in localHeaderMap }
            val missingOnDrive = localEntries.filter { it.header !in driveHeaderMap }

            // Kiểm tra các bản ghi cùng header nhưng nội dung đã bị sửa đổi trên máy
            val contentModifiedEntries = localEntries.filter { localEntry ->
                val driveEntry = driveHeaderMap[localEntry.header]
                driveEntry != null && driveEntry.content.trim() != localEntry.content.trim()
            }

            val hasChanges = missingOnLocal.isNotEmpty() ||
                    missingOnDrive.isNotEmpty() ||
                    contentModifiedEntries.isNotEmpty() ||
                    (localRaw.trim() != driveRaw.trim())

            if (!hasChanges) {
                Log.d(TAG, "Sync complete: Both local and Drive are already up-to-date and identical")
                _syncStatus.value = SyncStatus.Success(com.tatl.fastnote.R.string.str_sync_up_to_date, 0, System.currentTimeMillis())
                scheduleResetSyncStatus()
                return@withContext true
            }

            // 5. Lọc missingOnLocal qua blacklist — header đã xóa cố ý không được restore
            cleanupBlacklist(context)
            val deletedBlacklist = getBlacklist(context)
            val trulyMissingOnLocal = missingOnLocal.filter { it.header !in deletedBlacklist }

            // 6. Hợp nhất: Drive (genuine) + Local (wins on conflict)
            val mergedMap = LinkedHashMap<String, FileHelper.NoteEntry>()
            for (entry in trulyMissingOnLocal) {
                val key = entry.header.ifBlank { entry.content }
                if (key.isNotBlank()) mergedMap[key] = entry
            }
            for (entry in localEntries) {
                val key = entry.header.ifBlank { entry.content }
                if (key.isNotBlank()) mergedMap[key] = entry
            }

            // Sắp xếp theo thời gian tăng dần (cũ nhất ở trên, mới nhất ở dưới)
            val sortedEntries = mergedMap.values.sortedWith(
                compareBy { parseTimestampFromHeader(it.header) }
            )

            val mergedText = sortedEntries.joinToString("\n\n") { entry ->
                if (entry.header.isNotBlank()) "- ${entry.header}: ${entry.content}" else entry.content
            }

            // 7. Ghi đè local nếu có entry mới thật sự từ Drive (không nằm trong blacklist)
            if (trulyMissingOnLocal.isNotEmpty()) {
                FileHelper.getRawFile(context).writeText(mergedText, Charsets.UTF_8)
                FileHelper.getGuidiFile(context).writeText(mergedText, Charsets.UTF_8)

                val app = context.applicationContext as? AutoNoteApplication
                if (app != null) {
                    for (entry in trulyMissingOnLocal) {
                        val title = entry.content.split(" ").take(10).joinToString(" ")
                            .let { if (it.length > 50) it.take(50) + "..." else it }
                        app.noteRepository.insertNote(title = title, content = entry.content)
                    }
                }
                Log.d(TAG, "Restored ${trulyMissingOnLocal.size} entries from Drive (skipped ${missingOnLocal.size - trulyMissingOnLocal.size} blacklisted)")
            }

            // 8. Cập nhật Drive nếu local có bản ghi mới, sửa đổi, hoặc file Drive khác
            if (missingOnDrive.isNotEmpty() || contentModifiedEntries.isNotEmpty() || driveRaw.trim() != mergedText.trim()) {
                updateDriveFile(token, fileId, mergedText)
                Log.d(TAG, "Uploaded merged/updated notes to Drive (modified=${contentModifiedEntries.size}, missingOnDrive=${missingOnDrive.size})")
            }

            val successResId = if (trulyMissingOnLocal.isNotEmpty()) com.tatl.fastnote.R.string.str_sync_restored else com.tatl.fastnote.R.string.str_sync_success
            _syncStatus.value = SyncStatus.Success(successResId, trulyMissingOnLocal.size, System.currentTimeMillis())
            scheduleResetSyncStatus()
            true
        } catch (e: Exception) {
            Log.e(TAG, "sync error", e)
            _syncStatus.value = SyncStatus.Error(com.tatl.fastnote.R.string.str_sync_failed, e.message)
            scheduleResetSyncStatus()
            false
        }
    }

    private fun scheduleResetSyncStatus() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(3500L)
            if (_syncStatus.value !is SyncStatus.Syncing) {
                _syncStatus.value = SyncStatus.Idle
            }
        }
    }
}

package com.tatl.fastnote.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.tatl.fastnote.AutoNoteApplication
import com.tatl.fastnote.util.FileHelper
import kotlinx.coroutines.Dispatchers
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

    private const val DRIVE_FILE_NAME = "ghichu.txt"
    private const val DRIVE_API_FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"

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
                if (!response.isSuccessful) {
                    Log.w(TAG, "findAppDataFileId error: ${response.code} ${response.message}")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
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
                if (response.isSuccessful) {
                    val resBody = response.body?.string() ?: return@withContext null
                    val json = JSONObject(resBody)
                    val newId = json.optString("id")
                    Log.d(TAG, "Created new ghichu.txt on Drive with id: $newId")
                    return@withContext newId
                } else {
                    Log.w(TAG, "createDriveFile failed: ${response.code}")
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
                    Log.w(TAG, "updateDriveFile failed: ${response.code}")
                }
                ok
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateDriveFile exception", e)
            false
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

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("- Thứ") || trimmed.startsWith("- Chủ") || trimmed.startsWith("- ")) {
                if (currentHeader != null) {
                    entries.add(
                        FileHelper.NoteEntry(
                            header = currentHeader,
                            content = currentContent.toString().trim(),
                            fullLine = "- $currentHeader: ${currentContent.toString().trim()}"
                        )
                    )
                    currentContent.clear()
                }
                val colonIdx = trimmed.indexOf(":")
                if (colonIdx != -1) {
                    currentHeader = trimmed.substring(2, colonIdx).trim()
                    currentContent.append(trimmed.substring(colonIdx + 1).trim())
                } else {
                    currentHeader = trimmed.removePrefix("-").trim()
                }
            } else if (trimmed.isNotEmpty() && currentHeader != null) {
                if (currentContent.isNotEmpty()) currentContent.append("\n")
                currentContent.append(trimmed)
            }
        }

        if (currentHeader != null) {
            entries.add(
                FileHelper.NoteEntry(
                    header = currentHeader,
                    content = currentContent.toString().trim(),
                    fullLine = "- $currentHeader: ${currentContent.toString().trim()}"
                )
            )
        }

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
     *  4. Cập nhật lại cả 2 phía đồng nhất
     */
    suspend fun sync(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken(context)
            if (token == null) {
                Log.d(TAG, "No Google access token (user not logged in with Google)")
                return@withContext false
            }

            // 1. Đọc dữ liệu local
            val localRaw = FileHelper.readGuidiFile(context) ?: ""
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
                return@withContext true
            }

            // 3. Tải nội dung từ Drive
            val driveRaw = downloadDriveContent(token, fileId) ?: ""
            val driveEntries = parseRawTextToBlocks(driveRaw)
            val driveHeaderMap = driveEntries.associateBy { it.header }

            // 4. Tìm các bản ghi lệch
            val missingOnLocal = driveEntries.filter { it.header !in localHeaderMap }
            val missingOnDrive = localEntries.filter { it.header !in driveHeaderMap }

            if (missingOnLocal.isEmpty() && missingOnDrive.isEmpty()) {
                Log.d(TAG, "Sync complete: Both local and Drive are already up-to-date")
                return@withContext true
            }

            // 5. Hợp nhất danh sách tất cả các ghi chú (Loại bỏ trùng lặp theo header)
            val mergedEntries = mutableListOf<FileHelper.NoteEntry>()
            val seenHeaders = mutableSetOf<String>()

            // Ưu tiên thứ tự mới nhất (local trước, rồi drive)
            for (entry in localEntries + driveEntries) {
                if (entry.header !in seenHeaders) {
                    seenHeaders.add(entry.header)
                    mergedEntries.add(entry)
                }
            }

            // Xây dựng lại văn bản phẳng chuẩn
            val mergedText = mergedEntries.joinToString("\n\n") { entry ->
                "- ${entry.header}: ${entry.content}"
            }

            // 6. Ghi đè file local nếu có bản ghi mới từ Drive
            if (missingOnLocal.isNotEmpty()) {
                val guidiFile = FileHelper.getGuidiFile(context)
                guidiFile.writeText(mergedText, Charsets.UTF_8)

                // Cập nhật Room DB
                val app = context.applicationContext as? AutoNoteApplication
                if (app != null) {
                    for (entry in missingOnLocal) {
                        val title = entry.content.split(" ").take(10).joinToString(" ")
                            .let { if (it.length > 50) it.take(50) + "..." else it }
                        app.noteRepository.insertNote(title = title, content = entry.content)
                    }
                }
                Log.d(TAG, "Merged ${missingOnLocal.size} new entries from Drive into Local")
            }

            // 7. Cập nhật Drive nếu local có bản ghi mới
            if (missingOnDrive.isNotEmpty() || missingOnLocal.isNotEmpty()) {
                updateDriveFile(token, fileId, mergedText)
                Log.d(TAG, "Uploaded merged notes to Drive appDataFolder")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "sync error", e)
            false
        }
    }
}

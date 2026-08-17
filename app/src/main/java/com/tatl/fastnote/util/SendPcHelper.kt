package com.tatl.fastnote.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File

/**
 * Helper for "Gửi PC" feature.
 * - Zips ghichu.txt with AES-256 password encryption (Zip4j)
 * - Launches Android system share sheet
 *
 * Target: < 100ms for typical note file sizes.
 */
object SendPcHelper {

    private const val TAG       = "SendPcHelper"
    private const val ZIP_NAME  = "ghichu_baomat.zip"
    private const val AUTHORITY = "com.tatl.fastnote.fileprovider"

    /**
     * Creates an AES-256 encrypted zip from fileguidi.txt and opens the
     * system share sheet.  Must be called from a coroutine (uses IO dispatcher).
     *
     * @param password  Alphanumeric-only, min 6 chars — validated by caller.
     * @return          null on success, error message on failure.
     */
    suspend fun zipAndShare(context: Context, password: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val sourceFile = FileHelper.getGuidiFile(context)
                if (!sourceFile.exists() || sourceFile.length() == 0L) {
                    return@withContext "Không có dữ liệu ghi chú để gửi."
                }

                // Output zip in app cache dir (auto-cleaned)
                val outDir = File(context.cacheDir, "send_pc")
                outDir.mkdirs()
                val zipFile = File(outDir, ZIP_NAME)
                if (zipFile.exists()) zipFile.delete()

                // ── Zip + AES-256 (< 100ms for typical text files) ────────────
                val params = ZipParameters().apply {
                    compressionMethod  = CompressionMethod.DEFLATE
                    compressionLevel   = CompressionLevel.FASTEST   // speed priority
                    isEncryptFiles     = true
                    encryptionMethod   = EncryptionMethod.AES
                    aesKeyStrength     = AesKeyStrength.KEY_STRENGTH_256
                }

                ZipFile(zipFile, password.toCharArray()).use { zip ->
                    zip.addFile(sourceFile, params)
                }

                Log.d(TAG, "Zip created: ${zipFile.absolutePath} (${zipFile.length()} bytes)")

                // ── Share sheet ───────────────────────────────────────────────
                val uri = FileProvider.getUriForFile(context, AUTHORITY, zipFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type     = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Ghi chú bảo mật — giải nén bằng mật khẩu bạn đã nhập"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(
                    shareIntent,
                    "Gửi file qua Zalo / WhatsApp / Telegram..."
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                null   // success

            } catch (e: Exception) {
                Log.e(TAG, "zipAndShare failed", e)
                "Lỗi nén/gửi file: ${e.message?.take(80)}"
            }
        }
}

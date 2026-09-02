package com.tatl.fastnote.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.tatl.fastnote.auth.AuthManager
import com.tatl.fastnote.billing.PremiumManager
import java.util.concurrent.TimeUnit

/**
 * WorkManager Background Sync Worker for Google Drive (Bản Đặc Tả V38 - Phần 7).
 *
 * Tính năng & Cơ chế:
 *  - Ràng buộc (Constraints): NetworkType.CONNECTED (Chỉ chạy khi có mạng)
 *  - Tự động thử lại (Retry / Backoff Policy): Mất mạng hoặc lỗi kết nối -> Tự động thử lại khi có mạng
 *  - Tĩnh lặng 100%: Tự nuốt lỗi vào trong, không hiển thị bất kỳ thông báo hay vòng xoay loading nào
 *  - Chỉ chạy khi người dùng ĐÃ MUA Premium
 */
class GoogleDriveSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "GoogleDriveSyncWorker"
        const val WORK_NAME_ONE_TIME = "google_drive_sync_one_time"
        const val WORK_NAME_PERIODIC = "google_drive_sync_periodic"

        /**
         * Kích hoạt đồng bộ ngầm 1 lần qua WorkManager (khi mở app hoặc có thay đổi)
         */
        fun enqueueOneTimeSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncWork = OneTimeWorkRequestBuilder<GoogleDriveSyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME_ONE_TIME,
                    ExistingWorkPolicy.REPLACE,
                    syncWork
                )
                Log.d(TAG, "Enqueued OneTime Google Drive Sync Work")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enqueue one-time sync work: ${e.message}")
            }
        }

        /**
         * Thiết lập đồng bộ ngầm định kỳ (kể cả khi app đã tắt)
         */
        fun schedulePeriodicSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val periodicWork = PeriodicWorkRequestBuilder<GoogleDriveSyncWorker>(
                    30, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWork
                )
                Log.d(TAG, "Scheduled Periodic Google Drive Sync Work (every 30 mins)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to schedule periodic sync work: ${e.message}")
            }
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "GoogleDriveSyncWorker started")
        return try {
            // 1. Kiểm tra tài khoản
            val isGoogleUser = AuthManager.isLoggedIn() && !AuthManager.isAnonymous
            if (!isGoogleUser) {
                Log.d(TAG, "User not logged in with Google — skipping sync")
                return Result.success()
            }

            // 2. Kiểm tra bản quyền Premium
            val isPremium = PremiumManager.isPremium(applicationContext)
            if (!isPremium) {
                Log.d(TAG, "User is not Premium — skipping Google Drive sync")
                return Result.success()
            }

            // 3. Thực hiện đồng bộ ngầm
            val success = GoogleDriveSyncManager.sync(applicationContext)
            if (success) {
                Log.d(TAG, "GoogleDriveSyncWorker sync completed successfully")
                Result.success()
            } else {
                Log.w(TAG, "GoogleDriveSyncWorker sync returned false — will retry later")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "GoogleDriveSyncWorker failed with exception — will retry later", e)
            Result.retry()
        }
    }
}

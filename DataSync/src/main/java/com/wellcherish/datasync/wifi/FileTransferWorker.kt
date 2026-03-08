package com.wellcherish.datasync.wifi

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.wellcherish.base.log.ZLog
import com.wellcherish.base.notification.DATA_TRANS_CHANNEL_ID
import com.wellcherish.base.utils.stringRes
import com.wellcherish.base.R
import com.wellcherish.base.notification.DATA_TRANS_NOTIFICATION_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager 中执行的任务
 * */
class FileTransferWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        sendForegroundNotification()
        return withContext(Dispatchers.IO) {
            try {
                // todo 迁移文件数据
                Result.success() // 任务完成
            } catch (e: Exception) {
                ZLog.e(TAG, e)
                Result.failure()
                // Result.retry()   // 如果需要重试
            }
        }
    }

    private suspend fun sendForegroundNotification() {
        // 1. 构建通知
        val notification = NotificationCompat.Builder(applicationContext, DATA_TRANS_CHANNEL_ID)
            .setContentTitle(R.string.data_in_trans.stringRes)
            .setSmallIcon(R.drawable.ic_syncing)
            .setOngoing(true) // 设置为常驻通知
            .build()

        // 2. 将 Worker 提升为前台服务，这里的 1001 是唯一的通知 ID
        setForeground(ForegroundInfo(DATA_TRANS_NOTIFICATION_ID, notification))
    }

    companion object {
        private const val TAG = "FileTransferWorker"
    }
}
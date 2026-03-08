package com.wellcherish.base

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.wellcherish.base.notification.DATA_TRANS_CHANNEL_ID
import com.wellcherish.base.utils.stringRes

class MainApplication : Application() {
    companion object {
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()

        context = applicationContext

        // 在应用启动时立即注册通知渠道
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(
            DATA_TRANS_CHANNEL_ID,
            R.string.data_trans_channel_name.stringRes,
            importance
        ).apply {
            description = R.string.data_trans_channel_desc.stringRes
        }

        // 获取系统服务并注册
        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.createNotificationChannel(channel)
    }
}
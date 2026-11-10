package com.nov.memocard.liveupdate

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat

object PromotedNotificationSupport {
    const val CHANNEL_ID = "study_live_update_v2"
    const val CHANNEL_NAME = "背诵进度"
    const val LEGACY_CHANNEL_ID = "study_live_update_v1"
    private const val TAG = "LiveUpdate"

    private var lastResult = "尚未请求"

    fun sdkSupported(): Boolean = Build.VERSION.SDK_INT >= 36

    fun notificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun canPost(context: Context): Boolean {
        if (!sdkSupported()) return false
        return try {
            NotificationManagerCompat.from(context).canPostPromotedNotifications()
        } catch (t: Throwable) {
            false
        }
    }

    fun channelImportance(context: Context): Int =
        try {
            context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CHANNEL_ID)?.importance ?: -1
        } catch (t: Throwable) {
            -1
        }

    fun record(result: String) {
        lastResult = result
        Log.i(TAG, result)
    }

    fun diagnose(context: Context): String =
        "SDK=${Build.VERSION.SDK_INT}\n" +
            "通知已开启=${notificationsEnabled(context)}\n" +
            "canPostPromoted=${canPost(context)}\n" +
            "channel importance=${channelImportance(context)}\n" +
            "最近一次：$lastResult"
}
package com.nov.memocard.liveupdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.nov.memocard.MainActivity
import com.nov.memocard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LiveUpdateSpec(
    val title: String,
    val text: String,
    val shortText: String,
    val reviewedCount: Int,
    val totalDue: Int,
    val groupId: Long,
)

/** Reusable promoted-ongoing / Live Updates template (reader / video / study share it). */
class LiveUpdateNotifier(
    context: Context,
    private val channelId: String,
    private val notificationId: Int,
) {
    private val appContext = context.applicationContext
    private val nm = appContext.getSystemService(NotificationManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastPostAt = 0L
    private var lastSpec: LiveUpdateSpec? = null
    private var autoJob: Job? = null

    init {
        ensureChannel()
    }

    private fun ensureChannel() {
        try {
            nm.deleteNotificationChannel(PromotedNotificationSupport.LEGACY_CHANNEL_ID)
        } catch (_: Throwable) {
        }
        if (nm.getNotificationChannel(channelId) != null) return
        nm.createNotificationChannel(
            NotificationChannel(channelId, PromotedNotificationSupport.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    fun show(spec: LiveUpdateSpec) {
        lastSpec = spec
        lastPostAt = SystemClock.elapsedRealtime()
        autoJob?.cancel()
        post(spec)
    }

    fun showThrottled(spec: LiveUpdateSpec, intervalMs: Long = 1000L) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - lastPostAt
        if (elapsed >= intervalMs) {
            show(spec)
        } else {
            lastSpec = spec
            scope.launch {
                delay(intervalMs - elapsed)
                lastSpec?.let { show(it) }
            }
        }
    }

    private fun post(spec: LiveUpdateSpec) {
        try {
            if (!PromotedNotificationSupport.notificationsEnabled(appContext)) {
                PromotedNotificationSupport.record("App 没请求 promoted（通知已被系统关闭）")
                return
            }
            ensureChannel()
            nm.notify(notificationId, build(spec))
        } catch (t: Throwable) {
            PromotedNotificationSupport.record("构建失败：${t.javaClass.simpleName}")
        }
    }

    private fun build(spec: LiveUpdateSpec): Notification {
        val pi = PendingIntent.getActivity(
            appContext,
            notificationId,
            Intent(appContext, MainActivity::class.java)
                .putExtra("groupId", spec.groupId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val total = spec.totalDue.coerceAtLeast(1)
        val reviewed = spec.reviewedCount.coerceIn(0, total)
        val percent = reviewed * 100 / total
        val progressText = "${spec.reviewedCount}/${spec.totalDue}"
        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_review)
            .setContentTitle(spec.title)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)

        if (Build.VERSION.SDK_INT >= 36) {
            val canPost = PromotedNotificationSupport.canPost(appContext)
            try {
                builder.setRequestPromotedOngoing(true)
                builder.setShortCriticalText(spec.shortText.take(7))
                builder.setStyle(
                    NotificationCompat.ProgressStyle()
                        .setProgressSegments(
                            listOf(NotificationCompat.ProgressStyle.Segment(total)),
                        )
                        .setProgress(reviewed),
                )
                PromotedNotificationSupport.record(
                    if (canPost) "请求已发出（promoted granted=$canPost）"
                    else "系统没放行 promoted（canPost=false，降级普通通知）",
                )
            } catch (t: Throwable) {
                builder.setProgress(100, percent, false)
                PromotedNotificationSupport.record("构建失败：${t.javaClass.simpleName}（降级普通通知）")
            }
        } else {
            builder.setProgress(100, percent, false)
            PromotedNotificationSupport.record("App 没请求 promoted（SDK<36，普通通知）")
        }
        return builder.build()
    }

    fun cancel() {
        autoJob?.cancel()
        lastSpec = null
        try {
            nm.cancel(notificationId)
        } catch (_: Throwable) {
        }
    }

    fun cancelAfter(ms: Long) {
        autoJob?.cancel()
        autoJob = scope.launch {
            delay(ms)
            cancel()
        }
    }
}
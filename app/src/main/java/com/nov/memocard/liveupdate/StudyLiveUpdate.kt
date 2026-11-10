package com.nov.memocard.liveupdate

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object StudyLiveUpdate {
    private lateinit var notifier: LiveUpdateNotifier
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun init(context: Context) {
        notifier = LiveUpdateNotifier(context, PromotedNotificationSupport.CHANNEL_ID, 3003)
    }

    fun onBackground() {
        val s = StudySession.state.value
        if (!s.active) return
        if (!LiveActivityCoordinator.acquire(LiveFeature.STUDY)) return
        publish(s, throttled = false)
    }

    fun onForeground() {
        notifier.cancel()
        LiveActivityCoordinator.release(LiveFeature.STUDY)
    }

    fun onProgress() {
        if (LiveActivityCoordinator.current() != LiveFeature.STUDY) return
        val s = StudySession.state.value
        if (s.active) publish(s, throttled = true)
    }

    private fun publish(s: StudySessionState, throttled: Boolean) {
        val question = s.currentCardLabel.ifBlank { s.groupName }
        val spec = LiveUpdateSpec(
            title = if (s.completed) "今日完成" else question,
            text = "",
            shortText = "${s.reviewedCount}/${s.totalDue}".take(7),
            reviewedCount = s.reviewedCount,
            totalDue = s.totalDue,
            groupId = s.groupId,
        )
        if (throttled) notifier.showThrottled(spec) else notifier.show(spec)
        if (s.completed) {
            scope.launch {
                delay(3000)
                notifier.cancel()
                StudySession.end()
                LiveActivityCoordinator.release(LiveFeature.STUDY)
            }
        }
    }
}
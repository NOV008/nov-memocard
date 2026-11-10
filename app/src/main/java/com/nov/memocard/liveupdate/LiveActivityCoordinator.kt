package com.nov.memocard.liveupdate

enum class LiveFeature(val priority: Int) {
    VIDEO(3), READER(2), STUDY(1)
}

object LiveActivityCoordinator {
    private var holder: LiveFeature? = null
    private val releaseHooks = mutableMapOf<LiveFeature, () -> Unit>()

    fun onRelease(feature: LiveFeature, hook: () -> Unit) {
        releaseHooks[feature] = hook
    }

    fun acquire(feature: LiveFeature): Boolean {
        val cur = holder
        if (cur != null && cur != feature && cur.priority > feature.priority) return false
        if (cur != null && cur != feature) releaseHooks[cur]?.invoke()
        holder = feature
        return true
    }

    fun release(feature: LiveFeature) {
        if (holder == feature) holder = null
    }

    fun current(): LiveFeature? = holder
}
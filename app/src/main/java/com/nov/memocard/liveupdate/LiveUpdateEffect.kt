package com.nov.memocard.liveupdate

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.activity.ComponentActivity

object LiveUpdateEffect {
    fun bind(activity: ComponentActivity, onBackground: () -> Unit, onForeground: () -> Unit) {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                if (!activity.isChangingConfigurations && !activity.isFinishing) onBackground()
            }

            override fun onStart(owner: LifecycleOwner) {
                onForeground()
            }

            override fun onResume(owner: LifecycleOwner) {
                onForeground()
            }
        })
    }
}
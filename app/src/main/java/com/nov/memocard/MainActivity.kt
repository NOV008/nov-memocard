package com.nov.memocard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.nov.memocard.liveupdate.LiveUpdateEffect
import com.nov.memocard.liveupdate.ReviewIntent
import com.nov.memocard.liveupdate.StudyLiveUpdate
import com.nov.memocard.ui.navigation.AppRoot
import com.nov.memocard.ui.theme.MemoCardTheme
import com.nov.memocard.ui.theme.responsiveFontScale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        handleDeckIntent(intent)
        LiveUpdateEffect.bind(
            this,
            onBackground = { StudyLiveUpdate.onBackground() },
            onForeground = { StudyLiveUpdate.onForeground() },
        )
        setContent {
            val app = application as MemoCardApp
            val cfg = LocalConfiguration.current
            val base = LocalDensity.current
            val auto = responsiveFontScale(cfg.screenWidthDp, cfg.screenHeightDp)
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, base.fontScale * auto),
            ) {
                MemoCardTheme(themeMode = app.settings.themeMode) {
                    AppRoot()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleDeckIntent(intent)
    }

    private fun handleDeckIntent(intent: android.content.Intent?) {
        val id = intent?.getLongExtra("groupId", 0L) ?: 0L
        if (id != 0L) {
            (application as MemoCardApp).settings.setReviewDeck(id)
            ReviewIntent.openDeck.tryEmit(id)
        }
    }
}
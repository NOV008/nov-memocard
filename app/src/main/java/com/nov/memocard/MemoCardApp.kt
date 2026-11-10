package com.nov.memocard

import android.app.Application
import androidx.room.Room
import com.nov.memocard.data.AppDatabase
import com.nov.memocard.data.MemoRepository
import com.nov.memocard.data.SettingsStore
import com.nov.memocard.util.CrashHandler
import com.nov.memocard.liveupdate.StudyLiveUpdate

class MemoCardApp : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "memocard.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()
    }
    val repository: MemoRepository by lazy {
        MemoRepository(
            database,
            { settings.requestRetention.toDouble() },
            { settings.newPerDay to settings.reviewsPerDay },
        )
    }
    val settings: SettingsStore by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        StudyLiveUpdate.init(this)
    }
}
package com.nov.memocard.util

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import java.io.File
import kotlin.concurrent.thread

class CrashHandler(private val app: Application) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            val stack = Log.getStackTraceString(e)
            File(app.filesDir, "crash.txt").writeText(stack)
            val intent = Intent(app, CrashActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra("stack", stack)
            app.startActivity(intent)
        } catch (_: Throwable) {
        }
        thread {
            try {
                Thread.sleep(1500)
            } catch (_: InterruptedException) {
            }
            Process.killProcess(Process.myPid())
        }
    }
}
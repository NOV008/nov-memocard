package com.nov.memocard.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stack = intent.getStringExtra("stack")
            ?: File(filesDir, "crash.txt").takeIf { it.exists() }?.readText()
            ?: "no stack"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 24)
        }
        val title = TextView(this).apply {
            text = "崩溃堆栈（复制发给开发者）"
            textSize = 18f
        }
        val copy = Button(this).apply {
            text = "复制堆栈"
            setOnClickListener {
                val cm = getSystemService(ClipboardManager::class.java)
                cm.setPrimaryClip(ClipData.newPlainText("crash", stack))
            }
        }
        val tv = TextView(this).apply {
            text = stack
            textSize = 11f
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply { addView(tv) }
        root.addView(title)
        root.addView(copy)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }
}
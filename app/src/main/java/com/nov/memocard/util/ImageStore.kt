package com.nov.memocard.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

object ImageStore {
    private fun dir(context: Context): File = File(context.filesDir, "cards").apply { mkdirs() }

    fun save(context: Context, uri: Uri): String? = try {
        val name = "img_${UUID.randomUUID()}.jpg"
        context.contentResolver.openInputStream(uri)?.use { input ->
            File(dir(context), name).outputStream().use { input.copyTo(it) }
        }
        name
    } catch (t: Throwable) {
        null
    }

    fun loadBitmap(context: Context, name: String): Bitmap? =
        runCatching { BitmapFactory.decodeFile(File(dir(context), name).absolutePath) }.getOrNull()

    fun delete(context: Context, name: String?) {
        if (name.isNullOrBlank()) return
        runCatching { File(dir(context), name).delete() }
    }
}
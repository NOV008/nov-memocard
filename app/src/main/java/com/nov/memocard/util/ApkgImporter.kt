package com.nov.memocard.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.text.Html
import com.nov.memocard.data.MemoRepository
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApkgImporter {
    /** Imports text of Anki notes as front/back cards. Media is skipped; anki21b (zstd) is rejected. */
    suspend fun import(
        context: Context,
        uri: Uri,
        repo: MemoRepository,
        defaultName: String,
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "apkg_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            val dbFile = unzipCollection(context, uri, tmp)
                ?: throw IllegalStateException("未找到 collection.anki2/anki21（anki21b 压缩格式暂不支持，请用 Anki 导出兼容格式）")
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val deckId = repo.addDeck(defaultName)
            var n = 0
            db.rawQuery("SELECT flds FROM notes", null).use { c ->
                while (c.moveToNext()) {
                    val flds = c.getString(0) ?: continue
                    val parts = flds.split('\u001F')
                    val f = Html.fromHtml(parts.getOrElse(0) { "" }, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    val b = Html.fromHtml(parts.getOrElse(1) { "" }, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    if (f.isNotEmpty() && b.isNotEmpty()) {
                        repo.addCard(deckId, f, b)
                        n++
                    }
                }
            }
            db.close()
            n to defaultName
        } finally {
            tmp.deleteRecursively()
        }
    }

    private fun unzipCollection(context: Context, uri: Uri, tmp: File): File? {
        val names = setOf("collection.anki21", "collection.anki2")
        val found = mutableMapOf<String, File>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name in names) {
                        val f = File(tmp, entry.name)
                        f.outputStream().use { zis.copyTo(it) }
                        found[entry.name] = f
                    }
                    entry = zis.nextEntry
                }
            }
        }
        return found["collection.anki21"] ?: found["collection.anki2"]
    }
}
package com.nov.memocard.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nov.memocard.MemoCardApp
import com.nov.memocard.R
import com.nov.memocard.ui.components.NavOverflowMenu
import com.nov.memocard.ui.components.Segmented
import com.nov.memocard.ui.navigation.Routes
import com.nov.memocard.util.ApkgImporter
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private const val IMPORT_PROMPT = """请把以下学习材料整理成记忆卡 JSON，仅输出 JSON，不要任何解释或代码块标记：
{"name":"牌组名","cards":[{"front":"问题或术语","back":"答案或要点"},...]}
要求：
1. front 为简洁的提问或术语，back 为完整准确的答案；
2. 过长答案按要点拆成多张卡，每张卡只考一个点；
3. 保留原文关键表述与编号，不要编造材料中没有的内容；
4. 纯配图/无文字答案的条目跳过。
材料：
<在此粘贴学习材料>"""

private val Blue = Color(0xFF2F7CF6)
private val Amber = Color(0xFFF5A623)
private val Green = Color(0xFF30A46C)
private val Violet = Color(0xFF7C6CF0)
private val TealAccent = Color(0xFF26A69A)

@Composable
fun MineScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MemoCardApp
    val settings = app.settings
    val repo = app.repository
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showFont by remember { mutableStateOf(false) }
    var editingLimit by remember { mutableStateOf<String?>(null) }
    var showRetention by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    if (uri.toString().lowercase().contains(".apkg")) {
                        val name = "apkg导入"
                        val (n, importedName) = ApkgImporter.import(context, uri, repo, name)
                        Toast.makeText(context, "已从 apkg 导入 $n 张到「$importedName」", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@launch
                    val trimmed = text.trim()
                    val name: String
                    val pairs: List<Pair<String, String>>
                    if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                        val arr: JSONArray
                        if (trimmed.startsWith("{")) {
                            val obj = JSONObject(trimmed)
                            name = obj.optString("name", "导入牌组")
                            arr = obj.getJSONArray("cards")
                        } else {
                            name = "导入牌组"
                            arr = JSONArray(trimmed)
                        }
                        pairs = (0 until arr.length()).mapNotNull { i ->
                            val o = arr.getJSONObject(i)
                            val f = o.optString("front")
                            val b = o.optString("back")
                            if (f.isNotBlank() && b.isNotBlank()) f to b else null
                        }
                    } else {
                        name = "导入牌组"
                        pairs = trimmed.lineSequence().mapNotNull { line ->
                            val idx = line.indexOf(',')
                            if (idx <= 0) null
                            else line.substring(0, idx).trim().let { f ->
                                val b = line.substring(idx + 1).trim()
                                if (f.isNotEmpty() && b.isNotEmpty()) f to b else null
                            }
                        }.toList()
                    }
                    val deckId = repo.addDeck(name)
                    pairs.forEach { repo.addCard(deckId, it.first, it.second) }
                    Toast.makeText(context, "已导入 ${pairs.size} 张到「$name」", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导入失败：需 apkg/JSON/CSV", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val txt = repo.exportJson()
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(txt) }
                    Toast.makeText(context, "备份已导出", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardColor = if (dark) Color(0xFF1A1A1C) else Color.White
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("我的", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            NavOverflowMenu(current = Routes.MINE, onNavigate = onNavigate)
        }

        SectionLabel("通用设置")
        SettingCard(cardColor) {
            ControlRow(R.drawable.ic_theme, "外观主题", Indigo) {
                Segmented(listOf("自动", "日间", "夜间"), settings.themeMode) { settings.updateThemeMode(it) }
            }
            CardDivider(dividerColor)
            ControlRow(R.drawable.ic_vibrate, "振动效果", Amber) {
                Segmented(listOf("禁用", "启用"), if (settings.vibration) 1 else 0) { settings.updateVibration(it == 1) }
            }
            CardDivider(dividerColor)
            ActionRow(R.drawable.ic_font, "字体设置", Violet, trailingText = null) { showFont = true }
        }

        SectionLabel("学习")
        SettingCard(cardColor) {
            ActionRow(
                R.drawable.ic_target, "期望保留率", Green,
                trailingText = "${(settings.requestRetention * 100).toInt()}%",
            ) { showRetention = true }
            CardDivider(dividerColor)
            ActionRow(
                R.drawable.ic_newcard, "每日新卡上限", Blue,
                trailingText = if (settings.newPerDay <= 0) "不限" else "${settings.newPerDay}",
            ) { editingLimit = "new" }
            CardDivider(dividerColor)
            ActionRow(
                R.drawable.ic_repeat, "每日复习上限", Amber,
                trailingText = if (settings.reviewsPerDay <= 0) "不限" else "${settings.reviewsPerDay}",
            ) { editingLimit = "review" }
        }

        SectionLabel("数据")
        SettingCard(cardColor) {
            ActionRow(R.drawable.ic_import, "导入牌组（apkg / JSON / CSV）", TealAccent, null) {
                importLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "application/zip", "application/octet-stream", "text/*", "*/*"))
            }
            CardDivider(dividerColor)
            ActionRow(R.drawable.ic_export, "导出备份（JSON）", TealAccent, null) {
                exportLauncher.launch("Nov记忆卡-备份-${System.currentTimeMillis()}.json")
            }
            CardDivider(dividerColor)
            ActionRow(R.drawable.ic_copy, "复制导入提示词", Violet, null) {
                clipboard.setText(AnnotatedString(IMPORT_PROMPT))
                Toast.makeText(context, "提示词已复制，发给 AI 生成卡片 JSON", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showFont) {
        AlertDialog(
            onDismissRequest = { showFont = false },
            title = { Text("字体设置") },
            text = {
                Column {
                    Text("卡片字号缩放：${String.format("%.2f", settings.fontScale)}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Slider(value = settings.fontScale, onValueChange = { settings.updateFontScale(it) }, valueRange = 0.8f..1.4f)
                    Text("饱和空气", fontSize = 24.sp * settings.fontScale, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = { TextButton(onClick = { showFont = false }) { Text("完成") } },
        )
    }

    if (showRetention) {
        RetentionDialog(
            initial = settings.requestRetention,
            onDismiss = { showRetention = false },
            onSave = { settings.updateRequestRetention(it); showRetention = false },
        )
    }

    editingLimit?.let { kind ->
        val isNew = kind == "new"
        LimitDialog(
            title = if (isNew) "每日新卡上限" else "每日复习上限",
            initial = if (isNew) settings.newPerDay else settings.reviewsPerDay,
            presets = if (isNew) listOf(0, 10, 20, 50) else listOf(0, 100, 300, 500),
            onDismiss = { editingLimit = null },
            onSave = { v ->
                if (isNew) settings.updateNewPerDay(v) else settings.updateReviewsPerDay(v)
                editingLimit = null
            },
        )
    }
}

private val Indigo = Color(0xFF4F5BD5)

@Composable
private fun NiceDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String = "保存",
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (dark) Color(0xFF1F1F22) else Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.widthIn(min = 320.dp, max = 420.dp).padding(24.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                content()
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onConfirm, shape = RoundedCornerShape(50)) { Text(confirmText) }
                }
            }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LimitDialog(
    title: String,
    initial: Int,
    presets: List<Int>,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var v by remember { mutableStateOf(initial) }
    NiceDialog(title = title, onDismiss = onDismiss, onConfirm = { onSave(v) }) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton("−") { v = (v - 10).coerceAtLeast(0) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (v == 0) "不限" else "$v",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("张 / 天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StepButton("+") { v += 10 }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { p ->
                    val sel = v == p
                    Box(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { v = p }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            if (p == 0) "不限" else "$p",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("0 表示不限；对所有牌组生效", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RetentionDialog(initial: Float, onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var v by remember { mutableStateOf(initial) }
    NiceDialog(title = "期望保留率", onDismiss = onDismiss, onConfirm = { onSave(v) }) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(v * 100).toInt()}%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Slider(
                value = v,
                onValueChange = { v = it },
                valueRange = 0.80f..0.97f,
                steps = 16,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.85f, 0.90f, 0.92f, 0.95f).forEach { p ->
                    val sel = kotlin.math.abs(v - p) < 0.005f
                    Box(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { v = p }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "${(p * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("越高越稳，但复习次数更多", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 32.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingCard(cardColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(cardColor),
        content = content,
    )
}

@Composable
private fun CardDivider(color: Color) {
    Divider(color = color, modifier = Modifier.padding(start = 56.dp))
}

@Composable
private fun RowIcon(icon: Int, tint: Color) {
    Image(
        painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint),
    )
}

@Composable
private fun ControlRow(icon: Int, label: String, tint: Color, control: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon, tint)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        control()
    }
}

@Composable
private fun ActionRow(icon: Int, label: String, tint: Color, trailingText: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon, tint)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (trailingText != null) {
            Text(
                trailingText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
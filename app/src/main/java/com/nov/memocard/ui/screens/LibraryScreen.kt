@file:OptIn(ExperimentalMaterial3Api::class)

package com.nov.memocard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nov.memocard.data.Deck
import com.nov.memocard.ui.DeckCounts
import com.nov.memocard.ui.DeckListViewModel
import com.nov.memocard.ui.components.ConfirmDialog
import com.nov.memocard.ui.components.EmptyState
import com.nov.memocard.ui.components.NameDialog
import com.nov.memocard.ui.components.NavOverflowMenu
import com.nov.memocard.ui.navigation.Routes
import kotlin.math.ceil

@Composable
fun LibraryScreen(
    onOpenDeck: (Long) -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: DeckListViewModel = viewModel(),
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val now = remember { System.currentTimeMillis() }
    var search by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var topMenu by remember { mutableStateOf(false) }
    val filtered = decks.filter { it.name.contains(search.trim(), ignoreCase = true) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()) {
                            if (search.isEmpty()) {
                                Text("发现牌组", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            }
                            inner()
                        }
                    },
                )
            }
            NavOverflowMenu(current = Routes.LIBRARY, onNavigate = onNavigate)
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("默认分组", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建牌组")
            }
        }

        if (filtered.isEmpty()) {
            EmptyState(Modifier.fillMaxSize(), "没有牌组", "点击 + 新建牌组")
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered, key = { it.id }) { deck ->
                    val counts by viewModel.counts(deck.id, now).collectAsStateWithLifecycle(initialValue = DeckCounts())
                    LibraryRow(
                        deck = deck,
                        counts = counts,
                        onClick = { onOpenDeck(deck.id) },
                        onDelete = { viewModel.deleteDeck(deck) },
                        onRename = { viewModel.renameDeck(deck, it) },
                    )
                }
            }
        }
    }

    if (showAdd) {
        NameDialog("新建牌组", onDismiss = { showAdd = false }, onConfirm = { viewModel.addDeck(it); showAdd = false })
    }
}

@Composable
private fun LibraryRow(
    deck: Deck,
    counts: DeckCounts,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val learned = counts.total - counts.new
    val fraction = if (counts.total > 0) (learned.toFloat() / counts.total).coerceIn(0f, 1f) else 0f
    val days = ceil(counts.new / 20.0).toInt()
    val fill = MaterialTheme.colorScheme.primaryContainer

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .drawBehind { drawRect(fill, size = Size(size.width * fraction, size.height)) }
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("我的", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "进度 $learned/${counts.total}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "预计还需 $days 天完成",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (counts.due > 0) {
                Box(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("${counts.due}", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(4.dp))
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("重命名") }, onClick = { menu = false; showRename = true })
                    DropdownMenuItem(text = { Text("删除") }, onClick = { menu = false; showDelete = true })
                }
            }
        }
    }

    if (showRename) {
        NameDialog("重命名牌组", deck.name, { showRename = false }, { onRename(it); showRename = false })
    }
    if (showDelete) {
        ConfirmDialog("删除牌组", "将删除「${deck.name}」及其所有卡片，无法恢复。", { showDelete = false }, { onDelete(); showDelete = false })
    }
}
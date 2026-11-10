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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nov.memocard.data.Card
import com.nov.memocard.ui.DeckCounts
import com.nov.memocard.ui.DeckDetailViewModel
import com.nov.memocard.ui.components.ConfirmDialog
import com.nov.memocard.ui.components.EmptyState
import com.nov.memocard.ui.components.NameDialog
import com.nov.memocard.ui.components.stateLabel
import com.nov.memocard.ui.components.stateTint

@Composable
fun DeckDetailScreen(
    onBack: () -> Unit,
    onReview: (Long) -> Unit,
    onAddCard: (Long) -> Unit,
    onEditCard: (Long, Long) -> Unit,
    viewModel: DeckDetailViewModel = viewModel(),
) {
    val deck by viewModel.deck.collectAsStateWithLifecycle()
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val now = remember { System.currentTimeMillis() }
    val counts by viewModel.counts(now).collectAsStateWithLifecycle(initialValue = DeckCounts())
    var menu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDeleteDeck by remember { mutableStateOf(false) }
    val pending = counts.due + counts.new

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(deck?.name ?: "牌组", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("重命名牌组") }, onClick = { menu = false; showRename = true })
                            DropdownMenuItem(text = { Text("删除牌组") }, onClick = { menu = false; showDeleteDeck = true })
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddCard(viewModel.deckId) },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(Icons.Default.Add, contentDescription = "新建卡片") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "到期 ${counts.due} · 新卡 ${counts.new} · 共 ${counts.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { onReview(viewModel.deckId) },
                    enabled = pending > 0,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (pending > 0) "开始复习（$pending）" else "暂无到期卡片", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (cards.isEmpty()) {
                EmptyState(Modifier.fillMaxSize(), "还没有卡片", "点击右下角 + 添加第一张卡片")
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(cards, key = { it.id }) { card ->
                        CardRow(card, onClick = { onEditCard(viewModel.deckId, card.id) }, onDelete = { viewModel.deleteCard(card) })
                    }
                }
            }
        }
    }

    if (showRename) {
        NameDialog("重命名牌组", deck?.name ?: "", { showRename = false }, { viewModel.renameDeck(it); showRename = false })
    }
    if (showDeleteDeck) {
        ConfirmDialog("删除牌组", "将删除「${deck?.name}」及其所有卡片，无法恢复。", { showDeleteDeck = false }, { viewModel.deleteDeck(onBack); showDeleteDeck = false })
    }
}

@Composable
private fun CardRow(card: Card, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    val tint = stateTint(card.state)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(card.front, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(4.dp)).background(tint.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(stateLabel(card.state), style = MaterialTheme.typography.labelSmall, color = tint)
                }
            }
            IconButton(onClick = { showDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = "删除卡片", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showDelete) {
        ConfirmDialog("删除卡片", "删除「${card.front.take(20)}」？", { showDelete = false }, { onDelete(); showDelete = false })
    }
}
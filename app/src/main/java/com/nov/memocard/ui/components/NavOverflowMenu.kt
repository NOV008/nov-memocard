package com.nov.memocard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.nov.memocard.ui.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class MenuAction(
    val label: String,
    val active: Boolean,
    val enabled: Boolean,
    val onClick: () -> Unit,
    val dividerAfter: Boolean = false,
    val showSwitch: Boolean = false,
    val switchOn: Boolean = false,
)

@Composable
fun NavOverflowMenu(
    current: String,
    onNavigate: (String) -> Unit,
    onUndo: (() -> Unit)? = null,
    undoEnabled: Boolean = true,
    handwriting: Boolean? = null,
    onToggleHandwriting: (() -> Unit)? = null,
    canClearInk: Boolean = false,
    onClearInk: (() -> Unit)? = null,
) {
    var popupVisible by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val actions = buildList {
        if (onUndo != null) {
            add(MenuAction("撤销", active = false, enabled = undoEnabled, onClick = onUndo, dividerAfter = true))
        }
        if (handwriting != null && onToggleHandwriting != null) {
            add(
                MenuAction(
                    "手写",
                    active = false,
                    enabled = true,
                    onClick = onToggleHandwriting,
                    dividerAfter = !(handwriting && onClearInk != null),
                    showSwitch = true,
                    switchOn = handwriting,
                ),
            )
        }
        if (handwriting == true && onClearInk != null) {
            add(MenuAction("清除手写", active = false, enabled = canClearInk, onClick = onClearInk, dividerAfter = true))
        }
        add(MenuAction("复习", Routes.REVIEW == current, true, { onNavigate(Routes.REVIEW) }))
        add(MenuAction("牌库", Routes.LIBRARY == current, true, { onNavigate(Routes.LIBRARY) }))
        add(MenuAction("我的", Routes.MINE == current, true, { onNavigate(Routes.MINE) }))
    }

    fun dismiss(after: () -> Unit = {}) {
        expanded = false
        scope.launch {
            delay(170)
            popupVisible = false
            after()
        }
    }

    Box {
        IconButton(onClick = {
            expanded = false
            popupVisible = true
        }) {
            Icon(Icons.Default.MoreVert, contentDescription = "导航")
        }

        if (popupVisible) {
            LaunchedEffect(Unit) { expanded = true }
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, with(density) { 48.dp.roundToPx() }),
                onDismissRequest = { dismiss() },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(180)) + scaleIn(
                        animationSpec = tween(180),
                        initialScale = 0.85f,
                        transformOrigin = TransformOrigin(1f, 0f),
                    ),
                    exit = fadeOut(tween(150)) + scaleOut(
                        animationSpec = tween(150),
                        targetScale = 0.92f,
                        transformOrigin = TransformOrigin(1f, 0f),
                    ),
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 10.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(Modifier.width(200.dp).padding(vertical = 6.dp)) {
                            actions.forEachIndexed { index, action ->
                                var itemShown by remember { mutableStateOf(false) }
                                LaunchedEffect(expanded, index) {
                                    if (expanded) {
                                        delay(index * 8L)
                                        itemShown = true
                                    } else {
                                        itemShown = false
                                    }
                                }
                                AnimatedVisibility(
                                    visible = itemShown,
                                    enter = fadeIn(tween(120)) + slideInVertically(tween(120)) { _ ->
                                        with(density) { 6.dp.roundToPx() }
                                    },
                                    exit = fadeOut(tween(80)),
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = action.enabled) {
                                                dismiss { action.onClick() }
                                            }
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            action.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = when {
                                                !action.enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                action.active -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = if (action.active) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                        if (action.showSwitch) {
                                            Spacer(Modifier.weight(1f))
                                            Switch(
                                                checked = action.switchOn,
                                                onCheckedChange = null,
                                                modifier = Modifier.scale(0.8f),
                                            )
                                        } else if (action.active) {
                                            Spacer(Modifier.weight(1f))
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                                if (action.dividerAfter) {
                                    Divider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
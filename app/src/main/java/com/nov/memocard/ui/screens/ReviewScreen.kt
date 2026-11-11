@file:OptIn(ExperimentalMaterial3Api::class)

package com.nov.memocard.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nov.memocard.MemoCardApp
import com.nov.memocard.fsrs.Rating
import com.nov.memocard.fsrs.SchedulingInfo
import com.nov.memocard.ui.ReviewViewModel
import com.nov.memocard.ui.components.NavOverflowMenu
import com.nov.memocard.ui.components.formatInterval
import com.nov.memocard.ui.navigation.Routes
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.nov.memocard.util.ImageStore

private val CardShape = RoundedCornerShape(20.dp)
private val AgainColor = Color(0xFFE5484D)
private val HardColor = Color(0xFFF5A623)
private val GoodColor = Color(0xFF4F5BD5)
private val EasyColor = Color(0xFF30A46C)

private fun liftCurve(p: Float): Float {
    if (p <= 0.5f) return sin(Math.PI.toFloat() * p)
    val t = (p - 0.5f) / 0.5f
    return (1 - t) * cos(t * 1.5f * Math.PI.toFloat())
}

private fun DrawScope.drawCardShadow(alpha: Float) {
    if (alpha <= 0.01f) return
    val corner = 20.dp.toPx()
    val steps = 12
    val maxBleed = 10.dp.toPx()
    val dy = 3.dp.toPx()
    for (i in 0 until steps) {
        val t = i / (steps - 1f)
        val bleed = maxBleed * (1f - t)
        drawRoundRect(
            color = Color.Black.copy(alpha = (0.010f + 0.008f * t) * alpha),
            topLeft = Offset(-bleed, -bleed + dy),
            size = Size(size.width + bleed * 2f, size.height + bleed * 2f),
            cornerRadius = CornerRadius(corner + bleed),
        )
    }
}

@Composable
fun ReviewScreen(
    onNavigate: (String) -> Unit,
    viewModel: ReviewViewModel = viewModel(),
) {
    val context = LocalContext.current
    val settings = (context.applicationContext as MemoCardApp).settings
    val haptic = LocalHapticFeedback.current

    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val index by viewModel.index.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val reviewed by viewModel.reviewedCount.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()

    val selectedId = settings.reviewDeckId.takeIf { it != 0L } ?: decks.firstOrNull()?.id ?: 0L
    LaunchedEffect(selectedId) { viewModel.load(selectedId) }
    val deckName = decks.firstOrNull { it.id == selectedId }?.name ?: "记忆卡"

    val current = queue.getOrNull(index)
    var revealed by remember(current?.id) { mutableStateOf(false) }
    var showDeckMenu by remember { mutableStateOf(false) }
    val now = remember { System.currentTimeMillis() }
    val preview = remember(current?.id) {
        if (current != null) viewModel.preview(current, now) else emptyMap()
    }
    val frontBmp = remember(current?.id, current?.frontImage) {
        current?.frontImage?.let { ImageStore.loadBitmap(context, it) }
    }
    val backBmp = remember(current?.id, current?.backImage) {
        current?.backImage?.let { ImageStore.loadBitmap(context, it) }
    }

    val handwriting = settings.handwriting
    val frontInk = remember(current?.id) { mutableStateListOf<List<Offset>>() }
    val backInk = remember(current?.id) { mutableStateListOf<List<Offset>>() }
    val activeInk = remember(current?.id) { mutableStateListOf<Offset>() }
    var activeInkFront by remember(current?.id) { mutableStateOf(true) }

    val progress = remember(current?.id) { Animatable(0f) }
    var flipBusy by remember(current?.id) { mutableStateOf(false) }
    LaunchedEffect(revealed, current?.id) {
        progress.animateTo(if (revealed) 1f else 0f, tween(320, easing = LinearEasing))
        flipBusy = false
    }
    val rotation = progress.value * 180f
    val lift = liftCurve(progress.value)
    val flipScale = 1f + 0.05f * lift
    val showFront = rotation < 90f

    var pressed by remember(current?.id) { mutableStateOf(false) }
    var flipPending by remember(current?.id) { mutableStateOf(false) }
    var pressFx by remember(current?.id) { mutableStateOf(0.5f) }
    var pressFy by remember(current?.id) { mutableStateOf(0.5f) }
    val pressAmount = remember(current?.id) { Animatable(0f) }
    LaunchedEffect(pressed, current?.id) {
        if (pressed) pressAmount.animateTo(1f, tween(120))
        else pressAmount.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    // clear glare/press whenever the face changes so no leftover highlight
    LaunchedEffect(revealed, current?.id) {
        pressed = false
        pressAmount.snapTo(0f)
    }
    LaunchedEffect(flipPending, current?.id) {
        if (flipPending) {
            delay(140)
            revealed = !revealed
            flipPending = false
        }
    }
    val pressTilt = pressAmount.value * (pressFx - 0.5f) * 16f
    val pressScale = 1f - 0.03f * pressAmount.value

    fun rate(r: Rating) {
        if (settings.vibration) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.rate(r)
    }

    fun glareMod(mirror: Boolean): Modifier = Modifier.fillMaxSize().drawBehind {
        val a = pressAmount.value * 0.14f
        if (a > 0.005f) {
            val fx = if (mirror) 1f - pressFx else pressFx
            drawRect(
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = a), Color.Transparent),
                    center = Offset(fx * size.width, pressFy * size.height),
                    radius = size.minDimension * 0.7f,
                ),
            )
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(96.dp)) {
                Text(
                    "$index/${queue.size}",
                    modifier = Modifier.align(Alignment.CenterStart),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    deckName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "共 ${queue.size} 卡 · FSRS-6",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier.width(96.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    IconButton(onClick = { showDeckMenu = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "切换牌组")
                    }
                    DropdownMenu(
                        expanded = showDeckMenu,
                        onDismissRequest = { showDeckMenu = false },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        shadowElevation = 10.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        decks.forEach { d ->
                            val active = d.id == selectedId
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(30.dp).clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (active) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                d.name.take(1),
                                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            d.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (active) {
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                },
                                onClick = { showDeckMenu = false; viewModel.setDeck(d.id) },
                            )
                        }
                    }
                }
                NavOverflowMenu(
                    current = Routes.REVIEW,
                    onNavigate = onNavigate,
                    onUndo = { viewModel.undo() },
                    undoEnabled = canUndo,
                    handwriting = handwriting,
                    onToggleHandwriting = { settings.updateHandwriting(!handwriting) },
                    canClearInk = if (showFront) frontInk.isNotEmpty() else backInk.isNotEmpty(),
                    onClearInk = {
                        if (showFront) frontInk.clear() else backInk.clear()
                        activeInk.clear()
                    },
                )
            }
        }


        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            current == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (reviewed > 0) "本次复习了 $reviewed 张" else "当前没有到期的卡片",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { onNavigate(Routes.LIBRARY) }, shape = RoundedCornerShape(12.dp)) { Text("去牌库") }
                }
            }
            else -> BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val cardW = maxWidth * 0.97f
                val cardH = maxHeight * 0.98f
                Box(
                    Modifier
                        .width(cardW)
                        .height(cardH)
                        .graphicsLayer {
                            rotationY = rotation + pressTilt
                            cameraDistance = 16f * density
                            scaleX = flipScale * pressScale
                            scaleY = flipScale * pressScale
                            translationY = -lift * 14.dp.toPx()
                        }
                        // 阴影与卡片一起做 3D 变换，旋转时不会错位露出灰色翻边
                        .drawBehind {
                            drawCardShadow((1f - 0.45f * lift) * (1f - pressAmount.value.coerceIn(0f, 1f)))
                        }
                        .clip(CardShape)
                        .pointerInput(current?.id, handwriting) {
                            val touchSlop = viewConfiguration.touchSlop
                            if (handwriting) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                    val pointerId = down.id
                                    val faceIsFront = progress.value < 0.5f
                                    val mirrored = !faceIsFront
                                    fun toInk(p: Offset): Offset =
                                        if (mirrored) Offset(size.width - p.x, p.y) else p

                                    activeInk.clear()
                                    activeInkFront = faceIsFront

                                    var moved = false
                                    var released = false
                                    var canceled = false
                                    while (!released && !canceled) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == pointerId }
                                        if (change == null) {
                                            canceled = true
                                            break
                                        }
                                        if (change.pressed) {
                                            val ink = toInk(change.position)
                                            if (!moved && (change.position - down.position).getDistance() > touchSlop) {
                                                moved = true
                                                activeInk.add(toInk(down.position))
                                                activeInk.add(ink)
                                            } else if (moved && (ink - activeInk.last()).getDistance() > 1.5.dp.toPx()) {
                                                activeInk.add(ink)
                                            }
                                        } else {
                                            released = true
                                        }
                                        event.changes.forEach { it.consume() }
                                    }

                                    if (released && moved) {
                                        (if (faceIsFront) frontInk else backInk).add(activeInk.toList())
                                        activeInk.clear()
                                    } else {
                                        activeInk.clear()
                                        if (released && !moved) {
                                            if (settings.vibration) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            if (!flipBusy) {
                                                flipBusy = true
                                                flipPending = true
                                            }
                                        }
                                    }
                                }
                            } else {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val pointerId = down.id
                                    pressed = true
                                    pressFx = (down.position.x / size.width).coerceIn(0f, 1f)
                                    pressFy = (down.position.y / size.height).coerceIn(0f, 1f)
                                    if (settings.vibration) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }

                                    var moved = false
                                    var canceled = false
                                    var released = false

                                    while (!released && !canceled) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId }
                                        if (change == null) {
                                            canceled = true
                                            break
                                        }
                                        if (change.isConsumed) moved = true
                                        if (change.pressed) {
                                            pressFx = (change.position.x / size.width).coerceIn(0f, 1f)
                                            pressFy = (change.position.y / size.height).coerceIn(0f, 1f)
                                            if ((change.position - down.position).getDistance() > touchSlop) {
                                                moved = true
                                            }
                                        }
                                        if (change.previousPressed && !change.pressed) {
                                            released = true
                                        }
                                    }

                                    pressed = false
                                    if (released && !moved) {
                                        if (settings.vibration) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        if (!flipBusy) {
                                            flipBusy = true
                                            flipPending = true
                                        }
                                    }
                                }
                            }
                        },
                ) {
                    if (showFront) {
                        CardFace(pip = "问") {
                            Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    frontBmp?.let {
                                        Image(
                                            it.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = 220.dp).clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Fit,
                                        )
                                        Spacer(Modifier.height(10.dp))
                                    }
                                    Text(
                                        current.front,
                                        fontSize = (if (current.front.length > 60) 20.sp else 24.sp) * settings.fontScale,
                                        lineHeight = (if (current.front.length > 60) 28.sp else 34.sp) * settings.fontScale,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Text(
                                    if (handwriting) "手写中 · 点按翻面" else "点按卡片翻面",
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Box(glareMod(false))
                        }
                    } else {
                        Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                            CardFace(pip = "答") {
                                Column(
                                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
                                ) {
                                    backBmp?.let {
                                        Image(
                                            it.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Fit,
                                        )
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    Text(
                                        current.back,
                                        fontSize = (if (current.back.length > 160) 15.sp else 18.sp) * settings.fontScale,
                                        lineHeight = (if (current.back.length > 160) 23.sp else 28.sp) * settings.fontScale,
                                    )
                                }
                                Box(glareMod(true))
                            }
                        }
                    }
                    if (handwriting) {
                        InkLayer(
                            front = showFront,
                            strokes = if (showFront) frontInk else backInk,
                            active = activeInk,
                            activeFront = activeInkFront,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        if (current != null) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RatingPill("重来", preview[Rating.Again], now, AgainColor) { rate(Rating.Again) }
                RatingPill("困难", preview[Rating.Hard], now, HardColor) { rate(Rating.Hard) }
                RatingPill("良好", preview[Rating.Good], now, GoodColor) { rate(Rating.Good) }
                RatingPill("简单", preview[Rating.Easy], now, EasyColor) { rate(Rating.Easy) }
            }
        }
    }
}

@Composable
private fun InkLayer(
    front: Boolean,
    strokes: List<List<Offset>>,
    active: List<Offset>,
    activeFront: Boolean,
    color: Color,
) {
    Canvas(
        Modifier.fillMaxSize().graphicsLayer { rotationY = if (front) 0f else 180f },
    ) {
        val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        strokes.forEach { drawInkStroke(it, color, stroke) }
        if (active.isNotEmpty() && activeFront == front) {
            drawInkStroke(active, color, stroke)
        }
    }
}

private fun DrawScope.drawInkStroke(points: List<Offset>, color: Color, stroke: Stroke) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color = color, radius = stroke.width / 2f, center = points[0])
        return
    }
    val path = Path()
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        path.lineTo(points[i].x, points[i].y)
    }
    drawPath(path, color, style = stroke)
}

@Composable
private fun CardFace(pip: String, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, CardShape),
    ) {
        content()
        Text(
            pip,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            pip,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).graphicsLayer { rotationZ = 180f },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.RatingPill(
    label: String,
    info: SchedulingInfo?,
    now: Long,
    color: Color,
    onClick: () -> Unit,
) {
    val interval = info?.let { formatInterval(it.card.due, now) } ?: ""
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
        ),
        elevation = null,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(interval, style = MaterialTheme.typography.labelSmall)
        }
    }
}
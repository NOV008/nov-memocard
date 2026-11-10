package com.nov.memocard.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nov.memocard.fsrs.CardState
import com.nov.memocard.ui.theme.Amber
import com.nov.memocard.ui.theme.Green
import com.nov.memocard.ui.theme.Indigo
import com.nov.memocard.ui.theme.Red
import com.nov.memocard.ui.theme.Violet
import kotlinx.coroutines.delay

fun heroBrush(): Brush = Brush.linearGradient(listOf(Indigo, Violet))

fun stateTint(state: Int): Color = when (CardState.from(state)) {
    CardState.New -> Indigo
    CardState.Learning -> Amber
    CardState.Review -> Green
    CardState.Relearning -> Red
}

@Composable
fun StatChip(label: String, value: Int, tint: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            "$label $value",
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun Entrance(index: Int, content: @Composable () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val offset = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        delay(index * 45L)
        alpha.animateTo(1f, tween(300))
    }
    LaunchedEffect(Unit) {
        delay(index * 45L)
        offset.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offset.value.dp.toPx()
        },
    ) {
        content()
    }
}
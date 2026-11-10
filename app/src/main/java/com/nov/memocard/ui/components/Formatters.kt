package com.nov.memocard.ui.components

import com.nov.memocard.fsrs.CardState

fun formatInterval(dueMillis: Long, now: Long): String {
    val minutes = (dueMillis - now) / 60_000L
    return when {
        minutes < 1 -> "现在"
        minutes < 60 -> "${minutes}分钟"
        minutes < 1440 -> "${minutes / 60}小时"
        else -> {
            val days = minutes / 1440
            when {
                days < 30 -> "${days}天"
                days < 365 -> "${days / 30}个月"
                else -> "${days / 365}年"
            }
        }
    }
}

fun stateLabel(state: Int): String = when (CardState.from(state)) {
    CardState.New -> "新卡"
    CardState.Learning -> "学习中"
    CardState.Review -> "复习"
    CardState.Relearning -> "重学"
}
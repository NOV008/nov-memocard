package com.nov.memocard.liveupdate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StudySessionState(
    val groupId: Long = 0L,
    val groupName: String = "",
    val totalDue: Int = 0,
    val reviewedCount: Int = 0,
    val currentCardLabel: String = "",
    val startedAt: Long = 0L,
    val active: Boolean = false,
    val completed: Boolean = false,
)

object StudySession {
    private val _state = MutableStateFlow(StudySessionState())
    val state = _state.asStateFlow()

    fun start(groupId: Long, groupName: String, totalDue: Int, currentLabel: String) {
        _state.value = StudySessionState(
            groupId = groupId,
            groupName = groupName,
            totalDue = totalDue,
            reviewedCount = 0,
            currentCardLabel = currentLabel,
            startedAt = System.currentTimeMillis(),
            active = true,
            completed = false,
        )
    }

    fun updateProgress(reviewed: Int, currentLabel: String) {
        _state.update {
            it.copy(
                reviewedCount = reviewed,
                currentCardLabel = currentLabel,
                completed = it.totalDue > 0 && reviewed >= it.totalDue,
            )
        }
    }

    fun end() {
        _state.value = StudySessionState()
    }
}
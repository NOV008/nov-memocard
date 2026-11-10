package com.nov.memocard.fsrs

import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

/**
 * BasicScheduler ported from ts-fsrs: learning steps for New/Learning/Relearning,
 * FSRS intervals for Review. All times are epoch milliseconds.
 */
class FsrsScheduler(private val algorithm: FsrsAlgorithm = FsrsAlgorithm()) {

    private val params get() = algorithm.params

    fun preview(card: FsrsCard, now: Long): Map<Rating, SchedulingInfo> {
        val ctx = Context(card, now)
        return Rating.entries.associateWith { ctx.review(it) }
    }

    fun review(card: FsrsCard, now: Long, rating: Rating): SchedulingInfo =
        Context(card, now).review(rating)

    private inner class Context(card: FsrsCard, val now: Long) {
        val last: FsrsCard = card
        val elapsedDays: Int =
            if (card.state != CardState.New && card.lastReview != null) {
                dateDiffInDays(card.lastReview, now)
            } else {
                0
            }
        val current: FsrsCard =
            card.copy(lastReview = now, reps = card.reps + 1, elapsedDays = elapsedDays)
        private val cache = HashMap<Rating, SchedulingInfo>()

        fun review(grade: Rating): SchedulingInfo = when (last.state) {
            CardState.New -> newState(grade)
            CardState.Learning, CardState.Relearning -> learningState(grade)
            CardState.Review -> reviewState(grade)
        }

        private fun nextDs(t: Int, g: Rating, r: Double? = null): FsrsCard {
            val ms = algorithm.nextState(
                MemoryState(current.stability, current.difficulty), t, g, r,
            )
            return current.copy(stability = ms.stability, difficulty = ms.difficulty)
        }

        private fun newState(grade: Rating): SchedulingInfo {
            cache[grade]?.let { return it }
            val next = nextDs(elapsedDays, grade)
            val scheduled = applyLearningSteps(next, grade, CardState.Learning)
            return build(grade, scheduled).also { cache[grade] = it }
        }

        private fun learningState(grade: Rating): SchedulingInfo {
            cache[grade]?.let { return it }
            val next = nextDs(elapsedDays, grade)
            val scheduled = applyLearningSteps(next, grade, last.state)
            return build(grade, scheduled).also { cache[grade] = it }
        }

        private fun reviewState(grade: Rating): SchedulingInfo {
            cache[grade]?.let { return it }
            val interval = elapsedDays
            val r = algorithm.forgettingCurve(interval, current.stability)
            val nextAgain = nextDs(interval, Rating.Again, r)
            val nextHard = nextDs(interval, Rating.Hard, r)
            val nextGood = nextDs(interval, Rating.Good, r)
            val nextEasy = nextDs(interval, Rating.Easy, r)

            var hardI = algorithm.nextInterval(nextHard.stability, interval)
            var goodI = algorithm.nextInterval(nextGood.stability, interval)
            hardI = min(hardI, goodI)
            goodI = max(goodI, hardI + 1)
            val easyI = max(algorithm.nextInterval(nextEasy.stability, interval), goodI + 1)

            val hard = nextHard.asReview(hardI)
            val good = nextGood.asReview(goodI)
            val easy = nextEasy.asReview(easyI)
            val again = applyLearningSteps(nextAgain, Rating.Again, CardState.Relearning)
                .let { it.copy(lapses = it.lapses + 1) }

            cache[Rating.Again] = build(Rating.Again, again)
            cache[Rating.Hard] = build(Rating.Hard, hard)
            cache[Rating.Good] = build(Rating.Good, good)
            cache[Rating.Easy] = build(Rating.Easy, easy)
            return cache[grade]!!
        }

        private fun FsrsCard.asReview(interval: Int): FsrsCard = copy(
            state = CardState.Review,
            learningSteps = 0,
            scheduledDays = interval,
            due = now + interval * DAY_MS,
        )

        private fun applyLearningSteps(
            nextCard: FsrsCard,
            grade: Rating,
            toState: CardState,
        ): FsrsCard {
            val info = learningStepsStrategy(current.state, current.learningSteps)[grade]
            val minutes = max(0, info?.minutes ?: 0)
            val nextSteps = max(0, info?.nextStep ?: 0)
            return when {
                minutes in 1..1439 -> nextCard.copy(
                    learningSteps = nextSteps,
                    scheduledDays = 0,
                    state = toState,
                    due = now + minutes * MIN_MS,
                )
                minutes >= 1440 -> nextCard.copy(
                    state = CardState.Review,
                    learningSteps = nextSteps,
                    due = now + minutes * MIN_MS,
                    scheduledDays = minutes / 1440,
                )
                else -> {
                    val interval = algorithm.nextInterval(nextCard.stability, elapsedDays)
                    nextCard.asReview(interval)
                }
            }
        }

        private fun build(grade: Rating, next: FsrsCard): SchedulingInfo {
            val log = FsrsReviewLog(
                rating = grade,
                state = current.state,
                stability = current.stability,
                difficulty = current.difficulty,
                elapsedDays = elapsedDays,
                scheduledDays = current.scheduledDays,
                learningSteps = current.learningSteps,
                review = now,
                due = last.lastReview ?: last.due,
            )
            return SchedulingInfo(next, log)
        }
    }

    private data class StepInfo(val minutes: Int, val nextStep: Int)

    private fun learningStepsStrategy(state: CardState, curStep: Int): Map<Rating, StepInfo> {
        val steps =
            if (state == CardState.Relearning || state == CardState.Review) {
                params.relearningSteps
            } else {
                params.learningSteps
            }
        val len = steps.size
        if (len == 0 || curStep >= len) return emptyMap()
        val first = steps[0]
        fun hardInterval(): Int =
            if (len == 1) Math.round(first * 1.5).toInt()
            else Math.round((first + steps[1]) / 2.0).toInt()
        val stepInfo = steps[max(0, curStep)]
        return if (state == CardState.Review) {
            mapOf(Rating.Again to StepInfo(stepInfo, 0))
        } else {
            buildMap {
                put(Rating.Again, StepInfo(first, 0))
                put(Rating.Hard, StepInfo(hardInterval(), curStep))
                if (curStep + 1 < len) put(Rating.Good, StepInfo(steps[curStep + 1], curStep + 1))
            }
        }
    }

    companion object {
        const val DAY_MS = 86_400_000L
        const val MIN_MS = 60_000L

        fun dateDiffInDays(last: Long, cur: Long): Int {
            val l = Instant.ofEpochMilli(last).atZone(ZoneOffset.UTC).toLocalDate()
            val c = Instant.ofEpochMilli(cur).atZone(ZoneOffset.UTC).toLocalDate()
            return ChronoUnit.DAYS.between(l, c).toInt()
        }
    }
}
package com.nov.memocard.fsrs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsSchedulerTest {

    private val now = 1_700_000_000_000L
    private val day = 86_400_000L
    private val minute = 60_000L
    private val scheduler = FsrsScheduler()
    private val algo = FsrsAlgorithm()

    @Test
    fun forgettingCurveAtStabilityIs90Percent() {
        assertEquals(0.9, algo.forgettingCurve(10, 10.0), 1e-6)
    }

    @Test
    fun intervalEqualsStabilityAt90PercentRetention() {
        assertEquals(1.0, algo.intervalModifier, 1e-6)
        assertEquals(20, algo.nextInterval(20.0, 0))
    }

    @Test
    fun newCardAgainGoesToLearningFirstStep() {
        val info = scheduler.review(FsrsCard(), now, Rating.Again)
        assertEquals(CardState.Learning, info.card.state)
        assertEquals(0, info.card.learningSteps)
        assertEquals(now + minute, info.card.due)
        assertEquals(0.212, info.card.stability, 1e-6)
        assertEquals(1, info.card.reps)
    }

    @Test
    fun newCardHardUsesAverageOfSteps() {
        val info = scheduler.review(FsrsCard(), now, Rating.Hard)
        assertEquals(CardState.Learning, info.card.state)
        assertEquals(0, info.card.learningSteps)
        assertEquals(now + 6 * minute, info.card.due)
    }

    @Test
    fun newCardGoodAdvancesToSecondStep() {
        val info = scheduler.review(FsrsCard(), now, Rating.Good)
        assertEquals(CardState.Learning, info.card.state)
        assertEquals(1, info.card.learningSteps)
        assertEquals(now + 10 * minute, info.card.due)
        assertEquals(2.3065, info.card.stability, 1e-6)
    }

    @Test
    fun newCardEasyGraduatesToReview() {
        val info = scheduler.review(FsrsCard(), now, Rating.Easy)
        assertEquals(CardState.Review, info.card.state)
        assertEquals(0, info.card.learningSteps)
        assertEquals(8, info.card.scheduledDays)
        assertEquals(now + 8 * day, info.card.due)
    }

    @Test
    fun learningLastStepGoodGraduates() {
        val card = FsrsCard(
            state = CardState.Learning, learningSteps = 1,
            stability = 2.3065, difficulty = 2.0, lastReview = now, reps = 1,
        )
        val info = scheduler.review(card, now, Rating.Good)
        assertEquals(CardState.Review, info.card.state)
        assertEquals(0, info.card.learningSteps)
        assertTrue(info.card.scheduledDays >= 1)
    }

    @Test
    fun learningAgainResetsToFirstStep() {
        val card = FsrsCard(
            state = CardState.Learning, learningSteps = 1,
            stability = 2.3065, difficulty = 2.0, lastReview = now, reps = 1,
        )
        val info = scheduler.review(card, now, Rating.Again)
        assertEquals(CardState.Learning, info.card.state)
        assertEquals(0, info.card.learningSteps)
        assertEquals(now + minute, info.card.due)
    }

    @Test
    fun reviewAgainGoesToRelearningAndIncrementsLapses() {
        val card = FsrsCard(
            state = CardState.Review, stability = 10.0, difficulty = 5.0,
            lastReview = now - 10 * day, reps = 5, lapses = 1, scheduledDays = 10,
        )
        val info = scheduler.review(card, now, Rating.Again)
        assertEquals(CardState.Relearning, info.card.state)
        assertEquals(2, info.card.lapses)
        assertEquals(0, info.card.learningSteps)
        assertEquals(now + 10 * minute, info.card.due)
    }

    @Test
    fun reviewGoodStaysReviewAndGrowsStability() {
        val card = FsrsCard(
            state = CardState.Review, stability = 10.0, difficulty = 5.0,
            lastReview = now - 10 * day, reps = 5, scheduledDays = 10,
        )
        val info = scheduler.review(card, now, Rating.Good)
        assertEquals(CardState.Review, info.card.state)
        assertTrue(info.card.stability > 10.0)
        assertTrue(info.card.scheduledDays >= 1)
        assertEquals(now + info.card.scheduledDays * day, info.card.due)
    }

    @Test
    fun reviewIntervalsAreMonotonic() {
        val card = FsrsCard(
            state = CardState.Review, stability = 10.0, difficulty = 5.0,
            lastReview = now - 10 * day, reps = 5, scheduledDays = 10,
        )
        val p = scheduler.preview(card, now)
        val hard = p.getValue(Rating.Hard).card.scheduledDays
        val good = p.getValue(Rating.Good).card.scheduledDays
        val easy = p.getValue(Rating.Easy).card.scheduledDays
        assertTrue(hard <= good)
        assertTrue(good < easy)
    }

    @Test
    fun relearningGoodGraduatesToReview() {
        val card = FsrsCard(
            state = CardState.Relearning, learningSteps = 0,
            stability = 1.0, difficulty = 5.0, lastReview = now, reps = 3, lapses = 1,
        )
        val info = scheduler.review(card, now, Rating.Good)
        assertEquals(CardState.Review, info.card.state)
    }

    @Test
    fun previewReturnsAllFourRatings() {
        val p = scheduler.preview(FsrsCard(), now)
        assertEquals(4, p.size)
        assertTrue(p.keys.containsAll(Rating.entries))
    }

    @Test
    fun difficultyStaysInBounds() {
        val card = FsrsCard(
            state = CardState.Review, stability = 10.0, difficulty = 9.5,
            lastReview = now - 10 * day, reps = 5,
        )
        val again = scheduler.review(card, now, Rating.Again).card
        assertTrue(again.difficulty in 1.0..10.0)
    }

    @Test
    fun logRecordsPreReviewState() {
        val card = FsrsCard(
            state = CardState.Review, stability = 10.0, difficulty = 5.0,
            lastReview = now - 10 * day, reps = 5,
        )
        val info = scheduler.review(card, now, Rating.Good)
        assertEquals(CardState.Review, info.log.state)
        assertEquals(10.0, info.log.stability, 1e-6)
        assertEquals(10, info.log.elapsedDays)
        assertEquals(Rating.Good, info.log.rating)
    }
}
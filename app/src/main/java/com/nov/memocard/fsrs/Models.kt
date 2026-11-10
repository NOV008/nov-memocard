package com.nov.memocard.fsrs

enum class Rating(val value: Int) {
    Again(1), Hard(2), Good(3), Easy(4);

    companion object {
        fun from(value: Int): Rating = entries.first { it.value == value }
    }
}

enum class CardState(val value: Int) {
    New(0), Learning(1), Review(2), Relearning(3);

    companion object {
        fun from(value: Int): CardState = entries.first { it.value == value }
    }
}

data class MemoryState(val stability: Double, val difficulty: Double)

data class FsrsCard(
    val state: CardState = CardState.New,
    val due: Long = 0L,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val learningSteps: Int = 0,
    val lastReview: Long? = null,
    val elapsedDays: Int = 0,
    val scheduledDays: Int = 0,
)

data class FsrsReviewLog(
    val rating: Rating,
    val state: CardState,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Int,
    val scheduledDays: Int,
    val learningSteps: Int,
    val review: Long,
    val due: Long,
)

data class SchedulingInfo(val card: FsrsCard, val log: FsrsReviewLog)

data class FsrsParameters(
    val w: DoubleArray = DEFAULT_W.copyOf(),
    val requestRetention: Double = 0.9,
    val maximumInterval: Int = 36500,
    val enableFuzz: Boolean = false,
    val enableShortTerm: Boolean = true,
    val learningSteps: IntArray = intArrayOf(1, 10),
    val relearningSteps: IntArray = intArrayOf(10),
) {
    companion object {
        val DEFAULT_W: DoubleArray = doubleArrayOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001,
            1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014,
            1.8729, 0.5425, 0.0912, 0.0658, 0.1542,
        )
    }
}
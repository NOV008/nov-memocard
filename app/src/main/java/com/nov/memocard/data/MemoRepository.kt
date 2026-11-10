package com.nov.memocard.data

import com.nov.memocard.fsrs.CardState
import com.nov.memocard.fsrs.FsrsAlgorithm
import com.nov.memocard.fsrs.FsrsCard
import com.nov.memocard.fsrs.FsrsParameters
import com.nov.memocard.fsrs.FsrsReviewLog
import com.nov.memocard.fsrs.FsrsScheduler
import com.nov.memocard.fsrs.Rating
import com.nov.memocard.fsrs.SchedulingInfo
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class MemoRepository(
    private val db: AppDatabase,
    private val retentionProvider: () -> Double = { 0.9 },
    private val limitsProvider: () -> Pair<Int, Int> = { 20 to 300 },
) {
    private fun scheduler(): FsrsScheduler =
        FsrsScheduler(FsrsAlgorithm(FsrsParameters(requestRetention = retentionProvider())))

    val decks: Flow<List<Deck>> = db.deckDao().observeAll()

    fun cards(deckId: Long): Flow<List<Card>> = db.cardDao().observeByDeck(deckId)
    fun allCards(): Flow<List<Card>> = db.cardDao().observeAll()
    fun searchCards(q: String): Flow<List<Card>> = db.cardDao().search(q)
    fun countNew(deckId: Long): Flow<Int> = db.cardDao().countNew(deckId)
    fun countDue(deckId: Long, now: Long): Flow<Int> = db.cardDao().countDue(deckId, now)
    fun countTotal(deckId: Long): Flow<Int> = db.cardDao().countTotal(deckId)

    suspend fun deckById(id: Long): Deck? = db.deckDao().getById(id)
    suspend fun cardById(id: Long): Card? = db.cardDao().getById(id)
    suspend fun addDeck(name: String): Long = db.deckDao().insert(Deck(name = name))
    suspend fun renameDeck(deck: Deck) = db.deckDao().update(deck)
    suspend fun deleteDeck(deck: Deck) = db.deckDao().delete(deck)

    suspend fun updateDeckLimits(deckId: Long, newPerDay: Int, reviewsPerDay: Int) {
        db.deckDao().getById(deckId)?.let {
            db.deckDao().update(it.copy(newPerDay = newPerDay, reviewsPerDay = reviewsPerDay))
        }
    }

    suspend fun addCard(
        deckId: Long,
        front: String,
        back: String,
        frontImage: String? = null,
        backImage: String? = null,
        tags: String = "",
        suspended: Boolean = false,
    ): Long = db.cardDao().insert(
        Card(
            deckId = deckId, front = front, back = back,
            frontImage = frontImage, backImage = backImage,
            tags = tags, suspended = suspended,
        ),
    )

    suspend fun updateCard(card: Card) = db.cardDao().update(card)
    suspend fun deleteCard(card: Card) = db.cardDao().delete(card)

    suspend fun dueCards(deckId: Long, now: Long): List<Card> {
        db.deckDao().getById(deckId) ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val dayStart = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = dayStart + 86_400_000L
        val introducedToday = db.reviewLogDao().countNewIntroduced(deckId, dayStart, dayEnd)
        val reviewsDoneToday = db.reviewLogDao().countReviewsDone(deckId, dayStart, dayEnd)
        val (newPerDay, reviewsPerDay) = limitsProvider()
        val reviewLimit = if (reviewsPerDay <= 0) Int.MAX_VALUE
        else (reviewsPerDay - reviewsDoneToday).coerceAtLeast(0)
        val newLimit = if (newPerDay <= 0) Int.MAX_VALUE
        else (newPerDay - introducedToday).coerceAtLeast(0)
        val reviews = db.cardDao().getDueReviews(deckId, now).take(reviewLimit)
        val news = db.cardDao().getNewCards(deckId).take(newLimit)
        return reviews + news
    }

    data class ReviewResult(val updated: Card, val logId: Long, val before: Card)

    fun preview(card: Card, now: Long): Map<Rating, SchedulingInfo> =
        scheduler().preview(card.toFsrs(), now)

    suspend fun reviewCard(card: Card, rating: Rating, now: Long): ReviewResult {
        val info = scheduler().review(card.toFsrs(), now, rating)
        val updated = info.card.toCard(card)
        db.cardDao().update(updated)
        val logId = db.reviewLogDao().insert(info.log.toEntity(card.id))
        return ReviewResult(updated, logId, card)
    }

    suspend fun undoReview(before: Card, logId: Long) {
        db.cardDao().update(before)
        db.reviewLogDao().deleteById(logId)
    }

    suspend fun exportJson(): String {
        val decksArr = JSONArray()
        for (deck in db.deckDao().getAll()) {
            val cardsArr = JSONArray()
            for (c in db.cardDao().listByDeck(deck.id)) {
                cardsArr.put(
                    JSONObject()
                        .put("front", c.front).put("back", c.back)
                        .put("state", c.state).put("due", c.due)
                        .put("stability", c.stability).put("difficulty", c.difficulty)
                        .put("reps", c.reps).put("lapses", c.lapses)
                        .put("step", c.step).put("lastReview", c.lastReview)
                        .put("scheduledDays", c.scheduledDays),
                )
            }
            decksArr.put(
                JSONObject()
                    .put("name", deck.name)
                    .put("newPerDay", deck.newPerDay)
                    .put("reviewsPerDay", deck.reviewsPerDay)
                    .put("cards", cardsArr),
            )
        }
        return JSONObject()
            .put("app", "Nov记忆卡")
            .put("version", 2)
            .put("exportedAt", System.currentTimeMillis())
            .put("decks", decksArr)
            .toString(2)
    }

    private fun Card.toFsrs(): FsrsCard = FsrsCard(
        state = CardState.from(state),
        due = due,
        stability = stability,
        difficulty = difficulty,
        reps = reps,
        lapses = lapses,
        learningSteps = step,
        lastReview = if (lastReview == 0L) null else lastReview,
        scheduledDays = scheduledDays,
    )

    private fun FsrsCard.toCard(original: Card): Card = original.copy(
        state = state.value,
        due = due,
        stability = stability,
        difficulty = difficulty,
        reps = reps,
        lapses = lapses,
        step = learningSteps,
        lastReview = lastReview ?: 0L,
        scheduledDays = scheduledDays,
    )

    private fun FsrsReviewLog.toEntity(cardId: Long): ReviewLog = ReviewLog(
        cardId = cardId,
        rating = rating.value,
        state = state.value,
        stability = stability,
        difficulty = difficulty,
        elapsedDays = elapsedDays,
        review = review,
    )

}
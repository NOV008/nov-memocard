package com.nov.memocard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Deck>>

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    suspend fun getAll(): List<Deck>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getById(id: Long): Deck?

    @Query("SELECT * FROM decks WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Deck?

    @Insert
    suspend fun insert(deck: Deck): Long

    @Update
    suspend fun update(deck: Deck)

    @Delete
    suspend fun delete(deck: Deck)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdAt DESC")
    fun observeByDeck(deckId: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards")
    fun observeAll(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdAt DESC")
    suspend fun listByDeck(deckId: Long): List<Card>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): Card?

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND state != 0 AND suspended = 0 AND due <= :now ORDER BY due ASC")
    suspend fun getDueReviews(deckId: Long, now: Long): List<Card>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND state = 0 AND suspended = 0 ORDER BY id ASC")
    suspend fun getNewCards(deckId: Long): List<Card>

    @Query(
        "SELECT * FROM cards WHERE front LIKE '%' || :q || '%' OR back LIKE '%' || :q || '%' " +
            "OR tags LIKE '%' || :q || '%' ORDER BY createdAt DESC"
    )
    fun search(q: String): Flow<List<Card>>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND state = 0 AND suspended = 0")
    fun countNew(deckId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND state != 0 AND suspended = 0 AND due <= :now")
    fun countDue(deckId: Long, now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    fun countTotal(deckId: Long): Flow<Int>

    @Insert
    suspend fun insert(card: Card): Long

    @Insert
    suspend fun insertAll(cards: List<Card>)

    @Update
    suspend fun update(card: Card)

    @Delete
    suspend fun delete(card: Card)
}

@Dao
interface ReviewLogDao {
    @Insert
    suspend fun insert(log: ReviewLog): Long

    @Query("DELETE FROM review_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM review_logs WHERE cardId = :cardId ORDER BY review DESC")
    fun observeByCard(cardId: Long): Flow<List<ReviewLog>>

    @Query(
        "SELECT COUNT(DISTINCT l.cardId) FROM review_logs l INNER JOIN cards c ON c.id = l.cardId " +
            "WHERE c.deckId = :deckId AND l.state = 0 AND l.review >= :start AND l.review < :end"
    )
    suspend fun countNewIntroduced(deckId: Long, start: Long, end: Long): Int

    @Query(
        "SELECT COUNT(*) FROM review_logs l INNER JOIN cards c ON c.id = l.cardId " +
            "WHERE c.deckId = :deckId AND l.state != 0 AND l.review >= :start AND l.review < :end"
    )
    suspend fun countReviewsDone(deckId: Long, start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM review_logs WHERE review >= :start AND review < :end")
    fun countBetween(start: Long, end: Long): Flow<Int>

    @Query("SELECT * FROM review_logs WHERE review >= :start ORDER BY review DESC")
    fun observeSince(start: Long): Flow<List<ReviewLog>>
}
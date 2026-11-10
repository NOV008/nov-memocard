package com.nov.memocard.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val newPerDay: Int = 20,
    val reviewsPerDay: Int = 300,
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("deckId"), Index("due")],
)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val front: String,
    val back: String,
    val createdAt: Long = System.currentTimeMillis(),
    val state: Int = 0,
    val due: Long = 0,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val step: Int = 0,
    val lastReview: Long = 0,
    val scheduledDays: Int = 0,
    val frontImage: String? = null,
    val backImage: String? = null,
    val tags: String = "",
    val suspended: Boolean = false,
)

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("cardId"), Index("review")],
)
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val rating: Int,
    val state: Int,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Int,
    val review: Long = System.currentTimeMillis(),
)
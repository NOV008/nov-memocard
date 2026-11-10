package com.nov.memocard.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nov.memocard.MemoCardApp
import com.nov.memocard.data.Card
import com.nov.memocard.data.Deck
import com.nov.memocard.liveupdate.StudyLiveUpdate
import com.nov.memocard.liveupdate.StudySession
import com.nov.memocard.fsrs.Rating
import com.nov.memocard.fsrs.SchedulingInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeckCounts(val new: Int = 0, val due: Int = 0, val total: Int = 0)

private fun appRepo(app: Application) = (app as MemoCardApp).repository
private fun appSettings(app: Application) = (app as MemoCardApp).settings

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val settings = appSettings(app)
}

class DeckListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = appRepo(app)
    val decks = repo.decks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun counts(deckId: Long, now: Long): Flow<DeckCounts> = combine(
        repo.countNew(deckId),
        repo.countDue(deckId, now),
        repo.countTotal(deckId),
    ) { new, due, total -> DeckCounts(new, due, total) }

    fun addDeck(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.addDeck(name.trim()) }
    }

    fun deleteDeck(deck: Deck) = viewModelScope.launch { repo.deleteDeck(deck) }

    fun renameDeck(deck: Deck, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.renameDeck(deck.copy(name = name.trim())) }
    }
}

class DeckDetailViewModel(app: Application, savedStateHandle: SavedStateHandle) :
    AndroidViewModel(app) {
    private val repo = appRepo(app)
    val deckId: Long = savedStateHandle.get<Long>("deckId") ?: 0L
    val cards = repo.cards(deckId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deck = MutableStateFlow<Deck?>(null)
    val deck = _deck.asStateFlow()

    init {
        viewModelScope.launch { _deck.value = repo.deckById(deckId) }
    }

    fun counts(now: Long): Flow<DeckCounts> = combine(
        repo.countNew(deckId),
        repo.countDue(deckId, now),
        repo.countTotal(deckId),
    ) { new, due, total -> DeckCounts(new, due, total) }

    fun deleteCard(card: Card) = viewModelScope.launch { repo.deleteCard(card) }

    fun renameDeck(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _deck.value?.let { repo.renameDeck(it.copy(name = name.trim())) }
            _deck.value = repo.deckById(deckId)
        }
    }

    fun deleteDeck(onDone: () -> Unit) = viewModelScope.launch {
        _deck.value?.let { repo.deleteDeck(it) }
        onDone()
    }

    fun updateLimits(newPerDay: Int, reviewsPerDay: Int) = viewModelScope.launch {
        repo.updateDeckLimits(deckId, newPerDay, reviewsPerDay)
        _deck.value = repo.deckById(deckId)
    }
}

class ReviewViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = appRepo(app)
    private val settings = appSettings(app)
    val decks = repo.decks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _queue = MutableStateFlow<List<Card>>(emptyList())
    val queue = _queue.asStateFlow()
    private val _index = MutableStateFlow(0)
    val index = _index.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()
    private val _reviewedCount = MutableStateFlow(0)
    val reviewedCount = _reviewedCount.asStateFlow()
    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()
    private var lastUndo: Pair<Card, Long>? = null

    fun load(deckId: Long) {
        if (deckId == 0L) {
            _queue.value = emptyList()
            _loading.value = false
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _queue.value = repo.dueCards(deckId, System.currentTimeMillis())
            StudySession.start(
                deckId,
                repo.deckById(deckId)?.name ?: "",
                _queue.value.size,
                _queue.value.firstOrNull()?.front ?: "",
            )
            _index.value = 0
            _reviewedCount.value = 0
            lastUndo = null
            _canUndo.value = false
            _loading.value = false
        }
    }

    fun setDeck(id: Long) = settings.setReviewDeck(id)

    fun preview(card: Card, now: Long): Map<Rating, SchedulingInfo> = repo.preview(card, now)

    fun rate(rating: Rating) {
        val card = _queue.value.getOrNull(_index.value) ?: return
        viewModelScope.launch {
            val result = repo.reviewCard(card, rating, System.currentTimeMillis())
            lastUndo = result.before to result.logId
            _canUndo.value = true
            _reviewedCount.value += 1
            StudySession.updateProgress(_reviewedCount.value, _queue.value.getOrNull(_index.value + 1)?.front ?: "")
            StudyLiveUpdate.onProgress()
            _index.value += 1
        }
    }

    fun undo() {
        val u = lastUndo ?: return
        viewModelScope.launch {
            repo.undoReview(u.first, u.second)
            lastUndo = null
            _canUndo.value = false
            if (_index.value > 0) _index.value -= 1
            if (_reviewedCount.value > 0) _reviewedCount.value -= 1
            StudySession.updateProgress(_reviewedCount.value, _queue.value.getOrNull(_index.value)?.front ?: "")
            StudyLiveUpdate.onProgress()
        }
    }
}

class CardEditViewModel(app: Application, savedStateHandle: SavedStateHandle) :
    AndroidViewModel(app) {
    private val repo = appRepo(app)
    val deckId: Long = savedStateHandle.get<Long>("deckId") ?: 0L
    val cardId: Long = savedStateHandle.get<Long>("cardId") ?: 0L

    var front by mutableStateOf("")
    var back by mutableStateOf("")
    var frontImage by mutableStateOf<String?>(null)
    var backImage by mutableStateOf<String?>(null)
    var tags by mutableStateOf("")
    var suspended by mutableStateOf(false)
    private val _loaded = MutableStateFlow(false)
    val loaded = _loaded.asStateFlow()

    init {
        if (cardId == 0L) {
            _loaded.value = true
        } else {
            viewModelScope.launch {
                repo.cardById(cardId)?.let {
                    front = it.front
                    back = it.back
                    frontImage = it.frontImage
                    backImage = it.backImage
                    tags = it.tags
                    suspended = it.suspended
                }
                _loaded.value = true
            }
        }
    }

    fun save(onDone: () -> Unit) {
        if (front.isBlank() || back.isBlank()) return
        viewModelScope.launch {
            if (cardId == 0L) {
                repo.addCard(deckId, front.trim(), back.trim(), frontImage, backImage, tags.trim(), suspended)
            } else {
                repo.cardById(cardId)?.let {
                    repo.updateCard(
                        it.copy(
                            front = front.trim(),
                            back = back.trim(),
                            frontImage = frontImage,
                            backImage = backImage,
                            tags = tags.trim(),
                            suspended = suspended,
                        ),
                    )
                }
            }
            onDone()
        }
    }
}
package com.nov.memocard.liveupdate

import kotlinx.coroutines.flow.MutableSharedFlow

object ReviewIntent {
    val openDeck = MutableSharedFlow<Long>(extraBufferCapacity = 1)
}
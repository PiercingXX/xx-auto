package com.piercingxx.xxauto.media

/**
 * Pure "which session is active" rule (design: the one currently `isPlaying`;
 * if none, the one that most recently had a media item; if none, `Nothing
 * playing`). No Android imports, so a JVM unit test can exercise every branch.
 * The [SessionHub] feeds it the live state of each suite player, most recently
 * active first, and the drive screen uses the result to pick the now-playing
 * card.
 */
object SessionPick {

    /**
     * The live state of one suite player, as observed by the hub. `hasItem`
     * means the session currently holds a media item (a station, a book
     * chapter) even when paused.
     */
    data class State(
        val isPlaying: Boolean,
        val hasItem: Boolean,
    )

    /**
     * The index of the active session in [states] (which is ordered most
     * recently active first), or null when nothing is playing and no session
     * has an item. A playing session always wins; ties among playing sessions
     * and among item-holding sessions fall to the most recent (lowest index).
     */
    fun active(states: List<State>): Int? {
        states.forEachIndexed { index, state ->
            if (state.isPlaying) return index
        }
        states.forEachIndexed { index, state ->
            if (state.hasItem) return index
        }
        return null
    }
}
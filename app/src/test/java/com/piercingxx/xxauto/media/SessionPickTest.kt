package com.piercingxx.xxauto.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionPickTest {

    @Test
    fun `no sessions means nothing playing`() {
        assertNull(SessionPick.active(emptyList()))
    }

    @Test
    fun `no playing session and no item means nothing playing`() {
        assertNull(
            SessionPick.active(
                listOf(
                    SessionPick.State(isPlaying = false, hasItem = false),
                    SessionPick.State(isPlaying = false, hasItem = false),
                )
            )
        )
    }

    @Test
    fun `playing session wins over a more recent session that only has an item`() {
        // Index 0 is the most recent; the audiobook (index 1) is playing but
        // the radio (index 0) only holds an item — playing wins.
        assertEquals(
            1,
            SessionPick.active(
                listOf(
                    SessionPick.State(isPlaying = false, hasItem = true),
                    SessionPick.State(isPlaying = true, hasItem = true),
                )
            )
        )
    }

    @Test
    fun `most recent item-holding session wins when nothing is playing`() {
        // Index 0 holds no item; indices 1 and 2 both hold one. Nothing is
        // playing, so the tie among item-holders must fall to the most recent
        // (lowest index) — 1, not the last match 2.
        assertEquals(
            1,
            SessionPick.active(
                listOf(
                    SessionPick.State(isPlaying = false, hasItem = false),
                    SessionPick.State(isPlaying = false, hasItem = true),
                    SessionPick.State(isPlaying = false, hasItem = true),
                )
            )
        )
    }

    @Test
    fun `most recent playing session wins when several are playing`() {
        // Index 0 is not playing; indices 1 and 2 both are. The tie among
        // playing sessions must fall to the most recent (lowest index) — 1,
        // not the last match 2.
        assertEquals(
            1,
            SessionPick.active(
                listOf(
                    SessionPick.State(isPlaying = false, hasItem = true),
                    SessionPick.State(isPlaying = true, hasItem = true),
                    SessionPick.State(isPlaying = true, hasItem = true),
                )
            )
        )
    }

    @Test
    fun `a playing session with no item still wins over a paused one with an item`() {
        assertEquals(
            1,
            SessionPick.active(
                listOf(
                    SessionPick.State(isPlaying = false, hasItem = true),
                    SessionPick.State(isPlaying = true, hasItem = false),
                )
            )
        )
    }
}
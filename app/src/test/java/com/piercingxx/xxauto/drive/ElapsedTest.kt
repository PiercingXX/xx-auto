package com.piercingxx.xxauto.drive

import org.junit.Assert.assertEquals
import org.junit.Test

class ElapsedTest {

    @Test
    fun `under an hour is m colon ss`() {
        assertEquals("0:00", Elapsed.format(0))
        assertEquals("0:59", Elapsed.format(59_999))
        assertEquals("4:05", Elapsed.format(245_000))
        assertEquals("59:59", Elapsed.format(3_599_000))
    }

    @Test
    fun `an hour and up is h colon mm colon ss`() {
        assertEquals("1:00:00", Elapsed.format(3_600_000))
        assertEquals("12:03:09", Elapsed.format((12 * 3600 + 3 * 60 + 9) * 1000L))
    }

    @Test
    fun `negative clamps to zero`() {
        assertEquals("0:00", Elapsed.format(-5_000))
    }

    @Test
    fun `row clamps elapsed to the duration`() {
        assertEquals("1:23 / 4:56", Elapsed.row(83_000, 296_000))
        assertEquals("4:56 / 4:56", Elapsed.row(400_000, 296_000))
    }
}

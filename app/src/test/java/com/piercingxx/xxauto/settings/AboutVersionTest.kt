package com.piercingxx.xxauto.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AboutVersionTest {
    @Test
    fun `version line names the app, version and build`() {
        assertEquals("xx-auto 0.1.0 (1)", AboutVersion.line("0.1.0", 1))
    }

    @Test
    fun `missing version falls back to the name`() {
        assertEquals("xx-auto", AboutVersion.line(null, 0))
    }
}

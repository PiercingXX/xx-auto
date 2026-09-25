package com.piercingxx.xxauto.log

import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

/** Plain JVM: AppLog touches Android only through the logcat sink, which stays null here. */
class AppLogTest {

    private lateinit var dir: File

    @Before
    fun setUp() {
        AppLog.resetForTest()
        dir = createTempDirectory("app-log").toFile()
        AppLog.init(dir)
    }

    @After
    fun tearDown() {
        AppLog.resetForTest()
        dir.deleteRecursively()
    }

    @Test
    fun infoLinesLandInTheRotatingFileAndTheRing() {
        AppLog.i("test", "hello field")
        Thread.sleep(50)
        // The ring buffer is the core deliverable: emit() pushes every line.
        assertTrue(AppLog.dump().contains("hello field"))
        // The rotating file must actually contain the emitted payload, proving
        // the appendFile path ran behind emit() — not just that init()'s own
        // "dir ..." line populated the file. The write is async on the writer
        // executor, so poll the file until the message lands (with a timeout).
        assertTrue(
            "emit() did not persist its payload to ${AppLog.LOG_NAME}",
            awaitFileContains(AppLog.logFile(), "hello field"),
        )
    }

    /** Polls [file] until it contains [needle] (the file write is async). */
    private fun awaitFileContains(file: File?, needle: String, timeoutMs: Long = 2000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val text = file?.takeIf { it.isFile }?.readText() ?: ""
            if (text.contains(needle)) return true
            Thread.sleep(10)
        }
        return false
    }

    @Test
    fun redactStripsTokensEmailsAndSecrets() {
        val redacted = AppLog.redact(
            "Bearer abc.def login password=hunter2 mail=a@b.com otpauth://totp/X",
        )
        assertTrue(!redacted.contains("abc.def"))
        assertTrue(!redacted.contains("hunter2"))
        assertTrue(!redacted.contains("a@b.com"))
        assertTrue(!redacted.contains("otpauth://totp/X"))
        assertTrue(redacted.contains("Bearer ***"))
    }

    @Test
    fun crashFileIsWritten() {
        AppLog.persistCrash(RuntimeException("boom"))
        val crash = AppLog.lastCrash()
        assertTrue(crash != null && crash.contains("boom"))
        assertTrue(AppLog.dumpFiles().any { it.name == AppLog.CRASH_NAME })
    }
}
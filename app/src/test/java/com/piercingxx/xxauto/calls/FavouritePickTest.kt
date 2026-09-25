package com.piercingxx.xxauto.calls

import android.content.Intent
import com.piercingxx.xxauto.calls.FavouritePick.Number
import com.piercingxx.xxauto.calls.FavouritePick.NumberType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavouritePickTest {

    @Test
    fun `mobile number wins over earlier non-mobile`() {
        val numbers = listOf(
            Number("home", NumberType.HOME),
            Number("mobile", NumberType.MOBILE),
        )
        assertEquals("mobile", FavouritePick.primaryNumber(numbers)?.value)
    }

    @Test
    fun `first number wins when no mobile present`() {
        val numbers = listOf(
            Number("work", NumberType.WORK),
            Number("other", NumberType.OTHER),
        )
        assertEquals("work", FavouritePick.primaryNumber(numbers)?.value)
    }

    @Test
    fun `empty list resolves to null`() {
        assertNull(FavouritePick.primaryNumber(emptyList()))
    }

    @Test
    fun `single mobile number is returned`() {
        val numbers = listOf(Number("mobile", NumberType.MOBILE))
        assertEquals("mobile", FavouritePick.primaryNumber(numbers)?.value)
    }

    @Test
    fun `favourites sheet dials with ACTION_CALL when call permission is held`() {
        // FavouritesSheet fires ACTION_CALL with the primary number's tel: URI.
        assertEquals(Intent.ACTION_CALL, dialAction(callPhoneDenied = false))
    }

    @Test
    fun `favourites sheet falls back to ACTION_DIAL when call permission is denied`() {
        // FavouritesSheet falls back to ACTION_DIAL when CALL_PHONE is denied.
        assertEquals(Intent.ACTION_DIAL, dialAction(callPhoneDenied = true))
    }
}
package com.piercingxx.xxauto.calls

/**
 * Pure model of which of a contact's numbers is the one to call (Phase 5, AU8).
 * Design: "one primary number each (prefer mobile)". No Android imports, so
 * [primaryNumber] is JVM-testable. The Android side (`Favourites`) maps each
 * `ContactsContract` phone row to a [Number] and feeds the list here; the
 * returned [Number] is the one the sheet shows and dials.
 */
object FavouritePick {

    /** The phone-number kinds, mapped from the `ContactsContract` type codes. */
    enum class NumberType { MOBILE, HOME, WORK, OTHER }

    /** One phone number of a contact, with its kind. */
    data class Number(
        val value: String,
        val type: NumberType,
    )

    /**
     * The primary number: the mobile one if the contact has one, else the first
     * in the list, else null when the contact has no numbers at all.
     */
    fun primaryNumber(numbers: List<Number>): Number? = when {
        numbers.isEmpty() -> null
        else -> numbers.firstOrNull { it.type == NumberType.MOBILE } ?: numbers.first()
    }
}
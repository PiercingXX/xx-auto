package com.piercingxx.xxauto.display

/**
 * Pure first-come rule for the external display (Phase 9, AU13). Whichever of
 * the two apps the user opened owns the second screen; the other stays on the
 * phone. No arbitration protocol, no service, no negotiation — that is a whole
 * subsystem for a problem one person driving one car does not have.
 *
 * [decide] answers whether xx-auto should claim the external display, given
 * whether the other app (xx-maps) already owns it and whether the user is
 * opening xx-auto. No Android imports, so it is JVM-testable. [ExternalDisplay]
 * feeds the live DisplayManager answers here and shows / releases the
 * Presentation the returned decision names.
 */
object DisplayClaim {

    /** What xx-auto should do with the external display. */
    enum class Decision { CLAIM, STAY_ON_PHONE }

    /**
     * xx-auto claims the display only when the user is opening xx-auto and the
     * other app does not already own it. If xx-maps got there first, xx-auto
     * stays on the phone — first-come, no arbitration.
     */
    fun decide(displayOwnedByOther: Boolean, isOpening: Boolean): Decision =
        if (isOpening && !displayOwnedByOther) Decision.CLAIM else Decision.STAY_ON_PHONE
}
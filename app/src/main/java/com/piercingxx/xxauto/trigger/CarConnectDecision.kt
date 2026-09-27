package com.piercingxx.xxauto.trigger

/**
 * Pure decision for the AU2 auto-launch trigger. No Android imports, so a JVM
 * unit test can exercise every branch; [CarConnectReceiver] feeds it live
 * values and acts on the result.
 *
 * - `ACL_CONNECTED` from the saved device opens xx-auto: directly when the
 *   overlay grant is held (the background-start exemption), otherwise through
 *   the one-tap notification.
 * - `ACL_DISCONNECTED` from the saved device closes xx-auto **only if it was
 *   auto-opened**. A manual launch never self-closes.
 * - Anything else — another device, no device picked, auto-launch off, an
 *   unrelated action — is ignored.
 */
object CarConnectDecision {

    const val ACTION_ACL_CONNECTED = "android.bluetooth.device.action.ACL_CONNECTED"
    const val ACTION_ACL_DISCONNECTED = "android.bluetooth.device.action.ACL_DISCONNECTED"

    enum class Outcome { OPEN, OPEN_VIA_NOTIFICATION, CLOSE, IGNORE }

    fun decide(
        action: String?,
        deviceAddress: String?,
        savedAddress: String?,
        autoLaunch: Boolean,
        overlayGranted: Boolean,
        wasAutoOpened: Boolean,
    ): Outcome {
        if (!autoLaunch || savedAddress.isNullOrBlank() || deviceAddress.isNullOrBlank()) return Outcome.IGNORE
        // Bluetooth addresses are hex; compare without caring how either side cased them.
        if (!deviceAddress.equals(savedAddress, ignoreCase = true)) return Outcome.IGNORE
        return when (action) {
            ACTION_ACL_CONNECTED -> if (overlayGranted) Outcome.OPEN else Outcome.OPEN_VIA_NOTIFICATION
            ACTION_ACL_DISCONNECTED -> if (wasAutoOpened) Outcome.CLOSE else Outcome.IGNORE
            else -> Outcome.IGNORE
        }
    }
}

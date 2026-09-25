package com.piercingxx.xxauto.trigger

/**
 * Pure decision for the AU2 auto-launch trigger: given the current settings and
 * the address of the Bluetooth device that just connected, should xx-auto open?
 * No Android imports, so a JVM unit test can exercise every branch. The
 * [CarConnectReceiver] feeds it live values from [com.piercingxx.xxauto.settings.AutoPrefs]
 * and acts on the result.
 */
object CarConnectDecision {

    /**
     * Launch only when auto-launch is switched on, a device has actually been
     * picked, and the connecting device is exactly that picked device. A picked
     * device with no matching connection never launches; auto-launch off never
     * launches regardless of what connects.
     */
    fun shouldLaunch(autoLaunch: Boolean, pickedDevice: String?, connectedAddress: String?): Boolean =
        autoLaunch && pickedDevice != null && pickedDevice == connectedAddress
}
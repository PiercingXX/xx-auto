package com.piercingxx.xxauto.trigger

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context

/** The phone's paired Bluetooth devices, for the auto-launch picker (AU2). */
object BondedDevices {

    data class Device(val name: String, val address: String)

    /**
     * Name + address of every bonded device, sorted by name. Empty when
     * Bluetooth is off / absent or the permission is missing (API 31+ needs
     * BLUETOOTH_CONNECT; the settings flow asks for it first).
     */
    @SuppressLint("MissingPermission")
    fun list(context: Context): List<Device> = runCatching {
        context.getSystemService(BluetoothManager::class.java)
            ?.adapter
            ?.bondedDevices
            .orEmpty()
            .map { Device(name = it.name?.takeIf(String::isNotBlank) ?: it.address, address = it.address) }
            .sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList())

    /** The saved device's current name, or its address when it is no longer paired. */
    fun nameOf(context: Context, address: String): String =
        list(context).firstOrNull { it.address.equals(address, ignoreCase = true) }?.name ?: address
}

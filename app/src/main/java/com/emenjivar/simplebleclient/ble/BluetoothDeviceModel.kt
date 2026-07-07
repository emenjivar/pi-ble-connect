package com.emenjivar.simplebleclient.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import kotlinx.serialization.Serializable

/**
 * Lightweight representation of a BLE peripheral exposing only primitive values.
 * Using primitives (instead of the final [BluetoothDevice]) keeps the model easy
 * to mock and safe to carry across compose navigation.
 */
@Serializable
data class BluetoothDeviceModel(
    val name: String?,
    val macAddress: String
)

@SuppressLint("MissingPermission")
fun BluetoothDevice.toModel() = BluetoothDeviceModel(
    name = name,
    macAddress = address
)

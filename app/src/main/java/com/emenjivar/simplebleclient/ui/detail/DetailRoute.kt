package com.emenjivar.simplebleclient.ui.detail

import com.emenjivar.simplebleclient.ble.model.BluetoothDeviceModel
import kotlinx.serialization.Serializable

@Serializable
data class DetailRoute(
    val device: BluetoothDeviceModel
)

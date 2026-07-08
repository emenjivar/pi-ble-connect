package com.emenjivar.simplebleclient.ui.main

import com.emenjivar.simplebleclient.ble.model.BleConnectionState
import com.emenjivar.simplebleclient.ble.model.BluetoothDeviceModel

data class MainUiState(
    val pairedDevices: List<BluetoothDeviceModel> = emptyList(),
    val connectionState: BleConnectionState = BleConnectionState.Disconnected
)

package com.emenjivar.simplebleclient.ui.main

import com.emenjivar.simplebleclient.ble.BleConnectionState
import com.emenjivar.simplebleclient.ble.BluetoothDeviceModel

data class MainUiState(
    val pairedDevices: List<BluetoothDeviceModel> = emptyList(),
    val connectionState: BleConnectionState = BleConnectionState.Disconnected
)

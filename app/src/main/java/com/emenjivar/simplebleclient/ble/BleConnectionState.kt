package com.emenjivar.simplebleclient.ble

import android.bluetooth.BluetoothGattCallback

sealed class BleConnectionState {
    object Disconnected: BleConnectionState()
    object Connecting: BleConnectionState()
    object Disconnecting : BleConnectionState()
    object Failed : BleConnectionState()

    /**
     * @param ready Indicates when the peripheral is ready for reading/writing operations.
     *  Becomes `true` after [BluetoothGattCallback.onDescriptorWrite] completes,
     *  which signals that the GATT stack is free and services/characteristics are fully discovered.
     *  Triggering a read/write while [ready]=false, will be silently ignored by the GATT stack
     */
    data class Connected(
        val device: BluetoothDeviceModel,
        val ready: Boolean = false
    ) : BleConnectionState()

    fun isConnected() = this is Connected
}

package com.emenjivar.simplebleclient.ble

import com.emenjivar.simplebleclient.ble.commands.LEDCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Fake BLE implementation that lets the app run end-to-end without a peripheral.
 * Selected when the `mock` flavor is active.
 */
class MockBleManager(
    private val bleNotifications: BleNotifications
) : CustomBleManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceModel>> = _scannedDevices.asStateFlow()

    private var ledState = LEDCommand.OFF

    override fun startScan() {
        scope.launch {
            delay(SCAN_DELAY_MS)
            _scannedDevices.update { mockDevices }
        }
    }

    override fun stopScan() {
        // No-op for the mock layer.
    }

    override fun connect(model: BluetoothDeviceModel) {
        scope.launch {
            _connectionState.update { BleConnectionState.Connecting }
            delay(CONNECT_DELAY_MS)
            _connectionState.update { BleConnectionState.Connected(model, ready = true) }
        }
    }

    override fun disconnect() {
        _connectionState.update { BleConnectionState.Disconnected }
    }

    override fun <T> observe(command: BleCommand.Read<T>): Flow<T> =
        bleNotifications.observe(command)

    override fun <T> readCharacteristic(command: BleCommand.Read<T>) {
        val value = fakeValueFor(command.characteristic) ?: return
        bleNotifications.emit(
            service = command.service,
            characteristic = command.characteristic,
            value = value
        )
    }

    override fun <T> writeCharacteristic(command: BleCommand.Write<T>, value: T) {
        if (command.characteristic == ledCharacteristicUUID && value is LEDCommand) {
            ledState = value
            // Echo the write back so ReadLedStatus observers update.
            bleNotifications.emit(
                service = command.service,
                characteristic = command.characteristic,
                value = byteArrayOf(value.bytes)
            )
        }
    }

    private fun fakeValueFor(characteristic: UUID): ByteArray? = when (characteristic) {
        getIPCharacteristicUUID -> MOCK_IP.toByteArray()
        getSSIDCharacteristicUUID -> MOCK_SSID.toByteArray()
        ledCharacteristicUUID -> byteArrayOf(ledState.bytes)
        else -> null
    }

    private companion object {
        const val SCAN_DELAY_MS = 800L
        const val CONNECT_DELAY_MS = 600L
        const val MOCK_IP = "192.168.1.123"
        const val MOCK_SSID = "MockNetwork"

        val mockDevices = listOf(
            BluetoothDeviceModel(name = "Raspberry Pi (Mock)", macAddress = "AA:BB:CC:DD:EE:01"),
            BluetoothDeviceModel(name = "Raspberry Pi Zero (Mock)", macAddress = "AA:BB:CC:DD:EE:02")
        )
    }
}

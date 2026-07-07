package com.emenjivar.simplebleclient.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Handles scanning operations.
 */
interface BleScanner {
    /**
     * List of scanned devices.
     */
    val scannedDevices: StateFlow<List<BluetoothDeviceModel>>

    /**
     * Start BLE scanning, filtering by [primaryServiceUUID]
     */
    fun startScan()

    /**
     * Stop BLE scanning
     */
    fun stopScan()

    /**
     * Recover a real [BluetoothDevice] from a previously scanned [macAddress].
     */
    fun getRemoteDevice(macAddress: String): BluetoothDevice
}

@SuppressLint("MissingPermission")
class BleScannerImp(
    context: Context
) : BleScanner {

    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    override val scannedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())

    override fun getRemoteDevice(macAddress: String): BluetoothDevice {
        val adapter = bluetoothAdapter ?: throw Exception("Bluetooth not supported")
        return adapter.getRemoteDevice(macAddress)
    }

    override fun startScan() {
        if (bluetoothAdapter == null) {
            throw Exception("Bluetooth not supported")
        }

        if (!bluetoothAdapter.isEnabled) {
            // Bluetooth was manually disabled, prompt the user to enable it
            throw BluetoothDisabledException()
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString(primaryServiceUUID.toString()))
            .build()
        val setting = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scannedDevices.update { emptyList() }
        bluetoothAdapter.bluetoothLeScanner?.startScan(listOf(filter), setting, scanCallback)
    }

    override fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result?.device != null) {
                val model = result.device.toModel()
                scannedDevices.update { current ->
                    val exist = current.any { it.macAddress == model.macAddress }
                    if (exist) {
                        current.map { if (it.macAddress == model.macAddress) model else it }
                    } else {
                        current + model
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
    }
}
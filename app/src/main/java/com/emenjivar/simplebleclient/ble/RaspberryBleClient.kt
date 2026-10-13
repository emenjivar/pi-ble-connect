package com.emenjivar.simplebleclient.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import com.emenjivar.simplebleclient.ble.commands.BleCommand
import com.emenjivar.simplebleclient.ble.commands.clientCharacteristicConfigUUID
import com.emenjivar.simplebleclient.ble.commands.json.JSONChunk
import com.emenjivar.simplebleclient.ble.commands.ledCharacteristicUUID
import com.emenjivar.simplebleclient.ble.commands.primaryServiceUUID
import com.emenjivar.simplebleclient.ble.exceptions.CharacteristicNotFoundException
import com.emenjivar.simplebleclient.ble.model.BleConnectionState
import com.emenjivar.simplebleclient.ble.model.BluetoothDeviceModel
import com.emenjivar.simplebleclient.ble.model.toModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Real BLE implementation backed by Android's GATT stack.
 * Selected when the `raspberry` flavor is active.
 */
@SuppressLint("MissingPermission")
class RaspberryBleClient(
    private val context: Context,
    private val bleNotifications: BleNotifications,
    private val bleOperationQueue: BleOperationQueue,
    private val scanner: BleScanner
) : BleClient, BleScanner by scanner {

    private var bluetoothGatt: BluetoothGatt? = null
    private val _connectionState =
        MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private class BleReadResult(
        val service: UUID,
        val characteristic: UUID,
        val value: ByteArray
    )

    // Write operations does not return anything really,
    // but using this class to verify the operation ends
    private class BleWriteResult(
        val service: UUID,
        val characteristic: UUID
    )

    // A new instance is created on every read
    private var readDeferred = CompletableDeferred<BleReadResult>()
    private var writeDeferred = CompletableDeferred<BleWriteResult>()
    private var mtuDeferred = CompletableDeferred<Int>()
    private var cachedMtu: Int? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mtuNegotiationJob: Job? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    val connectedDevice = gatt?.device
                    if (connectedDevice != null) {
                        _connectionState.update { BleConnectionState.Connected(connectedDevice.toModel()) }
                    } else {
                        _connectionState.update { BleConnectionState.Failed }
                    }

                    gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    mtuNegotiationJob = scope.launch { getMTU() }
                    gatt?.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    mtuNegotiationJob?.cancel()
                    bleOperationQueue.clear()
                    bluetoothGatt?.close()
                    _connectionState.update { BleConnectionState.Disconnected }
                    bluetoothGatt = null
                    cachedMtu = null
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.update { BleConnectionState.Connecting }
                }

                BluetoothProfile.STATE_DISCONNECTING -> {
                    _connectionState.update { BleConnectionState.Disconnecting }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // TODO: you could iterate the notification characteristics here
                //  using [BleCommand.Read.registry], but filtering by `notification`
                val characteristic = gatt
                    ?.getService(primaryServiceUUID)
                    ?.getCharacteristic(ledCharacteristicUUID) ?: return

                // Enable notifications locally on android
                gatt.setCharacteristicNotification(characteristic, true)

                // Hardcoded UUID for receiving notifications
                val descriptor = characteristic.getDescriptor(clientCharacteristicConfigUUID)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(
                        descriptor,
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    )
                } else {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mtuDeferred.complete(mtu)
            } else {
                mtuDeferred.completeExceptionally(Exception("MTU negotiation failed"))
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readDeferred.complete(
                    BleReadResult(
                        service = characteristic.service.uuid,
                        characteristic = characteristic.uuid,
                        value = value
                    )
                )
            } else {
                readDeferred.completeExceptionally(
                    exception = Exception("Read operation ${characteristic.uuid} failed with status $status")
                )
            }

            bleOperationQueue.operationComplete()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic?.value

                if (value != null) {
                    readDeferred.complete(
                        BleReadResult(
                            service = characteristic.service.uuid,
                            characteristic = characteristic.uuid,
                            value = value
                        )
                    )
                } else {
                    // Handle error here
                    readDeferred.completeExceptionally(
                        exception = Exception("Read value is null")
                    )
                }
            } else {
                readDeferred.completeExceptionally(
                    exception = Exception("Read operation ${characteristic?.uuid} failed with status $status")
                )
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            bleNotifications.emit(
                service = characteristic.service.uuid,
                characteristic = characteristic.uuid,
                value = value
            )
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            _connectionState.update { state ->
                if (state is BleConnectionState.Connected) {
                    state.copy(ready = true)
                } else {
                    state
                }
            }
            bleOperationQueue.operationComplete()
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeDeferred.complete(
                    BleWriteResult(
                        service = characteristic.service.uuid,
                        characteristic = characteristic.uuid
                    )
                )
            } else {
                writeDeferred.completeExceptionally(
                    exception = Exception("Write operation ${characteristic.uuid} failed with status $status")
                )
            }
            bleOperationQueue.operationComplete()
        }
    }

    override fun connect(model: BluetoothDeviceModel) {
        val device = scanner.getRemoteDevice(model.macAddress)
        _connectionState.update { BleConnectionState.Connecting }
        stopScan()
        bluetoothGatt?.close()
        bluetoothGatt =
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    override fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    override fun <T> observe(command: BleCommand.Read<T>): Flow<T> =
        bleNotifications.observe(command)

    override fun <T> readCharacteristic(command: BleCommand.Read<T>) {
        val characteristic = command.getCharacteristic() ?: throw CharacteristicNotFoundException()
        bleOperationQueue.enqueue { bluetoothGatt?.readCharacteristic(characteristic) }
    }

    override fun <T> writeCharacteristic(command: BleCommand.Write<T>, value: T) {
        val characteristic = command.getCharacteristic() ?: throw CharacteristicNotFoundException()
        val encodedValue = command.encode(value)

        bleOperationQueue.enqueue {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(
                    characteristic,
                    encodedValue,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                characteristic.value = encodedValue
                bluetoothGatt?.writeCharacteristic(characteristic)
            }
        }
    }

    override suspend fun getMTU(): Int {
        cachedMtu?.let { return it }

        val pending = CompletableDeferred<Int>()
        mtuDeferred = pending

        // Android 14+ ignore the negotiation and set 517 by default
        bluetoothGatt?.requestMtu(MAX_MTU)

        val negotiatedMTU = withTimeout(MTU_TIMEOUT) { pending.await() }
        cachedMtu = negotiatedMTU
        return negotiatedMTU
    }

    override suspend fun getUsableBytes(): Int {
        val negotiatedMTU = getMTU()
        return negotiatedMTU - HEADER_SIZE
    }

    override suspend fun <T> write(
        command: BleCommand.Write<T>,
        value: T
    ) {
        val characteristic = command.getCharacteristic() ?: throw CharacteristicNotFoundException()
        val pending = CompletableDeferred<BleWriteResult>()

        val encodedValue = command.encode(value)
        writeDeferred = pending

        // This fires the bluetooth request, setting the result in the callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothGatt?.writeCharacteristic(
                characteristic,
                encodedValue,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
        } else {
            characteristic.value = encodedValue
            bluetoothGatt?.writeCharacteristic(characteristic)
        }

        val result = withTimeout(WRITE_TIMEOUT) { pending.await() }

        // Guard to ensure the request command matched with the returned result
        if (result.service != command.service || result.characteristic != command.characteristic) {
            throw Exception(
                "Wrong write request was executed. Expected ${command.characteristic}, returned: ${result.characteristic}"
            )
        }
    }

    override suspend fun <T> read(command: BleCommand.Read<T>): T {
        val characteristic = command.getCharacteristic() ?: throw CharacteristicNotFoundException()
        val pending = CompletableDeferred<BleReadResult>()
        readDeferred = pending

        // This fires the bluetooth request, setting the result in the callback
        bluetoothGatt?.readCharacteristic(characteristic)

        val result = withTimeout(READ_TIMEOUT) { pending.await() }

        // Guard to ensure the request command matches with the returned result
        if (result.service != command.service || result.characteristic != command.characteristic) {
            throw Exception(
                "Wrong characteristic mapped. expected: ${command.characteristic}, returned: ${result.characteristic}"
            )
        }

        return command.decode(result.value)
    }

    // TODO: shouldn't ve here
    override suspend fun read(command: BleCommand.ReadJSON): JSONChunk {
        val characteristic = command.getCharacteristic() ?: throw CharacteristicNotFoundException()
        val pending = CompletableDeferred<BleReadResult>()
        readDeferred = pending

        // This fires the bluetooth request, setting the result in the callback
        bluetoothGatt?.readCharacteristic(characteristic)

        val result = withTimeout(READ_TIMEOUT) { pending.await() }

        // Guard to ensure the request command matches with the returned result
        if (result.service != command.service || result.characteristic != command.characteristic) {
            throw Exception(
                "Wrong characteristic mapped. expected: ${command.characteristic}, returned: ${result.characteristic}"
            )
        }

        return command.decode(result.value)
    }

    private fun <T> BleCommand<T>.getCharacteristic(): BluetoothGattCharacteristic? {
        val characteristic = bluetoothGatt
            ?.getService(service)
            ?.getCharacteristic(characteristic)

        return characteristic
    }

    companion object {
        private val READ_TIMEOUT = 10.seconds
        private val WRITE_TIMEOUT = 10.seconds
        private val MTU_TIMEOUT = 10.seconds
        private const val MAX_MTU = 517

        // usable_bytes = negotiated_mtu = header_size
        private const val HEADER_SIZE = 5
    }
}

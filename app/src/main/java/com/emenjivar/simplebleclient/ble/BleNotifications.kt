package com.emenjivar.simplebleclient.ble

import com.emenjivar.simplebleclient.ble.commands.BleCommand
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

interface BleNotifications {
    fun emit(service: UUID, characteristic: UUID, value: ByteArray)
    fun <T> observe(command: BleCommand.Read<T>): Flow<T>
}

class BleNotificationsImp @Inject constructor() : BleNotifications {
    private val _updates = MutableSharedFlow<Pair<UUID, ByteArray>>(
        replay = 1,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun emit(service: UUID, characteristic: UUID, value: ByteArray) {
        val command = BleCommand.Read.getCommand(
            service = service,
            characteristic = characteristic
        )
        if (command != null) {
            _updates.tryEmit(command.characteristic to value)
        }
    }

    override fun <T> observe(command: BleCommand.Read<T>): Flow<T> =
        _updates
            .filter { (uuid, _) -> uuid == command.characteristic }
            .map { (_, bytes) -> command.decode(bytes) }
}
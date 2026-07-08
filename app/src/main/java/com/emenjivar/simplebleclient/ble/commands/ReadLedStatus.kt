package com.emenjivar.simplebleclient.ble.commands

/**
 * Read whether the led is on/off
 */
object ReadLedStatus : BleCommand.Read<LEDCommand>(
    service = primaryServiceUUID,
    characteristic = ledCharacteristicUUID
) {
    override fun decode(bytes: ByteArray): LEDCommand {
        val decodedValue = LEDCommand.entries.find { it.bytes == bytes.firstOrNull() }
        if (decodedValue != null) {
            return decodedValue
        }

        throw IllegalArgumentException("Fail to decode $bytes")
    }
}

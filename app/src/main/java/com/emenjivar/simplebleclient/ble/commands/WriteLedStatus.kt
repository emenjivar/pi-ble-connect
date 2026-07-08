package com.emenjivar.simplebleclient.ble.commands

/**
 * Turn on/off the LED
 */
object WriteLedStatus : BleCommand.Write<LEDCommand>(
    service = primaryServiceUUID,
    characteristic = ledCharacteristicUUID
) {
    override fun encode(value: LEDCommand): ByteArray {
        return byteArrayOf(value.bytes)
    }
}
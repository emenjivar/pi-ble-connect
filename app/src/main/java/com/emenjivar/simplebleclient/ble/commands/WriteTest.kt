package com.emenjivar.simplebleclient.ble.commands

object WriteTest : BleCommand.Write<String>(
    service = primaryServiceUUID,
    characteristic = testCharacteristicUUID
) {
    override fun encode(value: String): ByteArray {
        return value.toByteArray(Charsets.UTF_8)
    }
}
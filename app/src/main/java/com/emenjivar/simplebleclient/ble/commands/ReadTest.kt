package com.emenjivar.simplebleclient.ble.commands

object ReadTest : BleCommand.Read<String>(
    service = primaryServiceUUID,
    characteristic = testCharacteristicUUID
) {
    override fun decode(bytes: ByteArray): String {
        return String(bytes, Charsets.UTF_8)
    }
}
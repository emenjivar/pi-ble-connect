package com.emenjivar.simplebleclient.ble.commands

object GetSSID : BleCommand.Read<String>(
    service = primaryServiceUUID,
    characteristic = getSSIDCharacteristicUUID
) {
    override fun decode(bytes: ByteArray): String {
        return String(bytes, Charsets.UTF_8)
    }
}

package com.emenjivar.simplebleclient.ble.commands

object GetIPAddress : BleCommand.Read<String>(
    service = primaryServiceUUID,
    characteristic = getIPCharacteristicUUID
) {
    override fun decode(bytes: ByteArray): String {
        return String(bytes, Charsets.UTF_8)
    }
}

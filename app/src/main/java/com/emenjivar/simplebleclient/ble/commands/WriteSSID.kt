package com.emenjivar.simplebleclient.ble.commands

import com.emenjivar.simplebleclient.ble.BleCommand
import com.emenjivar.simplebleclient.ble.primaryServiceUUID
import com.emenjivar.simplebleclient.ble.writeSSIDCharacteristicUUID

object WriteSSID : BleCommand.Write<String>(
    service = primaryServiceUUID,
    characteristic = writeSSIDCharacteristicUUID
) {
    override fun encode(value: String): ByteArray {
        return value.encodeToByteArray()
    }
}

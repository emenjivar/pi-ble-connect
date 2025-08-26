package com.emenjivar.simplebleclient.ble.commands

import com.emenjivar.simplebleclient.ble.BleCommand
import com.emenjivar.simplebleclient.ble.primaryServiceUUID
import com.emenjivar.simplebleclient.ble.writeNetworkPasswordUUID

object WriteNetworkPassword : BleCommand.Write<String>(
    service = primaryServiceUUID,
    characteristic = writeNetworkPasswordUUID
) {
    override fun encode(value: String): ByteArray {
        return value.encodeToByteArray()
    }
}

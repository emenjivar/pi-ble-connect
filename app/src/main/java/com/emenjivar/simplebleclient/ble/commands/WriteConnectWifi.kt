package com.emenjivar.simplebleclient.ble.commands

import com.emenjivar.simplebleclient.ble.BleCommand
import com.emenjivar.simplebleclient.ble.commands.WriteConnectWifi.Commands
import com.emenjivar.simplebleclient.ble.primaryServiceUUID
import com.emenjivar.simplebleclient.ble.writeConnectWifiUUID



object WriteConnectWifi : BleCommand.Write<Commands>(
    service = primaryServiceUUID,
    characteristic = writeConnectWifiUUID
) {
    override fun encode(value: Commands): ByteArray {
        return byteArrayOf(value.byte)
    }

    enum class Commands(val byte: Byte) {
        DISCONNECT(0x00), // TODO: to be implemented
        CONNECT(0x01)
    }
}
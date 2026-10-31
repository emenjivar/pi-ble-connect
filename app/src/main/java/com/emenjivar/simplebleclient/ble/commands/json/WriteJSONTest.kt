package com.emenjivar.simplebleclient.ble.commands.json

import com.emenjivar.simplebleclient.ble.commands.BleCommand
import com.emenjivar.simplebleclient.ble.commands.jsonServiceUUID
import com.emenjivar.simplebleclient.ble.commands.writeJSONTestUUID

object WriteJSONTest : BleCommand.WriteJSON(
    service = jsonServiceUUID,
    characteristic = writeJSONTestUUID
)
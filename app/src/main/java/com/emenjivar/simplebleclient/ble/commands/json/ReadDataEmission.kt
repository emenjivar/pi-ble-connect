package com.emenjivar.simplebleclient.ble.commands.json

import com.emenjivar.simplebleclient.ble.commands.BleCommand
import com.emenjivar.simplebleclient.ble.commands.jsonServiceUUID
import com.emenjivar.simplebleclient.ble.commands.dataEmissionUUID
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class JSONChunk(
    val currentOffset: Int,
    val totalSize: Int,
    val content: List<Byte>
)

object ReadDataEmission: BleCommand.ReadJSON(
    service = jsonServiceUUID,
    characteristic = dataEmissionUUID
)

/**
 * @param chunkSize Size in bytes of the chunks to be emitted.
 *  the value expect some math before being sent.
 *  chuck = MTU - ATTHeaderSize - chunkHeaderSize
 *  where ATTHeaderSize is 3 bytes and chunkHeaderSize is 4 bytes.
 *  That math is not included here, but we expect [chunkSize] to be treated using that formula
 *  before passing it as a parameter here.
 * @param offset Position, in bytes, where the emission should (re)start.
 */
data class ResetChunkParams(
    val chunkSize: Int,
    val offset: Int
)

object ResetDataEmission: BleCommand.Write<ResetChunkParams>(
    service = jsonServiceUUID,
    characteristic = dataEmissionUUID
) {
    override fun encode(value: ResetChunkParams): ByteArray {
        return ByteBuffer.allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(value.chunkSize)
            .putInt(value.offset)
            .array()
    }
}

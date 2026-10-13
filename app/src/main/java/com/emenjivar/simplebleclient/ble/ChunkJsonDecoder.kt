package com.emenjivar.simplebleclient.ble

import com.emenjivar.simplebleclient.ble.commands.BleCommand
import com.emenjivar.simplebleclient.ble.commands.json.ResetChunkParams
import kotlinx.serialization.json.Json

class ChunkJsonDecoder(
    private val bleManager: BleClient
) {
    /**
     * Resets the offset of the chunk-emissions
     *
     * @param offset Position, in bytes, to (re)start the emission from. Defaults to 0,
     *  a fresh emission; pass a value greater than 0 to resume a previous one.
     */
    internal suspend fun resetOffset(command: BleCommand.Write<ResetChunkParams>, offset: Int = 0) {
        bleManager.write(
            command = command,
            value = ResetChunkParams(chunkSize = getUsableBytesPerChunk(), offset = offset)
        )
    }

    /**
     * @param readCommand Command for fetching a JSON through the GAT server
     * @param resetCommand Reset the offset of the chunk emission before executing [readCommand]
     */
    internal suspend inline fun <reified T> fetchJSON(
        readCommand: BleCommand.ReadJSON,
        resetCommand: BleCommand.Write<ResetChunkParams>
    ): T {
        resetOffset(resetCommand)

        val response = bleManager.read(readCommand)
        var currentOffset: Int = response.currentOffset
        var totalSize: Int = response.totalSize
        val receivedBytes = mutableListOf<Byte>()
        receivedBytes.addAll(response.content)

        while (currentOffset < totalSize) {
            val newResponse = bleManager.read(readCommand)
            currentOffset = newResponse.currentOffset

            // This value should be the same for all the requests
            totalSize = newResponse.totalSize
            receivedBytes.addAll(newResponse.content)
        }

        val json = String(receivedBytes.toByteArray(), Charsets.UTF_8)
        val payload = Json.decodeFromString<T>(json)
        return payload
    }

    suspend fun getUsableBytesPerChunk(): Int {
        val mtuLimitedSize = bleManager.getMTU() - ATT_HEADER_SIZE - CHUNK_HEADER_SIZE
        // The Bluetooth Core Spec (Vol 3, Part F, 3.2.8) caps any attribute value at 512 bytes,
        // regardless of the negotiated MTU. Without this, a max-MTU chunk (header + content)
        // comes out to 514 bytes and gets silently truncated by 2 bytes on the wire.
        val specLimitedSize = MAX_ATTRIBUTE_VALUE_SIZE - CHUNK_HEADER_SIZE
        return minOf(mtuLimitedSize, specLimitedSize)
    }

    companion object {
        // Bytes used for ATT DPU header, prepend on every packet by the low level BLE protocol
        private const val ATT_HEADER_SIZE = 3

        // Bytes used for the data_emission notification
        // 2 bytes are for current offset of the data emission
        // 2 bytes are the total size of the content
        private const val CHUNK_HEADER_SIZE = 4

        // Hard ceiling from the Bluetooth Core Spec on any single attribute value's length.
        private const val MAX_ATTRIBUTE_VALUE_SIZE = 512
    }
}

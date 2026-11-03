package com.emenjivar.simplebleclient.ble.json

import com.emenjivar.simplebleclient.ble.BleClient
import com.emenjivar.simplebleclient.ble.commands.BleCommand
import com.emenjivar.simplebleclient.ble.commands.json.ResetChunkParams
import kotlinx.serialization.json.Json

class ChunkJsonReader(
    private val bleClient: BleClient,
    private val chunkSizeCalculator: ChunkSizeCalculator
) {
    /**
     * Resets the offset of the chunk-emissions
     *
     * @param offset Position, in bytes, to (re)start the emission from. Defaults to 0,
     *  a fresh emission; pass a value greater than 0 to resume a previous one.
     */
    internal suspend fun resetOffset(command: BleCommand.Write<ResetChunkParams>, offset: Int = 0) {
        bleClient.write(
            command = command,
            value = ResetChunkParams(
                chunkSize = chunkSizeCalculator.getUsableBytesPerChunk(),
                offset = offset
            )
        )
    }

    /**
     * @param readCommand Command for fetching a JSON through the GAT server
     * @param resetCommand Reset the offset of the chunk emission before executing [readCommand]
     */
    internal suspend inline fun <reified T> read(
        readCommand: BleCommand.ReadJSON,
        resetCommand: BleCommand.Write<ResetChunkParams>
    ): T {
        resetOffset(resetCommand)

        val response = bleClient.read(readCommand)
        var currentOffset: Int = response.currentOffset
        var totalSize: Int = response.totalSize
        val receivedBytes = mutableListOf<Byte>()
        receivedBytes.addAll(response.content)

        while (currentOffset < totalSize) {
            val newResponse = bleClient.read(readCommand)
            currentOffset = newResponse.currentOffset

            // This value should be the same for all the requests
            totalSize = newResponse.totalSize
            receivedBytes.addAll(newResponse.content)
        }

        val json = String(receivedBytes.toByteArray(), Charsets.UTF_8)
        val payload = Json.decodeFromString<T>(json)
        return payload
    }
}

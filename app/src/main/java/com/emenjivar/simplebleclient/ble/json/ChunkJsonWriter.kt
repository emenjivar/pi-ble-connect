package com.emenjivar.simplebleclient.ble.json

import android.util.Log
import com.emenjivar.simplebleclient.ble.BleClient
import com.emenjivar.simplebleclient.ble.commands.BleCommand
import com.emenjivar.simplebleclient.ble.commands.json.JSONChunk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChunkJsonWriter(
    private val bleClient: BleClient,
    private val chunkSizeCalculator: ChunkSizeCalculator
) {

    internal suspend inline fun <reified T> write(
        command: BleCommand.WriteJSON,
        value: T
    ) {
        // Catch this exception later
        val jsonToWrite = Json.encodeToString(value)
        val chunkSize = chunkSizeCalculator.getUsableBytesPerChunk()

        // Size of bytes instead of size of the string
        val totalSize = jsonToWrite.toByteArray(Charsets.UTF_8).size

        // Chunk the byte array instead of chunking the string
        val chunksToWrite = jsonToWrite
            .toByteArray(Charsets.UTF_8)
            .toList().chunked(chunkSize)

        var offset = 0

        chunksToWrite.forEach { chunk ->
            Log.d("ChunkJsonDecoder", "writing JSON: { offset : $offset, totalSize: $totalSize, content: $chunk }")
            bleClient.write(
                command = command,
                value = JSONChunk(
                    currentOffset = offset,
                    totalSize = totalSize,
                    content = chunk
                )
            )

            offset += chunkSize
        }
    }
}

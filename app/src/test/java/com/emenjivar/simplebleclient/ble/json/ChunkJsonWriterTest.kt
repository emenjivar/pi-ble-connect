package com.emenjivar.simplebleclient.ble.json

import com.emenjivar.simplebleclient.ble.BleClient
import com.emenjivar.simplebleclient.ble.commands.json.JSONChunk
import com.emenjivar.simplebleclient.ble.commands.json.WriteJSONTest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith

class ChunkJsonWriterTest {

    private val bleClient = mock<BleClient>()
    private val chunkSizeCalculator = mock<ChunkSizeCalculator>()
    private val chunkJsonWriter = ChunkJsonWriter(
        bleClient = bleClient,
        chunkSizeCalculator = chunkSizeCalculator
    )

    @Test
    fun `writes should throw an exception when value is not serializable`() = runTest {
        // Given a non-serializable data class
        data class FakeClass(val test: String)
        val notSerializableClass = FakeClass(test = "foo")

        // The serialization should throw an exception
        assertFailsWith<SerializationException> {
            chunkJsonWriter.write(WriteJSONTest, notSerializableClass)
        }
    }

    @Test
    fun `write should send a single chunk when the serialized JSON fits in one chunk`() = runTest {
        @Serializable
        data class TestJSON(val message: String)

        // Given a chunk size larger than the JSON
        val chunkSize = 113
        whenever(chunkSizeCalculator.getUsableBytesPerChunk()).thenReturn(chunkSize)

        val value = TestJSON(message = "hello")
        chunkJsonWriter.write(WriteJSONTest, value)

        // 19 bytes string
        val expectedBytes = """{"message":"hello"}""".toByteArray(Charsets.UTF_8).toList()

        // Then a single chunk is written
        verify(bleClient).write(
            command = WriteJSONTest,
            value = JSONChunk(
                currentOffset = 0,
                totalSize = expectedBytes.size,
                content = expectedBytes
            )
        )
    }

    @Test
    fun `write should split the JSON into multiple chunks and keep offsets and totalSize consistent`() =
        runTest {
            @Serializable
            data class TestJSON(val message: String)

            // Given a small chunk-size
            val chunkSize = 1
            whenever(chunkSizeCalculator.getUsableBytesPerChunk()).thenReturn(chunkSize)

            val value = TestJSON(message = "hi")
            chunkJsonWriter.write(WriteJSONTest, value)

            // 16 bytes string
            val expectedBytes = """{"message":"hi"}""".toByteArray(Charsets.UTF_8).toList()
            val expectedChunkCount = expectedBytes.chunked(chunkSize).size

            val chunkCaptor = argumentCaptor<JSONChunk>()
            verify(bleClient, times(expectedChunkCount)).write(
                command = eq(WriteJSONTest),
                value = chunkCaptor.capture()
            )

            val capturedChunks = chunkCaptor.allValues
            capturedChunks.forEachIndexed { index, chunk ->
                assertEquals(index * chunkSize, chunk.currentOffset)
                assertEquals(expectedBytes.size, chunk.totalSize)
            }

            // Reassembling every chunk's content should reproduce the original JSON bytes, in order
            val reassembled = capturedChunks.flatMap { it.content }
            assertEquals(expectedBytes, reassembled)
        }
}
package com.emenjivar.simplebleclient.ble.json

import com.emenjivar.simplebleclient.ble.BleClient
import com.emenjivar.simplebleclient.ble.commands.json.JSONChunk
import com.emenjivar.simplebleclient.ble.commands.json.ReadDataEmission
import com.emenjivar.simplebleclient.ble.commands.json.ResetChunkParams
import com.emenjivar.simplebleclient.ble.commands.json.ResetDataEmission
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ChunkJsonReaderTest {

    @Test
    fun `resetOffset should write the usable chunk size and offset to RequestDataEmission`() = runTest {
        val mtuSize = 120
        val bleManager = mock<BleClient>()
        whenever(bleManager.getMTU()).thenReturn(mtuSize)
        val chunkSizeCalculator = ChunkSizeCalculatorImp(bleClient = bleManager)
        val chunkJsonReader = ChunkJsonReader(
            bleClient = bleManager,
            chunkSizeCalculator = chunkSizeCalculator
        )

        chunkJsonReader.resetOffset(ResetDataEmission, offset = 0)

        val expectedChunkSize = chunkSizeCalculator.getUsableBytesPerChunk()
        verify(bleManager)
            .write(
                command = ResetDataEmission,
                value = ResetChunkParams(chunkSize = expectedChunkSize, offset = 0)
            )
    }

    @Test
    fun `resetOffset should forward a non-zero offset to resume a previous emission`() = runTest {
        val mtuSize = 120
        val bleManager = mock<BleClient>()
        whenever(bleManager.getMTU()).thenReturn(mtuSize)
        val chunkSizeCalculator = ChunkSizeCalculatorImp(bleClient = bleManager)
        val chunkJsonReader = ChunkJsonReader(
            bleClient = bleManager,
            chunkSizeCalculator = chunkSizeCalculator
        )

        chunkJsonReader.resetOffset(ResetDataEmission, offset = 300)

        val expectedChunkSize = chunkSizeCalculator.getUsableBytesPerChunk()
        verify(bleManager)
            .write(
                command = ResetDataEmission,
                value = ResetChunkParams(chunkSize = expectedChunkSize, offset = 300)
            )
    }

    @Test
    fun `fetchJSON should reset the offset every time it's called`() = runTest {
        val mtuSize = 120
        val bleManager = mock<BleClient>()
        whenever(bleManager.getMTU()).thenReturn(mtuSize)

        // We don't care about the parsing here (covered elsewhere), so an empty
        // response is fine even though it makes fetchJSON throw.
        whenever(bleManager.read(ReadDataEmission)).thenReturn(
            JSONChunk(
                currentOffset = 0,
                totalSize = 0,
                content = emptyList()
            )
        )
        val chunkSizeCalculator = ChunkSizeCalculatorImp(bleClient = bleManager)
        val chunkJsonReader = ChunkJsonReader(
            bleClient = bleManager,
            chunkSizeCalculator = chunkSizeCalculator
        )

        // Call it twice, to prove the reset happens on every call, not just the first
        runCatching {
            chunkJsonReader.read<Any>(
                readCommand = ReadDataEmission,
                resetCommand = ResetDataEmission
            )
        }
        runCatching {
            chunkJsonReader.read<Any>(
                readCommand = ReadDataEmission,
                resetCommand = ResetDataEmission
            )
        }

        val expectedChunkSize = chunkSizeCalculator.getUsableBytesPerChunk()
        verify(bleManager, times(2))
            .write(
                command = ResetDataEmission,
                value = ResetChunkParams(chunkSize = expectedChunkSize, offset = 0)
            )
    }

    @Test
    fun `fetchJSON should reassemble chunks into the original JSON string`() =
        runTest {
            @Serializable
            data class TestJSON(val message: String)

            val mtuSize = 8
            val json = "{ \"message\" : \"hello\" }"
            val chunkIterator = chunkString(value = json, mtuSize = mtuSize)
            val bleManager = mock<BleClient>()
            whenever(bleManager.getMTU()).thenReturn(mtuSize)
            whenever(bleManager.read(ReadDataEmission))
                .thenAnswer { chunkIterator.next() }

            val chunkJsonReader = ChunkJsonReader(
                bleClient = bleManager,
                chunkSizeCalculator = ChunkSizeCalculatorImp(bleClient = bleManager)
            )

            val result = chunkJsonReader.read<TestJSON>(
                readCommand = ReadDataEmission,
                resetCommand = ResetDataEmission
            )
            assertEquals(TestJSON(message = "hello"), result)
        }

    // Cannot declare it inside the test function
    enum class ConnectionState {
        CONNECTED, ERROR
    }

    @Test
    fun `fetchJSON should parse long and nested JSONs`() = runTest {
        // Given some complex/nested data classes
        @Serializable
        data class SensorReading(
            val name: String,
            val value: Double,
            val unit: String,
            val timestamp: Long
        )

        @Serializable
        data class NetworkInfo(
            val ssid: String,
            val rssi: Int,
            val ipAddress: String,
            val state: ConnectionState,
            val errorMessage: String?
        )

        @Serializable
        data class StatusSnapshot(
            val deviceId: String,
            val firmwareVersion: String,
            val batteryLevel: Int,
            val isCharging: Boolean,
            val uptimeSeconds: Long,
            val sensors: List<SensorReading>,
            val network: NetworkInfo,
            val tags: List<String>
        )

        val longJSON = """
            {
                "deviceId": "pi001",
                "firmwareVersion": "1.0.0",
                "batteryLevel": 87,
                "isCharging": false,
                "uptimeSeconds": 18430,
                "sensors": [
                        { "name": "temperature", "value": 22.5, "unit": "celsius", "timestamp": 1735489200 },
                        { "name": "humidity", "value": 48.2, "unit": "percent", "timestamp": 1735489200 },
                        { "name": "pressure", "value": 1013.25, "unit": "hpa", "timestamp": 1735489201 }
                ],
                "network" : {
                    "ssid": "Network5G",
                    "rssi": -58,
                    "ipAddress": "192.168.1.2",
                    "state": "CONNECTED",
                    "errorMessage": null
                },
                "tags": [ "school", "zone1", "outdoor" ]
            }
        """.trimIndent()

        val mtuSize = 16
        val iterator = chunkString(value = longJSON, mtuSize = mtuSize)
        val bleManager = mock<BleClient>()
        whenever(bleManager.getMTU()).thenReturn(mtuSize)
        whenever(bleManager.read(ReadDataEmission))
            .thenAnswer { iterator.next() }

        val chunkJsonReader = ChunkJsonReader(
            bleClient = bleManager,
            chunkSizeCalculator = ChunkSizeCalculatorImp(bleClient = bleManager)
        )

        val result = chunkJsonReader.read<StatusSnapshot>(
            readCommand = ReadDataEmission,
            resetCommand = ResetDataEmission
        )
        val expectedStatusSnapshot = StatusSnapshot(
            deviceId = "pi001",
            firmwareVersion = "1.0.0",
            batteryLevel = 87,
            isCharging = false,
            uptimeSeconds = 18430,
            sensors = listOf(
                SensorReading(
                    name = "temperature",
                    value = 22.5,
                    unit = "celsius",
                    timestamp = 1735489200
                ),
                SensorReading(
                    name = "humidity",
                    value = 48.2,
                    unit = "percent",
                    timestamp = 1735489200
                ),
                SensorReading(
                    name = "pressure",
                    value = 1013.25,
                    unit = "hpa",
                    timestamp = 1735489201
                )
            ),
            network = NetworkInfo(
                ssid = "Network5G",
                rssi = -58,
                ipAddress = "192.168.1.2",
                state = ConnectionState.CONNECTED,
                errorMessage = null
            ),
            tags = listOf("school", "zone1", "outdoor")
        )
        assertEquals(expectedStatusSnapshot, result)
    }

    @Test
    fun `fetchJSON should throw an exception when parsing an invalid JSON`() =
        runTest {
            @Serializable
            data class TestJSON(val message: String)

            val mtuSize = 8

            // Missing the closing brace on purpose
            val invalidJson = "{ \"message\" : \"hello\" "

            val chunkIterator = chunkString(value = invalidJson, mtuSize = mtuSize)
            val bleManager = mock<BleClient>()
            whenever(bleManager.getMTU()).thenReturn(mtuSize)
            whenever(bleManager.read(ReadDataEmission))
                .thenAnswer { chunkIterator.next() }

            val chunkJsonReader = ChunkJsonReader(
                bleClient = bleManager,
                chunkSizeCalculator = ChunkSizeCalculatorImp(bleClient = bleManager)
            )

            val result = runCatching {
                chunkJsonReader.read<TestJSON>(
                    readCommand = ReadDataEmission,
                    resetCommand = ResetDataEmission
                )
            }

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SerializationException)
        }

    private fun chunkString(value: String, mtuSize: Int): Iterator<JSONChunk> {
        val byteArray = value.toByteArray(Charsets.UTF_8)
        val chunkSize = byteArray.size / mtuSize
        val chunks = byteArray.toList().chunked(chunkSize)
            .mapIndexed { index, bytes ->
                JSONChunk(
                    currentOffset = ((index + 1) * chunkSize).coerceAtMost(byteArray.size),
                    totalSize = byteArray.size,
                    content = bytes
                )
            }
        return chunks.iterator()
    }
}

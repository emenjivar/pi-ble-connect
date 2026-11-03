package com.emenjivar.simplebleclient.ble.json

import com.emenjivar.simplebleclient.ble.BleClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ChunkSizeCalculatorTest {

    @Test
    fun `getUsableBytesPerChunk should subtract ATT and chuck headers from MTU`() = runTest {
        val bleManager = mock<BleClient>()
        whenever(bleManager.getMTU()).thenReturn(120)
        val chunkSizeCalculator = ChunkSizeCalculatorImp(bleClient = bleManager)

        val result = chunkSizeCalculator.getUsableBytesPerChunk()
        assertEquals(113, result) // 120 - 3 (ATT header) - 4 (chunk header)
    }

    @Test
    fun `getUsableBytesPerChunk should cap the chunk size at the 512-byte attribute value limit`() = runTest {
        val bleManager = mock<BleClient>()
        whenever(bleManager.getMTU()).thenReturn(517) // max MTU
        val chunkSizeCalculator = ChunkSizeCalculatorImp(bleClient = bleManager)

        val result = chunkSizeCalculator.getUsableBytesPerChunk()
        // Naively would be 517 - 3 - 4 = 510, but header(4) + content(510) = 514 bytes
        // exceeds the Bluetooth Core Spec's 512-byte attribute value cap, so it must be
        // clamped to 512 - 4 = 508.
        assertEquals(508, result)
    }
}

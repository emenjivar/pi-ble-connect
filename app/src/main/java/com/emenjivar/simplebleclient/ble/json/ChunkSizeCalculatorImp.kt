package com.emenjivar.simplebleclient.ble.json

import com.emenjivar.simplebleclient.ble.BleClient

interface ChunkSizeCalculator {
    suspend fun getUsableBytesPerChunk(): Int
}

class ChunkSizeCalculatorImp(
    private val bleClient: BleClient
) : ChunkSizeCalculator {
    override suspend fun getUsableBytesPerChunk(): Int {
        val mtuLimitedSize = bleClient.getMTU() - ATT_HEADER_SIZE - CHUNK_HEADER_SIZE
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

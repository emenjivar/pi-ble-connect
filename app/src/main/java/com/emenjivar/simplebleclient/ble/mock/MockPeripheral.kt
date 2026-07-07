package com.emenjivar.simplebleclient.ble.mock

data class MockPeripheral(
    val macAddress: String,
    val name: String,
    val ip: String,
    val ssid: String
)

class MockBleDataSource {
    fun getDeviceDataByMacAddress(macAddress: String): MockPeripheral? {
        return mockPeripheralData[macAddress.lowercase()]
    }

    fun getAllDevices(): List<MockPeripheral> = mockPeripheralData.values.toList()

    private val peripheral1 = MockPeripheral(
        macAddress = "aa:bb:cc:dd:ee:01",
        name = "Raspberry Pi 3 (mock)",
        ip = "192.168.1.120",
        ssid = "Mock network"
    )

    private val peripheral2 = MockPeripheral(
        macAddress = "aa:bb:cc:dd:ee:02",
        name = "Raspberry Pi 4 (mock)",
        ip = "192.168.1.121",
        ssid = "Mock network"
    )

    private val mockPeripheralData = listOf(peripheral1, peripheral2)
        .associateBy { it.macAddress.lowercase() }
}

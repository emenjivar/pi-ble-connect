package com.emenjivar.simplebleclient.ui.detail

/**
 * @param ssid The network name displayed in WIFI settings.
 * @param rssi The strength indicator for the network.
 *  The value is represented in a negative form, the closer the value to 0,
 *  the stronger the received signal has been.
 *  [Received signal strength](https://en.wikipedia.org/wiki/Received_signal_strength_indicator#In_802.11_implementations)
 */
data class WifiNetwork(
    val ssid: String,
    val rssi: Int
)

sealed class StateResult<out T> {
    object Idle : StateResult<Nothing>()
    object Loading : StateResult<Nothing>()
    class Success<out T>(val data: T) : StateResult<T>()
}
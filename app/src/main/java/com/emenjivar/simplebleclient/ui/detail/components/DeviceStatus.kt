package com.emenjivar.simplebleclient.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emenjivar.simplebleclient.ble.model.BleConnectionState
import com.emenjivar.simplebleclient.ui.theme.GreenOpaque
import com.emenjivar.simplebleclient.ui.theme.Orange
import com.emenjivar.simplebleclient.ui.theme.RedRaspberry

@Composable
fun DeviceStatus(
    modifier: Modifier = Modifier,
    status: BleConnectionState
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 5.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(status.getStatusColor())
            )

            Text(
                text = status.getStatusName(),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

fun BleConnectionState.getStatusColor(): Color {
    return when (this) {
        is BleConnectionState.Connected -> GreenOpaque
        is BleConnectionState.Connecting -> Orange
        else -> RedRaspberry
    }
}

private fun BleConnectionState.getStatusName(): String {
    return when (this) {
        is BleConnectionState.Connected -> "Connected"
        BleConnectionState.Connecting -> "Connecting..."
        BleConnectionState.Disconnecting -> "Disconnecting..."
        BleConnectionState.Disconnected -> "Disconnected"
        BleConnectionState.Failed -> "Failed"
    }
}

@Preview
@Composable
private fun DeviceStatusConnectingPreview() {
    DeviceStatus(status = BleConnectionState.Connecting)
}

@Preview
@Composable
private fun DeviceStatusDisconnectedPreview() {
    DeviceStatus(status = BleConnectionState.Disconnected)
}
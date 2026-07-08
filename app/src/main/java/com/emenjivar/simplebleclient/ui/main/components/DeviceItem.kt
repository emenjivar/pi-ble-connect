package com.emenjivar.simplebleclient.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emenjivar.simplebleclient.R
import com.emenjivar.simplebleclient.ble.model.BleConnectionState
import com.emenjivar.simplebleclient.ui.detail.components.getStatusColor
import com.emenjivar.simplebleclient.ui.theme.SimpleBLEClientTheme

private const val ALPHA_SUBTEXT = 0.8f

@Composable
fun DeviceItem(
    name: String,
    macAddress: String,
    modifier: Modifier = Modifier,
    status: BleConnectionState = BleConnectionState.Disconnecting,
    onClick: () -> Unit
) {
    val statusColor = remember(status) {
        status.getStatusColor()
    }
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {

            Icon(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(40.dp),
                painter = painterResource(R.drawable.ic_raspberry_pi),
                contentDescription = "Raspberry PI"
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        text = name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
                Text(
                    modifier = Modifier.alpha(ALPHA_SUBTEXT),
                    text = macAddress,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            IconButton(onClick = onClick) {
                Icon(
                    Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = null
                )
            }
        }
    }
}

@Preview
@Composable
private fun DeviceItemPreview() {
    SimpleBLEClientTheme {
        DeviceItem(
            name = "Raspberry Pi 4",
            macAddress = "aa:bb:cc:dd:ee:ff",
            onClick = {}
        )
    }
}
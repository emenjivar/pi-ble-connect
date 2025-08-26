package com.emenjivar.simplebleclient.ui.detail

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emenjivar.simplebleclient.R
import com.emenjivar.simplebleclient.ble.BleConnectionState
import com.emenjivar.simplebleclient.ble.commands.LEDCommand
import com.emenjivar.simplebleclient.permission.LocationPermissionDeniedDialog
import com.emenjivar.simplebleclient.ui.components.PrimaryButton
import com.emenjivar.simplebleclient.ui.components.SecondaryButton
import com.emenjivar.simplebleclient.ui.detail.components.DeviceSpecificationItem
import com.emenjivar.simplebleclient.ui.detail.components.DeviceStatus
import com.emenjivar.simplebleclient.ui.detail.components.WifiBottomSheet
import com.emenjivar.simplebleclient.ui.theme.SimpleBLEClientTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

private val permissions = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.CHANGE_WIFI_STATE,
    Manifest.permission.ACCESS_WIFI_STATE
)

@Composable
fun DetailScreen(
    route: DetailRoute,
    viewModel: DetailViewModel = hiltViewModel<DetailViewModel, DetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(route) }
    ),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetailScreen(
        uiState = uiState,
        onUpdateLedState = viewModel::updateLedState,
        onConnectDevice = viewModel::connect,
        onConnectToWifi = viewModel::scanWifiNetworks,
        connectToWifiNetwork  = viewModel::connectToWifiNetwork,
        onDisconnectDevice = viewModel::disconnect,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onUpdateLedState: (LEDCommand) -> Unit,
    onConnectDevice: () -> Unit,
    onConnectToWifi: () -> Unit,
    connectToWifiNetwork: (WifiNetworkCredentials) -> Unit,
    onDisconnectDevice: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var showBottomSheet by remember { mutableStateOf(false) }
    var openPermissionDeniedDialog by remember { mutableStateOf(false) }
    val permissionState = rememberMultiplePermissionsState(
        permissions = permissions,
        onPermissionsResult = { results ->
            if (results.values.any { !it }) {
                openPermissionDeniedDialog = true
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                title = {
                    Text(
                        text = uiState.deviceName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        },
        bottomBar = {
            AnimatedContent(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(20.dp),
                targetState = uiState.connectionState,
                transitionSpec = { fadeIn().togetherWith(fadeOut()) }
            ) { connectionState ->
                when (uiState.connectionState) {
                    is BleConnectionState.Connected -> {
                        val enableButton = remember(uiState.connectionState) {
                            uiState.connectionState.isConnected()
                        }

                        Column {
                            PrimaryButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Connect to WIFI",
                                icon = R.drawable.ic_wifi,
                                enabled = enableButton,
                                onClick = {
                                    when {
                                        permissionState.allPermissionsGranted -> {
                                            onConnectToWifi()
                                            showBottomSheet = true
                                        }

                                        permissionState.shouldShowRationale -> openPermissionDeniedDialog =
                                            true

                                        else -> permissionState.launchMultiplePermissionRequest()
                                    }
                                }
                            )
                            SecondaryButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Disconnect",
                                icon = R.drawable.ic_disconnect,
                                enabled = enableButton,
                                onClick = onDisconnectDevice
                            )
                        }
                    }

                    else -> {
                        val enableButton = remember(uiState.connectionState) {
                            uiState.connectionState == BleConnectionState.Disconnected || uiState.connectionState == BleConnectionState.Failed
                        }
                        PrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Connect",
                            icon = R.drawable.ic_connect,
                            enabled = enableButton,
                            onClick = onConnectDevice
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                DeviceStatus(
                    status = uiState.connectionState
                )

                DeviceSpecificationItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = "MAC ADDRESS",
                    value = uiState.macAddress
                )
                DeviceSpecificationItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = "SSID",
                    value = uiState.ssid
                )
                DeviceSpecificationItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = "IP ADDRESS",
                    value = uiState.ipAddress
                )

                Button(
                    enabled = uiState.connectionState.isConnected(),
                    onClick = {
                        val state = when (uiState.ledState) {
                            LEDCommand.ON -> LEDCommand.OFF
                            else -> LEDCommand.ON
                        }
                        onUpdateLedState(state)
                    }
                ) {
                    Text(text = if (uiState.ledState == LEDCommand.ON) "Turn OFF" else "Turn ON")
                }
            }
        }

        if (openPermissionDeniedDialog) {
            LocationPermissionDeniedDialog(
                onDismissRequest = { openPermissionDeniedDialog = false },
                openSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }

                    context.startActivity(intent)
                    openPermissionDeniedDialog = false
                }
            )
        }

        if (showBottomSheet) {
            WifiBottomSheet(
                modifier = Modifier.statusBarsPadding(),
                sheetState = sheetState,
                wifiScanResult = uiState.wifiScanResult,
                onConnectNetwork = connectToWifiNetwork,
                onDismissRequest = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }
            )
        }
    }
}

@Preview
@Composable
private fun DetailScreenPreview() {
    SimpleBLEClientTheme {
        DetailScreen(
            uiState = DetailUiState(),
            onUpdateLedState = {},
            onConnectDevice = {},
            onConnectToWifi = {},
            connectToWifiNetwork = {},
            onDisconnectDevice = {},
            onNavigateBack = {}
        )
    }
}
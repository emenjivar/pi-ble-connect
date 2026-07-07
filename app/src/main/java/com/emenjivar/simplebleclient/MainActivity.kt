package com.emenjivar.simplebleclient

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.emenjivar.simplebleclient.ui.detail.DetailRoute
import com.emenjivar.simplebleclient.ui.detail.DetailScreen
import com.emenjivar.simplebleclient.ui.main.MainRoute
import com.emenjivar.simplebleclient.ui.main.MainScreen
import com.emenjivar.simplebleclient.ui.main.MainViewModel
import com.emenjivar.simplebleclient.ui.theme.SimpleBLEClientTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val backStack = remember { mutableStateListOf<Any>(MainRoute) }

            SimpleBLEClientTheme {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        entry<MainRoute> {
                            MainScreen(
                                viewModel = viewModel,
                                onRequestBluetoothEnable = { intent ->
                                    enableBluetoothLaunched.launch(intent)
                                },
                                onClickDetail = { model ->
                                    backStack.add(DetailRoute(device = model))
                                }
                            )
                        }
                        entry<DetailRoute> { route ->
                            DetailScreen(
                                route = route,
                                onNavigateBack = {
                                    backStack.removeLastOrNull()
                                }
                            )
                        }
                    }
                )

            }
        }
    }

    private val enableBluetoothLaunched =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    runCatching { viewModel.startScan() }
                }

                RESULT_CANCELED -> {
                    Toast.makeText(
                        applicationContext,
                        "Grant all the permissions to continue", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    override fun onStop() {
        super.onStop()
        viewModel.stopScan()
    }
}

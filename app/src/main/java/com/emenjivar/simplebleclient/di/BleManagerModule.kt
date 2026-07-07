package com.emenjivar.simplebleclient.di

import android.content.Context
import com.emenjivar.simplebleclient.BuildConfig
import com.emenjivar.simplebleclient.ble.BleNotifications
import com.emenjivar.simplebleclient.ble.BleOperationQueue
import com.emenjivar.simplebleclient.ble.BleScanner
import com.emenjivar.simplebleclient.ble.BleScannerImp
import com.emenjivar.simplebleclient.ble.CustomBleManager
import com.emenjivar.simplebleclient.ble.RaspberryBleManager
import com.emenjivar.simplebleclient.ble.mock.MockBleDataSource
import com.emenjivar.simplebleclient.ble.mock.MockBleManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BleManagerModule {

    @Provides
    @Singleton
    fun providesBleScanner(
        @ApplicationContext context: Context
    ): BleScanner = BleScannerImp(context)

    @Provides
    @Singleton
    fun providesMockBleDataSource(): MockBleDataSource = MockBleDataSource()

    /**
     * Picks the BLE implementation from the active product flavor.
     * `raspberry` → real GATT stack, anything else (`mock`) → in-memory fake.
     */
    @Provides
    @Singleton
    fun providesCustomBleManager(
        @ApplicationContext context: Context,
        bleNotifications: BleNotifications,
        bleOperationQueue: BleOperationQueue,
        scanner: BleScanner,
        mockBleDataSource: MockBleDataSource
    ): CustomBleManager = when (BuildConfig.FLAVOR) {
        "raspberry" -> RaspberryBleManager(
            context = context,
            bleNotifications = bleNotifications,
            bleOperationQueue = bleOperationQueue,
            scanner = scanner
        )

        else -> MockBleManager(
            bleNotifications = bleNotifications,
            mockBleDataSource = mockBleDataSource
        )
    }
}

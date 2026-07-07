package com.emenjivar.simplebleclient.di

import com.emenjivar.simplebleclient.ble.BleNotifications
import com.emenjivar.simplebleclient.ble.BleNotificationsImp
import com.emenjivar.simplebleclient.ble.BleOperationQueue
import com.emenjivar.simplebleclient.ble.BleOperationQueueImp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MainModule {

    @Provides
    @Singleton
    fun providesBleNotifications(): BleNotifications = BleNotificationsImp()

    @Provides
    @Singleton
    fun providesBleOperationQueue(): BleOperationQueue = BleOperationQueueImp()
}

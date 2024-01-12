package de.digural.app.di

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.location.LocationManager
import android.os.BatteryManager
import android.os.PowerManager
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.digural.app.auth.AuthManager
import de.digural.app.bluetooth.BluetoothDeviceManager
import de.digural.app.bluetooth.BluetoothScanner
import de.digural.app.database.AppDatabase
import de.digural.app.device.DeviceRepository
import de.digural.app.location.LocationService
import de.digural.app.mqtt.MqttManager
import de.digural.app.mqtt.MqttMessagePersistence
import de.digural.app.permission.PermissionsManager
import de.digural.app.tracking.BackgroundServiceWatcher
import de.digural.app.tracking.TrackingManager
import de.digural.app.update.UpdateManager
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppModule() {

    @Singleton
    @Provides
    fun provideLocationService(): LocationService {
        return LocationService()
    }

    @Singleton
    @Provides
    fun provideDatabase(application: Application): AppDatabase {
        return Room.databaseBuilder(
            application, AppDatabase::class.java, de.digural.app.AppConstants.ROOM_DB_NAME
        ).build()
    }

    @Singleton
    @Provides
    fun provideMqttPersistence(appDatabase: AppDatabase): MqttMessagePersistence {
        return MqttMessagePersistence(appDatabase)
    }

    @Singleton
    @Provides
    fun provideBluetoothAdapter(application: Application): BluetoothAdapter {
        return (application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    @Singleton
    @Provides
    fun provideLocationManager(application: Application): LocationManager {
        return application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    @Singleton
    @Provides
    fun provideFusedLocationProviderClients(application: Application): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(application)
    }

    @Singleton
    @Provides
    fun provideBluetoothScanner(
        application: Application, bluetoothAdapter: BluetoothAdapter
    ): BluetoothScanner {
        return BluetoothScanner(application, bluetoothAdapter)
    }


    @Singleton
    @Provides
    fun provideBluetoothDeviceManager(
        application: Application,
        bluetoothScanner: BluetoothScanner,
        locationService: LocationService,
        deviceRepository: DeviceRepository,
        trackingManager: TrackingManager,
        backgroundServiceWatcher: BackgroundServiceWatcher
    ): BluetoothDeviceManager {
        return BluetoothDeviceManager(
            application,
            bluetoothScanner,
            locationService,
            deviceRepository,
            trackingManager,
            backgroundServiceWatcher
        )
    }

    @Singleton
    @Provides
    fun provideAuthManager(application: Application): AuthManager {
        return AuthManager(application)
    }

    @Singleton
    @Provides
    fun provideMqttManager(
        application: Application,
        mqttMessagePersistence: MqttMessagePersistence,
        authManager: AuthManager
    ): MqttManager {
        return MqttManager(application, mqttMessagePersistence, authManager)
    }

    @Singleton
    @Provides
    fun provideDeviceManager(appDatabase: AppDatabase): DeviceRepository {
        return DeviceRepository(appDatabase)
    }

    @Singleton
    @Provides
    fun provideServiceWatcher(application: Application): BackgroundServiceWatcher {
        return BackgroundServiceWatcher(application)
    }

    @Singleton
    @Provides
    fun provideTrackingManager(
        mqttManager: MqttManager,
        authManager: AuthManager,
        deviceRepository: DeviceRepository,
        powerManager: PowerManager,
        batteryManager: BatteryManager,
        application: Application
    ): TrackingManager {
        return TrackingManager(
            mqttManager, authManager, deviceRepository, powerManager, batteryManager, application
        )
    }

    @Singleton
    @Provides
    fun provideUpdateManager(
        application: Application, appUpdateManager: AppUpdateManager
    ): UpdateManager {
        return UpdateManager(application, appUpdateManager)
    }

    @Singleton
    @Provides
    fun provideAppUpdateManager(
        application: Application
    ): AppUpdateManager {
        return AppUpdateManagerFactory.create(application)
    }

    @Singleton
    @Provides
    fun providePowerManager(
        application: Application
    ): PowerManager {
        return application.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    @Singleton
    @Provides
    fun provideBatteryManager(
        application: Application
    ): BatteryManager {
        return application.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }

    @Singleton
    @Provides
    fun providePermissionsManager(): PermissionsManager {
        return PermissionsManager()
    }

//    @Singleton
//    @Provides
//    fun provideBleConnectionManager(
//        application: Application,
//    ): BleConnectionManager {
//        return BleConnectionManager(application)
//    }

    private fun migrate(): Migration {
        return object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }
    }
}
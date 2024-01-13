package de.digural.app.tracking

import android.annotation.SuppressLint
import android.app.*
import android.app.PendingIntent.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import de.digural.app.AppConstants
import de.digural.app.R
import de.digural.app.auth.AuthManager
import de.digural.app.bluetooth.BluetoothDeviceManager
import de.digural.app.bluetooth.BluetoothScanner
import de.digural.app.bluetooth.event.ConnectToBluetoothDeviceEvent
import de.digural.app.bluetooth.event.StartBleScannerEvent
import de.digural.app.bluetooth.event.StopBleScannerEvent
import de.digural.app.device.DeviceRepository
import de.digural.app.location.LocationService
import de.digural.app.mqtt.MqttManager
import de.digural.app.ui.MainActivity
import de.digural.app.ui.event.StartTrackingServiceEvent
import de.digural.app.ui.event.StopTrackingServiceEvent
import de.digural.app.util.LoggingHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import javax.inject.Inject

@AndroidEntryPoint
class BackgroundService() : Service() {

    private val LOG_TAG: String = BackgroundService::class.java.getName()

    @Inject
    lateinit var locationService: LocationService

    @Inject
    lateinit var bluetoothDeviceManager: BluetoothDeviceManager

    @Inject
    lateinit var mqttManager: MqttManager

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var deviceRepository: DeviceRepository

    @Inject
    lateinit var backgroundServiceWatcher: BackgroundServiceWatcher

    @Inject
    lateinit var trackingManager: TrackingManager

    @Inject
    lateinit var bluetoothScanner: BluetoothScanner

    private lateinit var notificationManager: NotificationManager

    private var mBroadcastReceiver: BroadcastReceiver? = null

    private lateinit var coroutineScope: CoroutineScope

    override fun onBind(p0: Intent?): IBinder? {
        return null;
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(LOG_TAG, "onStartCommand()")
        if (backgroundServiceWatcher.isBackgroundServiceActive.value !== BackgroundServiceState.RUNNING) {
            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            locationService.startLocationUpdates(this)

            coroutineScope.launch {
                merge(
                    bluetoothDeviceManager.hasConnectedDevices(),
                    trackingManager.trackingState,
                    authManager.user
                )
                    .debounce(1000)
                    .collect {
                        // Update notification when any of these changed
                        Log.v(LOG_TAG, "updateNotification()")
                        updateNotification()
                    }
            }

            coroutineScope.launch {
                //Wait for first connected device then register connection lost listener
                bluetoothDeviceManager.hasConnectedDevices().filter { it }.first()
                bluetoothDeviceManager.hasConnectedDevices().filterNot { it }
                    .collect {
                        launch { stopSelf() }
                    }
            }

//        coroutineScope.launch {
//            authManager.user.collect {
//                if (it == null) {
//                    onDestroy()
//                }
//            }
//        }

            startForeground(
                de.digural.app.AppConstants.TRACKING_SERVICE_NOTIFICATION_ID,
                buildNotification("Background service started")
            )

            backgroundServiceWatcher.isBackgroundServiceActive.value =
                BackgroundServiceState.RUNNING
        }

        return START_STICKY
    }

    override fun onCreate() {
        Log.v(LOG_TAG, "onCreate()")
        registerBroadcastReceivers()
        EventBus.getDefault().register(this)
        coroutineScope = CoroutineScope(Dispatchers.IO)
        super.onCreate()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerBroadcastReceivers() {
        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                when (p1?.action) {
                    de.digural.app.AppConstants.TRACKING_NOTIFICATION_STOP_ACTION -> {
                        Log.i(
                            LOG_TAG,
                            de.digural.app.AppConstants.TRACKING_NOTIFICATION_STOP_ACTION
                        )
                        stopSelf()
                    }

                    de.digural.app.AppConstants.FORCE_RECONNECT_ACTION -> {
                        LoggingHelper.logWithCurrentThread(LOG_TAG, "Ble Device found")
//                        bluetoothDeviceManager.forceReconnect()
                    }
                }

            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                mBroadcastReceiver,
                IntentFilter(de.digural.app.AppConstants.TRACKING_NOTIFICATION_STOP_ACTION),
                RECEIVER_EXPORTED
            )

            registerReceiver(
                mBroadcastReceiver,
                IntentFilter(de.digural.app.AppConstants.FORCE_RECONNECT_ACTION),
                RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(
                mBroadcastReceiver,
                IntentFilter(de.digural.app.AppConstants.TRACKING_NOTIFICATION_STOP_ACTION)
            )

            registerReceiver(
                mBroadcastReceiver,
                IntentFilter(de.digural.app.AppConstants.FORCE_RECONNECT_ACTION)
            )
        }
    }

    override fun onDestroy() {
        Log.v(LOG_TAG, "onDestroy()")
        coroutineScope.cancel()
        backgroundServiceWatcher.isBackgroundServiceActive.value =
            BackgroundServiceState.NOT_RUNNING
        EventBus.getDefault().unregister(this)
        locationService.stopLocationUpdates()

        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver)
        }

        bluetoothDeviceManager.reset()

        CoroutineScope(Dispatchers.Default)
            .launch {
                trackingManager.stopTracking()
                stopForeground(true)
                super.onDestroy()
                Log.v(LOG_TAG, "finishedOnDestroy()")
            }
    }

    private suspend fun updateNotification() {
        var trackingStateText = "No data transmission"
        var bluetoothConnectionText = "No device connected"

        if (trackingManager.trackingState.value == TrackingState.TRACKING ||
            trackingManager.trackingState.value == TrackingState.LOCATION_ONLY
        ) {
            trackingStateText = "Transmitting data"
        }

        when (bluetoothDeviceManager.hasConnectedDevices().firstOrNull()) {
            true -> {
                val connectedDevices =
                    deviceRepository.getConnectedDevices().filterNot { it.isEmpty() }
                        .firstOrNull()

                connectedDevices?.let {
                    bluetoothConnectionText = "${connectedDevices.size} connected"

                }
            }

            else -> null //Throws error w/o else branch
        }

        val notificationText = "$bluetoothConnectionText | $trackingStateText"


        val notification = buildNotification(notificationText)
        notificationManager.notify(1001, notification)
    }

    private fun buildNotification(message: String): Notification {
        val intent = Intent(de.digural.app.AppConstants.TRACKING_NOTIFICATION_STOP_ACTION)
        val activityActionPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            this,
            1002,
            intent,
            FLAG_IMMUTABLE
        )

        val forceReconnectIntent = Intent(de.digural.app.AppConstants.FORCE_RECONNECT_ACTION)
        val forceReconnectPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            this,
            1003,
            forceReconnectIntent,
            FLAG_UPDATE_CURRENT or getCompatImmutableFlag()
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                de.digural.app.AppConstants.TRACKING_NOTIFICATION_CHANNEL_ID,
                de.digural.app.AppConstants.TRACKING_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            de.digural.app.AppConstants.TRACKING_NOTIFICATION_CODE,
            notificationIntent,
            getCompatImmutableFlag()
        )

        val notification: Notification =
            NotificationCompat.Builder(
                this,
                de.digural.app.AppConstants.TRACKING_NOTIFICATION_CHANNEL_ID
            )
                .setContentTitle("DIGURAL")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_data_transmission_24)
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .addAction(
                    R.drawable.googleg_standard_color_18,
                    "Stop",
                    activityActionPendingIntent
                )
                .build()
        return notification
    }


    @ExperimentalCoroutinesApi
    @Subscribe
    fun onStartTrackingServiceEvent(event: StartTrackingServiceEvent) {
        coroutineScope.launch {
            trackingManager.startTracking()
        }
    }

    @Subscribe
    fun onStopTrackingServiceEvent(event: StopTrackingServiceEvent) {
        coroutineScope.launch {
            trackingManager.stopTracking()
        }
    }

    @Subscribe
    fun onConnectToBluetoothDevice(event: ConnectToBluetoothDeviceEvent) {
        coroutineScope.launch(Dispatchers.IO) {
            LoggingHelper.logWithCurrentThread(
                LOG_TAG,
                "Received ConnectToBluetoothDeviceEvent: ${event.macAddress} - Retry: ${event.connectWithRetry}"
            )
            if (event.connectWithRetry) {
                bluetoothDeviceManager.connectDeviceWithRetry(event.macAddress, event.deviceType)
            } else {
                bluetoothDeviceManager.connectDevice(event.macAddress, event.deviceType)
            }
        }
    }

    @Subscribe
    fun onStartBleScannerEvent(event: StartBleScannerEvent) {
        LoggingHelper.logWithCurrentThread(LOG_TAG, "onStartBleScannerEvent()")
        bluetoothScanner.startBleScan(event.leScanCallback, event.macAddress)
    }

    @Subscribe
    fun onStopBleScannerEvent(event: StopBleScannerEvent) {
        LoggingHelper.logWithCurrentThread(LOG_TAG, "onStopBleScannerEvent()")
        bluetoothScanner.stopBleScan(event.leScanCallback)
    }

    fun getCompatImmutableFlag(): Int {
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            FLAG_MUTABLE
//        } else {
//            FLAG_IMMUTABLE
//        }
        return FLAG_IMMUTABLE
    }


}
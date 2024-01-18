package de.digural.app.ui

import android.app.Activity
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.digural.app.BuildConfig
import de.digural.app.R
import de.digural.app.auth.AuthManager
import de.digural.app.bluetooth.BluetoothDeviceManager
import de.digural.app.bluetooth.event.ConnectToBluetoothDeviceEvent
import de.digural.app.device.DeviceEntity
import de.digural.app.device.DeviceRepository
import de.digural.app.location.LocationService
import de.digural.app.location.LocationValue
import de.digural.app.location.event.LocationUpdateEvent
import de.digural.app.sensor.SensorValue
import de.digural.app.theming.ThemingManager
import de.digural.app.tracking.BackgroundServiceWatcher
import de.digural.app.tracking.TrackingManager
import de.digural.app.tracking.TrackingState
import de.digural.app.ui.event.*
import de.digural.app.update.UpdateManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val locationManager: LocationService,
    private val backgroundServiceWatcher: BackgroundServiceWatcher,
    private val deviceRepository: DeviceRepository,
    private val bluetoothDeviceManager: BluetoothDeviceManager,
    private val trackingManager: TrackingManager,
    private val application: Application,
    private val updateManager: UpdateManager,
    val themingManager: ThemingManager
) : ViewModel() {
    private val LOG_TAG = HomeViewModel::class.java.name

    private var job: Job? = null

    val location = MutableLiveData<LocationValue?>(locationManager.getCurrentLocation())
    val user = authManager.user.asLiveData()
    val versionInfo =
        if (de.digural.app.BuildConfig.DEBUG) "${de.digural.app.BuildConfig.VERSION_NAME}-dev (${de.digural.app.BuildConfig.VERSION_CODE})" else "${de.digural.app.BuildConfig.VERSION_NAME} (${de.digural.app.BuildConfig.VERSION_CODE})"

    lateinit var trackingEnabled: LiveData<Boolean>
    lateinit var linkedDevices: LiveData<List<DeviceEntity>>
    lateinit var connectedDevices: LiveData<List<DeviceEntity>>
    lateinit var trackingState: LiveData<TrackingState>

    var sensorValues: MutableStateFlow<SensorValue?> = MutableStateFlow(null)
    val selectedDevice: MutableStateFlow<DeviceEntity?> = MutableStateFlow(null)

    init {
        EventBus.getDefault().register(this)

        viewModelScope.launch {
            linkedDevices = deviceRepository.getDevices().asLiveData()
            connectedDevices = deviceRepository.getDevicesShouldBeConnected().asLiveData()
            trackingState = trackingManager.trackingState.asLiveData()

            trackingEnabled =
                deviceRepository.getDevicesShouldBeConnected()
                    .combine(authManager.user) { connectedDevices, user ->
                        Log.v(LOG_TAG, "connectedDevices: ${connectedDevices.size}")
                        Log.v(LOG_TAG, "user: ${user}")
                        connectedDevices.isNotEmpty() && user != null || user?.isTrackOnlyUser() == true
                    }
                    .asLiveData()
        }
    }

    private fun clearData() {
    }

    @ExperimentalCoroutinesApi
    fun onToggleTrackingClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            if (trackingState.value == TrackingState.TRACKING || trackingState.value == TrackingState.LOCATION_ONLY) {
                backgroundServiceWatcher.sendEventToService(StopTrackingServiceEvent())
            } else {
                backgroundServiceWatcher.sendEventToService(StartTrackingServiceEvent())
            }
        }
    }

    fun onBtConnectClicked() {
        if (connectedDevices.value?.isNotEmpty() == true) {
            viewModelScope.launch(Dispatchers.IO) {
                bluetoothDeviceManager.disconnectAllDevices()
            }
        } else {
            if (linkedDevices.value?.isEmpty() == true) {
                EventBus.getDefault().post(NavigationEvent(R.id.devices))
            } else {
                linkedDevices.value?.forEach {
//                    viewModelScope.launch(Dispatchers.IO) {
//                        bluetoothDeviceManager.connectDeviceWithRetry(it._macAddress.value)
//                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        it._macAddress.value?.let { macAddress ->
                            backgroundServiceWatcher.sendEventToService(
                                ConnectToBluetoothDeviceEvent(
                                    macAddress,
                                    it.getBluetoothDeviceType()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocationUpdate(locationUpdateEvent: LocationUpdateEvent) {
        location.value = locationUpdateEvent.location
    }

    fun onUserButtonClicked(activity: FragmentActivity) {
        if (user.value != null) {
            viewModelScope.launch {
                authManager.logout(activity)
            }
        } else {
            viewModelScope.launch {
                try {
                    authManager.login(activity)
                } catch (e: Exception) {
                    authManager.logout(activity)
                    val messageText = e.message ?: "Unknown error"
                    Toast.makeText(
                        application,
                        activity.getString(R.string.home_viewmodel_login_error, messageText),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    }

    fun startLocationUpdates() {
        backgroundServiceWatcher.sendEventToService(StartLocationServiceEvent())
    }

    fun stopLocationUpdates() {
        backgroundServiceWatcher.sendEventToService(StopLocationServiceEvent())
    }

    fun disconnectDevice(device: DeviceEntity) {
        bluetoothDeviceManager.disconnect(device)
    }

    fun onDeviceButtonClicked(device: DeviceEntity) {
        selectedDevice.value = device
    }

    fun checkForUpdates(activity: Activity) {
        viewModelScope.launch {
            val checkUpdateResponse = updateManager.checkForUpdates()
            withContext(Dispatchers.Main) {
                if (checkUpdateResponse.isUpdateAvailable) {
                    try {
                        updateManager.startUpdateFlow(checkUpdateResponse, activity)
                    } catch (e: Exception) {
                        Toast.makeText(activity, e.message, 10000).show()
                    }
                }
            }
        }

    }
}
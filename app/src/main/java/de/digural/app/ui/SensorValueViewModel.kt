package de.digural.app.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.digural.app.bluetooth.BluetoothConnectionState
import de.digural.app.bluetooth.BluetoothDeviceManager
import de.digural.app.bluetooth.event.ConnectToBluetoothDeviceEvent
import de.digural.app.device.DeviceEntity
import de.digural.app.sensor.SensorType
import de.digural.app.sensor.SensorValue
import de.digural.app.tracking.BackgroundServiceWatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SensorValueViewModel @Inject constructor(
    private val bluetoothDeviceManager: BluetoothDeviceManager,
    private val backgroundServiceWatcher: BackgroundServiceWatcher
) : ViewModel() {
    private val LOG_TAG = SensorValueViewModel::class.java.name

    val selectedDevice: MutableStateFlow<DeviceEntity?> = MutableStateFlow(null)
    val clearUiEventFlow: MutableSharedFlow<Boolean> = MutableSharedFlow()
//    val sensorValues: MutableStateFlow<SensorValue?> = MutableStateFlow(null)

    val sensorValueFlows: MutableMap<SensorType, MutableStateFlow<SensorValue?>> = HashMap()

    fun registerUiUpdates() {
        viewModelScope.launch {
            selectedDevice.filterNotNull().collect { selectedDevice ->
                val cachedValuesFlow =
                    selectedDevice.sensorValueCache.entries.asFlow()
                        .map { entry ->
                            Log.v(LOG_TAG, "Cached Entry")
                            entry.value
                        }

                val liveUpdatesFlow = selectedDevice._sensorValues.filterNotNull()

                merge(cachedValuesFlow, liveUpdatesFlow)
                    .collect {
                        getFlowForProperty(it.sensorType).value = it
                    }
            }
        }
    }

    fun getFlowForProperty(sensorType: SensorType): MutableStateFlow<SensorValue?> {
        val flow = sensorValueFlows[sensorType]
        if (flow == null) {
            sensorValueFlows[sensorType] = MutableStateFlow(null)
        }
        return sensorValueFlows[sensorType]!!
    }

    fun onDisconnectClicked() {
        viewModelScope.launch {
            val connectionState = selectedDevice.value?._targetConnectionState?.value
            if (connectionState !== BluetoothConnectionState.DISCONNECTED) {
                selectedDevice.value?._targetConnectionState?.value =
                    BluetoothConnectionState.DISCONNECTED
                selectedDevice.value?.disconnect()
                clearUiEventFlow.emit(true)
            } else {
                selectedDevice.value?._targetConnectionState?.value =
                    BluetoothConnectionState.CONNECTED

                selectedDevice.value?.let { device ->
                    device._macAddress?.value?.let { macAddress ->
                        backgroundServiceWatcher.sendEventToService(
                            ConnectToBluetoothDeviceEvent(
                                macAddress,
                                device.getBluetoothDeviceType()
                            )
                        )
                    }
                }
            }
        }
    }
}
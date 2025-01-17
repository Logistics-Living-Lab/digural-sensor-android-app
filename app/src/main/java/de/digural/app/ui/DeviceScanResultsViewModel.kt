package de.digural.app.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.digural.app.bluetooth.BluetoothDeviceManager
import de.digural.app.bluetooth.BluetoothDeviceType
import de.digural.app.bluetooth.BluetoothScanState
import de.digural.app.bluetooth.BluetoothScanner
import de.digural.app.device.DeviceEntity
import de.digural.app.device.DeviceRepository
import de.digural.app.util.LoggingHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class DeviceScanResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val deviceRepository: DeviceRepository,
    val bluetoothScanner: BluetoothScanner
) : ViewModel() {

    private val LOG_TAG: String = DeviceScanResultsViewModel::class.java.name

    val foundDevices: MutableLiveData<DeviceEntity> = MutableLiveData()
    var scanState: MutableLiveData<BluetoothScanState> = MutableLiveData()
    val useLegacyModeChannel: MutableSharedFlow<Boolean> = MutableSharedFlow()
    private var useLegacyMode: Boolean = false

    private var discoveryJob: Job? = null

    init {
        //Restart Scanning
        viewModelScope.launch {
            useLegacyModeChannel.collect {
                useLegacyMode = it
                LoggingHelper.logWithCurrentThread(LOG_TAG, "Switch $it")
                stopScanning()
                delay(200)
                startScanning()
            }
        }

        viewModelScope.launch {
            bluetoothScanner.bluetoothScanState().collect {
                if (discoveryJob != null && it === BluetoothScanState.NOT_SCANNING) {
                    scanState.value = it
                    discoveryJob?.cancel()
                    discoveryJob = null
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @ExperimentalCoroutinesApi
    fun startScanning() {
        scanState.value = BluetoothScanState.SCANNING
        discoveryJob = viewModelScope.launch(Dispatchers.IO) {
            bluetoothScanner
                .discoverDevices(useLegacyMode)
                .map {
                    Log.i(
                        LOG_TAG,
                        it.address + ": [" + it.name + "] " + getDeviceTypeAsString(it.type)
                    )
                    it
                }
                .filter {
                    deviceRepository.isDeviceSupported(it)
                }
                .filterNot {
                    deviceRepository.containsDeviceAddress(it.address)
                }
                .collect {
                    foundDevices.postValue(DeviceEntity.fromBluetoothDevice(it))
                }
        }
    }

    fun stopScanning() {
        bluetoothScanner.cancelDeviceDiscovery()
    }

    fun onDeviceSelected(device: DeviceEntity) {
        viewModelScope.launch {
            deviceRepository.addDevice(device)
        }
    }

    fun getDeviceTypeAsString(deviceType: Int): String {
        return when (deviceType) {
            1 -> "DEVICE_TYPE_CLASSIC"
            2 -> "DEVICE_TYPE_LE"
            3 -> "DEVICE_TYPE_DUAL"
            else -> "DEVICE_TYPE_UNKNOWN"
        }
    }

    fun getBluetoothDeviceType(bluetoothDevice: BluetoothDevice): BluetoothDeviceType {
        return BluetoothDeviceManager.getDeviceTypeForBluetoothDevice(bluetoothDevice)
    }
}
package de.digural.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import de.digural.app.bluetooth.BluetoothScanner
import de.digural.app.device.DeviceRepository
import de.digural.app.tracking.BackgroundServiceWatcher
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val bluetoothScanner: BluetoothScanner,
    private val backgroundServiceWatcher: BackgroundServiceWatcher,
    private val deviceRepository: DeviceRepository,
    application: Application
) : AndroidViewModel(application) {

    private val LOG_TAG: String = DebugViewModel::class.java.name

    val activeScanJobs = bluetoothScanner.activeScanJobs
    val bluetoothScanScanState = bluetoothScanner.bluetoothScanState().asLiveData()


}
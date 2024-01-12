package de.digural.app.bluetooth.event

import de.digural.app.bluetooth.BluetoothDeviceType

class ConnectToBluetoothDeviceEvent(
    val macAddress: String,
    val deviceType: BluetoothDeviceType,
    val connectWithRetry: Boolean = true
) {

}

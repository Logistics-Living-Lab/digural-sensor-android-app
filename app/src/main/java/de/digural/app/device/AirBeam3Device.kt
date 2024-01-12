package de.digural.app.device

import android.bluetooth.BluetoothDevice
import de.digural.app.DiguralApplication
import de.digural.app.bluetooth.AirBeam3BleConnection
import de.digural.app.bluetooth.BluetoothConnectionState
import de.digural.app.bluetooth.BluetoothDeviceType
import de.digural.app.sensor.AirBeamSensorValueParser
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.BleManager
import org.joda.time.DateTime

class AirBeam3Device(
    macAddress: String,
    targetConnectionState: BluetoothConnectionState = BluetoothConnectionState.NONE
) : BleDevice(macAddress, targetConnectionState) {

    override fun getBleConnection(): BleManager {
        return AirBeam3BleConnection(DiguralApplication.context, object :
            AirBeam3BleConnection.ConnectionListener {
            override fun onDisconnected() {
                this@AirBeam3Device.onDisconnected()
            }

            override fun onDataReceived(data: String, device: BluetoothDevice) {
                coroutineScope.launch {
                    val sensorValue = AirBeamSensorValueParser.parseLine(
                        device.address,
                        getBluetoothDeviceType(),
                        data,
                        DateTime()
                    )

                    sensorValue?.let {
                        _sensorValues.emit(sensorValue)
                    }
                }
            }
        })
    }

    override fun getBluetoothDeviceType(): BluetoothDeviceType {
        return BluetoothDeviceType.AIRBEAM3
    }
}
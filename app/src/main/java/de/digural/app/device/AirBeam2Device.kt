package de.digural.app.device

import android.bluetooth.BluetoothDevice
import de.digural.app.bluetooth.AirBeam3BleConnection
import de.digural.app.bluetooth.BluetoothConnectionState
import de.digural.app.bluetooth.BluetoothDeviceType
import de.digural.app.bluetooth.BluetoothSocketConnection
import de.digural.app.sensor.AirBeamSensorValueParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.joda.time.DateTime

class AirBeam2Device(
    macAddress: String,
    targetConnectionState: BluetoothConnectionState = BluetoothConnectionState.NONE
) : DeviceEntity(macAddress, targetConnectionState) {

    private val LOG_TAG: String = AirBeam2Device::class.java.name

    var socketConnection: BluetoothSocketConnection? = null

    private var bluetoothConnectionJob: Job? = null

    override suspend fun forceReconnect() {
        socketConnection?.simulateConnectionLoss()
    }

    override suspend fun connect() {
        closeConnection()
        bluetoothConnectionJob = coroutineScope.launch(Dispatchers.IO) {
            bluetoothDevice?.let {
                try {
                    socketConnection = BluetoothSocketConnection(
                        it,
                        object : AirBeam3BleConnection.ConnectionListener {
                            override fun onDataReceived(
                                data: String,
                                device: BluetoothDevice
                            ) {
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

                            override fun onDisconnected() {
                                coroutineScope.launch {
                                    this@AirBeam2Device.disconnect()
                                }
                            }

                        })
                    socketConnection?.connect { this@AirBeam2Device.onConnected() }
                    disconnect()
                } catch (e: Exception) {
                    disconnect()
                }
            }
        }
    }

    private fun closeConnection() {
        socketConnection?.closeConnection()
        socketConnection = null
    }

    override suspend fun disconnect() {
        closeConnection()
        onDisconnected()
    }

    override fun getBluetoothDeviceType(): BluetoothDeviceType {
        return BluetoothDeviceType.AIRBEAM2
    }
}
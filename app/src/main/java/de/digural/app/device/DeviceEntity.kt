package de.digural.app.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import de.digural.app.bluetooth.BluetoothConnectionState
import de.digural.app.bluetooth.BluetoothDeviceManager
import de.digural.app.bluetooth.BluetoothDeviceType
import de.digural.app.bluetooth.event.BtDeviceConnectionChangeEvent
import de.digural.app.sensor.SensorType
import de.digural.app.sensor.SensorValue
import de.digural.app.ui.event.SensorValueReceivedEvent
import de.digural.app.util.LoggingHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

@OptIn(ExperimentalCoroutinesApi::class)
abstract class DeviceEntity {
    private val LOG_TAG = DeviceEntity::class.java.name

    val _macAddress: MutableStateFlow<String?> = MutableStateFlow(null)
    val _targetConnectionState: MutableStateFlow<BluetoothConnectionState> =
        MutableStateFlow(BluetoothConnectionState.NONE)
    val _actualConnectionState: MutableStateFlow<BluetoothConnectionState> =
        MutableStateFlow(BluetoothConnectionState.NONE)

    val _sensorValues: MutableSharedFlow<SensorValue?> = MutableSharedFlow<SensorValue?>()
    val _name: MutableStateFlow<String?> = MutableStateFlow(null)

    var bluetoothDevice: BluetoothDevice? = null
    val _changes: Flow<Any?>

    val sensorValueCache = HashMap<SensorType, SensorValue>()

    protected val coroutineScope = CoroutineScope(Dispatchers.Default)

    constructor(
        macAddress: String,
        targetConnectionState: BluetoothConnectionState = BluetoothConnectionState.NONE
    ) {
        val sanitizedMacAddress = macAddress.replace(":", "")
        this._macAddress.value = macAddress
        this._name.value =
            getBluetoothDeviceType().toString() + " " + sanitizedMacAddress.subSequence(
                sanitizedMacAddress.length - 4,
                sanitizedMacAddress.length
            )
        this._targetConnectionState.value = targetConnectionState

        _changes = merge(
            _name,
            _macAddress,
            _targetConnectionState,
            _actualConnectionState,
            _sensorValues
        )
    }

    init {
        coroutineScope.launch {
            _sensorValues.collect {
                LoggingHelper.logWithCurrentThread(
                    LOG_TAG,
                    "Sensor Value Received - ${_macAddress.value}"
                )
                it?.let {
                    sensorValueCache.put(it.sensorType, it)
                }
                EventBus.getDefault().post(SensorValueReceivedEvent(it))
            }
        }

        coroutineScope.launch {
            _actualConnectionState.collect {
                EventBus.getDefault().post(BtDeviceConnectionChangeEvent(this@DeviceEntity))
            }
        }
    }

    abstract suspend fun forceReconnect()
    abstract suspend fun connect()
    abstract suspend fun disconnect()
    abstract fun getBluetoothDeviceType(): BluetoothDeviceType

    protected fun onConnected() {
        this._actualConnectionState.value = BluetoothConnectionState.CONNECTED
    }

    protected fun onDisconnected() {
        this._actualConnectionState.value = BluetoothConnectionState.DISCONNECTED
    }

    protected fun isAutoConnecting(): Flow<Boolean> {
        return merge(
            this._actualConnectionState,
            this._targetConnectionState
        ).map {
            val targetState = this._targetConnectionState.value
            val actualState = this._actualConnectionState.value
            targetState === BluetoothConnectionState.CONNECTED && actualState !== targetState
        }
    }

    @SuppressLint("MissingPermission")
    companion object {
        fun fromBluetoothDevice(bluetoothDevice: BluetoothDevice): DeviceEntity {
            var deviceEntity: DeviceEntity
            val bluetoothDeviceType =
                BluetoothDeviceManager.getDeviceTypeForBluetoothDevice(bluetoothDevice)

            when (bluetoothDeviceType) {
                BluetoothDeviceType.AIRBEAM2 -> deviceEntity = AirBeam2Device(
                    bluetoothDevice.address
                )
                BluetoothDeviceType.AIRBEAM3 -> deviceEntity = AirBeam3Device(
                    bluetoothDevice.address
                )
                BluetoothDeviceType.RUUVI_TAG -> deviceEntity = RuuviTagDevice(
                    bluetoothDevice.address
                )
                else -> throw Exception("Unknown device type: $bluetoothDeviceType")
            }

            deviceEntity.bluetoothDevice = bluetoothDevice
            return deviceEntity
        }

        fun fromDeviceRecord(deviceRecord: DeviceRecord): DeviceEntity {
            var deviceEntity: DeviceEntity = when (deviceRecord.bluetoothDeviceType) {
                BluetoothDeviceType.AIRBEAM3 ->
                    AirBeam3Device(deviceRecord.macAddress)
                BluetoothDeviceType.RUUVI_TAG ->
                    RuuviTagDevice(deviceRecord.macAddress)
                BluetoothDeviceType.AIRBEAM2 ->
                    AirBeam2Device(deviceRecord.macAddress)
                else ->
                    throw Exception("Unknown device type: ${deviceRecord.bluetoothDeviceType}")
            }
            deviceEntity._actualConnectionState.value = BluetoothConnectionState.DISCONNECTED
            deviceEntity._targetConnectionState.value = deviceRecord.targetConnectionState
            return deviceEntity
        }

    }
}
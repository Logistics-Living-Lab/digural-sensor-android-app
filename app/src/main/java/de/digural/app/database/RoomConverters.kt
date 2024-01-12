package de.digural.app.database

import androidx.room.TypeConverter
import de.digural.app.bluetooth.BluetoothConnectionState

class RoomConverters {

    @TypeConverter
    fun fromConnectionStateEnum(value: BluetoothConnectionState): String {
        return value.name
    }

    @TypeConverter
    fun toDeviceConnectionState(name: String): BluetoothConnectionState {
        return BluetoothConnectionState.valueOf(name)
    }
}
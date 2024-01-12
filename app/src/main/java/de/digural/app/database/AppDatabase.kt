package de.digural.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.digural.app.device.DeviceDao
import de.digural.app.device.DeviceRecord
import de.digural.app.mqtt.MqttMessageDao
import de.digural.app.mqtt.PersistentMqttMessage

@Database(
    entities = arrayOf(DeviceRecord::class, PersistentMqttMessage::class),
    version = 13,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun mqttMessageDao(): MqttMessageDao
}
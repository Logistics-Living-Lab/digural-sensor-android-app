package de.digural.app.bluetooth

import androidx.annotation.StringRes
import de.digural.app.R


enum class BluetoothConnectionState(private val strId: Int) {
    CONNECTED(R.string.bluetooth_connection_state_connected),
    CONNECTING(R.string.bluetooth_connection_state_connecting),
    RECONNECTING(R.string.bluetooth_connection_state_reconnecting),
    DISCONNECTED(R.string.bluetooth_connection_state_disconnected),
    NONE(R.string.bluetooth_connection_state_none);

    @StringRes
    open fun getStringId(): Int {
        return strId
    }
}
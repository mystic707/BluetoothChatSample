package com.mysticagit.bluetooth.sample

import android.bluetooth.BluetoothSocket
import android.content.IntentFilter
import java.util.*

object SampleDataManager {

    const val logTag = "mysticagit"
    val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    data class PairableDeviceInfo(
        var deviceName: String?,
        var deviceAddress: String?)

    enum class SelectionState(val value: Int) {
        Close(0),
        Selected(1),
    }


    interface RegisterReceiverListener {

        fun onRegister(filter: IntentFilter)
    }
    interface DeviceListSelectionListener {

        fun onSelection(state: SelectionState, deviceInfo: PairableDeviceInfo?)
    }

    interface BluetoothReadMessageListener {

        fun onReadMessage(message: String?)
    }

    interface BluetoothConnectionStateListener {

        fun onState(state: BTConnectManager.ConnectionState, additionalInfo: String?)
    }
}
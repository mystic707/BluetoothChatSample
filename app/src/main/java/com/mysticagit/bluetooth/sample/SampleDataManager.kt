package com.mysticagit.bluetooth.sample

import android.bluetooth.BluetoothSocket
import android.content.IntentFilter
import java.util.*

object SampleDataManager {

    const val logTag = "mysticagit"
//    val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")  // SerialPortServiceClass_UUID (포럼들에서 연결되었다는 값)
//    val myUUID: UUID = UUID.fromString("00000000-0000-1000-8000-00805F9B34FB")
//    val myUUID: UUID = UUID.fromString("0000112f-0000-1000-8000-00805f9b34fb")    // connectable
//    val myUUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")    // 유투브 샘플 영상에서 사용한 UUID
    val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
//val myUUID: UUID = UUID.fromString("26b7d1da-08c7-4505-a6d1-2459987e5e2d")


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
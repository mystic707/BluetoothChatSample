package com.mysticagit.bluetooth.sample

object SampleManager {

    const val logTag = "mysticagit"

    data class PairableDeviceInfo(
        var deviceName: String?,
        var deviceAddress: String?)

    interface DeviceListSelectionListener {

        fun onSelection(deviceInfo: PairableDeviceInfo)
    }
}
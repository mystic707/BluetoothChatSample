package com.mysticagit.bluetooth.sample

import android.app.ActionBar.LayoutParams
import android.app.Dialog
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class DeviceListDialog : Dialog {

    private lateinit var context: Context
    private lateinit var pairedDeviceList: ArrayList<SampleManager.PairableDeviceInfo>
    private lateinit var selectionListener: SampleManager.DeviceListSelectionListener
    private var newDeviceList: ArrayList<SampleManager.PairableDeviceInfo>? = null

    constructor(
        context: Context,
        deviceList: ArrayList<SampleManager.PairableDeviceInfo>,
        listener: SampleManager.DeviceListSelectionListener)
            : super(context, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen) {

        this.context = context
        this.pairedDeviceList = deviceList
        this.selectionListener = listener
        newDeviceList = ArrayList<SampleManager.PairableDeviceInfo>()
    }


    fun createDialogUI() {

        var btnPairedListExit = findViewById<Button>(R.id.bt_paired_list_close)
        var btnNewListExit = findViewById<Button>(R.id.bt_new_list_close)

        btnPairedListExit.setOnClickListener {
            dismiss()
        }
        btnNewListExit.setOnClickListener {
            dismiss()
        }


        var baseLayout: LinearLayout = findViewById(R.id.layout_pairable_devicelist)
        var btLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        // 페어링되었던 다바이스 정보 업데이트
        if(pairedDeviceList.size > 0) {

            // 타이틀에 디바이스 수 표시
            var title: TextView = findViewById(R.id.tv_pairable_device_num)
            title.text = ("(" + pairedDeviceList.size.toString() + ")")

            // 디바이스별 버튼 추가
            for(deviceInfo in pairedDeviceList) {

                var button: Button = Button(context)
                button.text = deviceInfo.deviceName + " / " + deviceInfo.deviceAddress

                button.setOnClickListener {
                    selectionListener.onSelection(deviceInfo)
                    this.dismiss()
                }

                baseLayout.addView(button, btLayoutParams)
            }
        }
    }

    fun reDrawNewDeviceListUI(deviceInfo: SampleManager.PairableDeviceInfo?) {
        var baseLayout: LinearLayout = findViewById(R.id.layout_new_devicelist)
        var btLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        // 신규 연결 가능한 디바이스 정보 업데이트
        deviceInfo?.let {
            newDeviceList?.add(it)

            // 타이틀에 디바이스 수 표시
            var title: TextView = findViewById(R.id.tv_new_device_num)
            title.text = ("(" + newDeviceList?.size.toString() + ")")

            // 디바이스별 버튼 추가
            var button: Button = Button(context)
            button.text = it.deviceName + " / " + it.deviceAddress

            button.setOnClickListener {
                selectionListener.onSelection(deviceInfo)
                this.dismiss()
            }

            baseLayout.addView(button, btLayoutParams)
        }
    }

    override fun dismiss() {
        newDeviceList?.clear()      // 신규 다바이스 목록 초기화
        super.dismiss()
    }
}
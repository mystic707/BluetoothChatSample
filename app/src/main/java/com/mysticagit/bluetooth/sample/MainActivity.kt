package com.mysticagit.bluetooth.sample

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    var dialog: DeviceListDialog? = null
    var messageHistory: String? = ""

    companion object {
        lateinit var context: Context
    }

    var handler: Handler = Handler(object : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            var sendMessage: String? = msg.obj.toString()

            var tvMessageList: TextView = findViewById(R.id.tv_message_list)

            messageHistory += (sendMessage + "\n")
            tvMessageList.text = messageHistory

            return false
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = this

        // UI 설정
        initUI()
    }
    protected override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(deviceInfoReceiver)

        BTConnectManager.stopEverything()
    }

    /**
     * UI 설정
     */
    private fun initUI() {
        var btnRequestPermission = findViewById<Button>(R.id.btn_request_permission)
        var btnEnableBluetooth = findViewById<Button>(R.id.btn_enable_bluetooth)
        var btnConnectBluetoothDevice = findViewById<Button>(R.id.btn_find_connectable_device)
        var btnCheckConnection = findViewById<Button>(R.id.btn_check_connection)
        var btnSendMessage = findViewById<Button>(R.id.btn_send_message)
        var tvSendMessageText: TextView = findViewById(R.id.tv_send_message)
        var tvMessageList: TextView = findViewById(R.id.tv_message_list)

        // 권한 요청
        btnRequestPermission.setOnClickListener {
            getPermissions()
        }

        // 블루투스 활성
        btnEnableBluetooth.setOnClickListener {
            enableBluetooth()
        }

        // 블루투스 연결 가능한 기기 찾기, 디아비스 목록 팝업 노출
        btnConnectBluetoothDevice.setOnClickListener {
            showDeviceListDialog(findPairableBluetoothDevice())
            registerDeviceFinder()
        }

        // 블루투스 연결 상태 확인
        btnCheckConnection.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {

                // nothing to do

            } else {
////                if(BTConnectManager.bluetoothSocket != null && BTConnectManager.connectedDevice != null) {
////                    Toast.makeText(this, "Connection : " + BTConnectManager.bluetoothSocket?.isConnected + "\nDevice : " + connectedDevice?.name, Toast.LENGTH_SHORT).show()
////                } else {
////                    Toast.makeText(this, "Connection : bluetoothSocket is null", Toast.LENGTH_SHORT).show()
////                }
//            }
                when (BTConnectManager.connectionState) {
                    BTConnectManager.ConnectionState.None -> {
                        Toast.makeText(this, "Not Connect", Toast.LENGTH_SHORT).show()
                    }
                    BTConnectManager.ConnectionState.Listen -> {
                        Toast.makeText(this, "Not Connect (state : Listening)", Toast.LENGTH_SHORT)
                            .show()
                    }
                    BTConnectManager.ConnectionState.Connecting -> {
                        Toast.makeText(this, "Connecting..", Toast.LENGTH_SHORT).show()
                    }
                    BTConnectManager.ConnectionState.Connected -> {
                        Toast.makeText(
                            this,
                            "Connected : ${BTConnectManager.connectedDevice?.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // 메시지 보내기
        btnSendMessage.setOnClickListener {
//            BTConnectManager.connectedThread?.let {
//                it.sendMessage(tvSendMessageText.text.toString())
//
//
//            }
            BTConnectManager.sendMessage(tvSendMessageText.text.toString())

            messageHistory += (tvSendMessageText.text.toString() + "\n")
            tvMessageList.text = messageHistory
        }

        // 블루투스 메시지 수신 시 read 리스너 등록
        BTConnectManager.setBluetoothReadListener(btReadListener)
        //
        BTConnectManager.setBluetoothStateListener(btStateListener)
    }

    private val btReadListener: SampleDataManager.BluetoothReadMessageListener = object : SampleDataManager.BluetoothReadMessageListener {
        override fun onReadMessage(message: String?) {
//            var tvMessageList: TextView = findViewById(R.id.tv_message_list)
//
//            messageHistory += (message + "\n")
//            tvMessageList.text = messageHistory

            handler.obtainMessage(1, -1, -1, message).sendToTarget()
        }
    }

    private val btStateListener: SampleDataManager.BluetoothConnectionStateListener = object : SampleDataManager.BluetoothConnectionStateListener {
        override fun onState(state: BTConnectManager.ConnectionState, additionalInfo: String?) {
            var tvConnectionState: TextView = findViewById(R.id.tv_connection_state)
            when(state) {
                BTConnectManager.ConnectionState.None -> {
                    tvConnectionState.text = "None"
                }
                BTConnectManager.ConnectionState.Listen -> {
                    tvConnectionState.text = "Listen"
                }
                BTConnectManager.ConnectionState.Connecting -> {
                    tvConnectionState.text = "Connecting"
                }
                BTConnectManager.ConnectionState.Connected -> {
                    tvConnectionState.text = "Connected"
                }
            }
        }
    }

    /**
     * 권한 요청
     *
     * 위치 권한이 있어야 블루투스 주변 장치 검색이 가능
     * ref : https://developer.android.com/training/location/permissions?hl=ko
     */
    private fun getPermissions() {
        // 블루투스 기능 사용 시 아래 권한을 모두 요청
        locationPermissionRequest.launch(arrayOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_ADVERTISE))
    }

    // ActivityResult 설정 (about permission)
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                Log.d(SampleDataManager.logTag, "getPermissions, access fine location access granted.")
            }
            permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Log.d(SampleDataManager.logTag, "getPermissions, access coarse location access granted.")
            }
            permissions.getOrDefault(android.Manifest.permission.BLUETOOTH_SCAN, false) -> {
                Log.d(SampleDataManager.logTag, "getPermissions, bluetooth scan access granted.")
            }
            permissions.getOrDefault(android.Manifest.permission.BLUETOOTH_CONNECT, false) -> {
                Log.d(SampleDataManager.logTag, "getPermissions, bluetooth connect access granted.")
            }
            permissions.getOrDefault(android.Manifest.permission.BLUETOOTH_ADVERTISE, false) -> {
                Log.d(SampleDataManager.logTag, "getPermissions, bluetooth advertise access granted.")
            }
            else -> {
                Log.d(SampleDataManager.logTag, "getPermissions, No permissions access granted.")
            }
        }
    }

    /**
     * 블루투스 활성화
     *
     * 블루투스 어댑터 초기화를 위해 호출되어야 한다.
     */
    private fun enableBluetooth () {
        BTConnectManager.initBluetoothAdapter()

//        startActivity(BTConnectManager.getBluetoothIntent())
        
        var enableBluetoothIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        enableBluetoothIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // nothing to do
        } else {
            startActivity(enableBluetoothIntent)

            Toast.makeText(this, "enable Bluetooth", Toast.LENGTH_SHORT).show()

//            var enableBluetoothIntent2: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            launcher.launch(enableBluetoothIntent2)
        }
    }

    // ActivityResult 설정 (about bluetooth)
//    private val launcher: ActivityResultLauncher<Intent> = registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { activityResult ->
//        when (activityResult.resultCode) {
//            RESULT_OK -> {
//                Log.d(SampleDataManager.logTag, "enableBluetooth, bluetooth activityResult.resultCode : RESULT_OK")
//            }
//            else -> {
//                Log.d(SampleDataManager.logTag, "enableBluetooth, bluetooth activityResult.resultCode : ${activityResult.resultCode}")
//            }
//        }
//    }

    /**
     * 연결 가능한 다비이스 정보 획득
     *
     * 이전에 이미 페어링 하였던 단말 목록을 반환
     */
    private fun findPairableBluetoothDevice(): ArrayList<SampleDataManager.PairableDeviceInfo> {
        var pairableDeviceList = ArrayList<SampleDataManager.PairableDeviceInfo>()
        var bluetoothDevice: Set<BluetoothDevice>

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "need to request bluetooth permissions", Toast.LENGTH_SHORT).show()
        }
        else {
            BTConnectManager.bluetoothAdapter?.let {
                bluetoothDevice = it.bondedDevices as Set<BluetoothDevice>  // bondedDevices : 페어링된 기계

                if(bluetoothDevice.isNotEmpty()) {
                    pairableDeviceList.clear()

                    for(device in bluetoothDevice) {
                        var deviceInfo = SampleDataManager.PairableDeviceInfo(device.name, device.address)
                        pairableDeviceList.add(deviceInfo)
                    }
                }
                else {
                    Toast.makeText(this, "not found bluetooth deivce", Toast.LENGTH_SHORT).show()
                }
            } ?: {
                Toast.makeText(this, "need to enable Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }

        Log.d(SampleDataManager.logTag, "findPairableBluetoothDevice, device size : " + pairableDeviceList.size)
        return pairableDeviceList
    }

    /**
     * 페어링된 기기 목록 다이얼로그 노출
     */
    fun showDeviceListDialog(deviceList: ArrayList<SampleDataManager.PairableDeviceInfo>) {
        dialog = DeviceListDialog(this, deviceList, object : SampleDataManager.DeviceListSelectionListener {
            override fun onSelection(state: SampleDataManager.SelectionState, deviceInfo: SampleDataManager.PairableDeviceInfo?) {
                when(state) {
                    SampleDataManager.SelectionState.Close -> {
                        Log.d(SampleDataManager.logTag, "showDeviceListDialog, not select device")
                    }
                    SampleDataManager.SelectionState.Selected -> {
                        connectBluetoothDevice(deviceInfo)
                    }
                }
            }
        })
        dialog?.setContentView(applicationContext.resources.getIdentifier("devicelist_dialog",
            "layout", applicationContext.packageName))
        dialog?.createDialogUI()
        dialog?.show()
    }

    /**
     * 연결 단말 다이얼로그에서 선택한 단말 정보로 연결 시도
     */
    private fun connectBluetoothDevice(deviceInfo: SampleDataManager.PairableDeviceInfo?) {
        var targetDeviceAddress: String? =
            if(deviceInfo != null) { deviceInfo.deviceAddress } else ""

        if(!targetDeviceAddress.isNullOrEmpty()) {
            BTConnectManager.connectedDevice = BTConnectManager.bluetoothAdapter?.getRemoteDevice(targetDeviceAddress)

            if(BTConnectManager.connectedDevice != null) {
                try {
                    BTConnectManager.connect()
                } catch (e: Exception) {
                    Log.d(SampleDataManager.logTag, "connectBluetoothDevice, exception : $e")
                }
            }
        }
    }

    private fun registerDeviceFinder() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {

            Log.d(SampleDataManager.logTag, "registerDeviceFinder, not have permission")
            return
        } else {
            BTConnectManager.bluetoothAdapter?.let {
                if(it.isDiscovering) {
                    it.cancelDiscovery()
                }
                else {
                    if(it.isEnabled) {
                        it.startDiscovery()     // 검색 시작

                        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                        registerReceiver(deviceInfoReceiver, filter)    // 리시버 등록

                    } else {
                        Log.d(SampleDataManager.logTag, "registerDeviceFinder, need to enable bluetoothAdapter")
                    }
                }
            }
        }
    }

    private val deviceInfoReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var action = intent?.action

            if(BluetoothDevice.ACTION_FOUND.equals(action)) {

                var device: BluetoothDevice?
                if(Build.VERSION.SDK_INT >= 33) {
                    device = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    device = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                if (ActivityCompat.checkSelfPermission(
                        MainActivity.context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    //
                }
                if(device?.bondState != BluetoothDevice.BOND_BONDED) {
                    var newDeviceInfo = getPairableDeviceInfo(device)

                    dialog?.let {
                        if(it.isShowing) {
                            it.reDrawNewDeviceListUI(newDeviceInfo)
                        }
                    }
                }
            }
        }
    }

    private fun getPairableDeviceInfo(device: BluetoothDevice?): SampleDataManager.PairableDeviceInfo? {
        return if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {

            null
        } else {
            SampleDataManager.PairableDeviceInfo(device?.name, device?.address)
        }
    }

}



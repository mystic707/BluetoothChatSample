package com.mysticagit.bluetooth.sample

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    var dialog: DeviceListDialog? = null
    var messageHistory: String? = ""

    companion object {
        lateinit var context: Context
    }

    // chat 메시지 리스트 갱신을 위한 handler
    var uiHandler: Handler = Handler(object : Handler.Callback {
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

        // 메시지 보내기
        btnSendMessage.setOnClickListener {
            BTConnectManager.sendMessage(tvSendMessageText.text.toString())

            messageHistory += (tvSendMessageText.text.toString() + "\n")
            tvMessageList.text = messageHistory
        }

        // 블루투스 메시지 수신 시 read 리스너 등록
        BTConnectManager.setBluetoothReadListener(btReadListener)
        // 블루투스 연결 상태 리스너 등록
        BTConnectManager.setBluetoothStateListener(btStateListener)
    }

    private val btReadListener: SampleDataManager.BluetoothReadMessageListener = object : SampleDataManager.BluetoothReadMessageListener {
        override fun onReadMessage(message: String?) {

            uiHandler.obtainMessage(1, -1, -1, message).sendToTarget()
            // 현재 구조에선 handler를 통하지 않고 UI 변경 시 일반 Thread 내에서 UI 변경 요청이 되어 exception 발생함
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
        permissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE))
    }

    // ActivityResult 설정 (about permission)
    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                Log.d(SampleDataManager.logTag, "getPermissions, access fine location access granted.")
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Log.d(SampleDataManager.logTag, "getPermissions, access coarse location access granted.")
            }
            permissions.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false) -> {
                Log.d(SampleDataManager.logTag, "getPermissions, bluetooth scan access granted.")
            }
            permissions.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false) -> {
                Log.d(SampleDataManager.logTag, "getPermissions, bluetooth connect access granted.")
            }
            permissions.getOrDefault(Manifest.permission.BLUETOOTH_ADVERTISE, false) -> {
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

        startActivity(BTConnectManager.getBluetoothIntent())

        Toast.makeText(this, "enable Bluetooth", Toast.LENGTH_SHORT).show()
    }

    /**
     * 연결 가능한 다비이스 정보 획득
     *
     * 이전에 이미 페어링 하였던 단말 목록을 반환
     */
    private fun findPairableBluetoothDevice(): ArrayList<SampleDataManager.PairableDeviceInfo> {

        return BTConnectManager.getPairableBluetoothDevice()
    }

    /**
     * 페어링된 기기 목록 다이얼로그 노출
     */
    private fun showDeviceListDialog(deviceList: ArrayList<SampleDataManager.PairableDeviceInfo>) {
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
     * 선택한 단말 정보로 연결 시도
     */
    private fun connectBluetoothDevice(deviceInfo: SampleDataManager.PairableDeviceInfo?) {
        deviceInfo?.let {
            BTConnectManager.startConnectWithDeviceInfo(it)
        }
    }

    /**
     * 근처에 있는 블루투스 디바이스 검색 요청
     */
    private fun registerDeviceFinder() {
        BTConnectManager.getBluetoothFinderFilter()?.let {
            registerReceiver(deviceInfoReceiver, it)
        }
    }

    private val deviceInfoReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var action = intent?.action

            if(BluetoothDevice.ACTION_FOUND == action) {

                var device: BluetoothDevice?
                if(Build.VERSION.SDK_INT >= 33) {
                    device = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    device = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                if (ActivityCompat.checkSelfPermission(MainActivity.context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED)
                {
                    // nothing to do
                }
                else {
                    if (device?.bondState != BluetoothDevice.BOND_BONDED) {
                        dialog?.let {
                            if (it.isShowing) {
                                // 찾은 디바이스 정보로 UI 리스트 갱신하여 노출
                                it.reDrawNewDeviceListUI(SampleDataManager.PairableDeviceInfo(device?.name, device?.address))
                            }
                        }
                    }
                }

            }
        }
    }

    // end of class
}



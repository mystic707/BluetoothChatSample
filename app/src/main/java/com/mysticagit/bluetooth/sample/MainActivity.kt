package com.mysticagit.bluetooth.sample

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    var bluetoothAdapter: BluetoothAdapter? = null
    var dialog: DeviceListDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI 설정
        initUI()
    }

    /**
     * UI 설정
     */
    private fun initUI() {
        var btnRequestPermission = findViewById<Button>(R.id.btn_request_permission)
        var btnEnableBluetooth = findViewById<Button>(R.id.btn_enable_bluetooth)
        var btnConnectBluetoothDevice = findViewById<Button>(R.id.btn_find_connectable_device)

        btnRequestPermission.setOnClickListener {
            getPermissions()
        }

        btnEnableBluetooth.setOnClickListener {
            enableBluetooth()
        }

        btnConnectBluetoothDevice.setOnClickListener {
            showDeviceListDialog(findPairableBluetoothDevice())
            registerDeviceFinder()
        }
    }

    /**
     * 권한 요청
     *
     * 위치 권한이 있어야 블루투스 주변 장치 검색이 가능
     * ref : https://developer.android.com/training/location/permissions?hl=ko
     */
    private fun getPermissions() {
        // 블루투스 기능 사용 시 아래 4개 권한을 모두 요청
        locationPermissionRequest.launch(arrayOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT))
    }

    // ActivityResult 설정 (about permission)
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                Log.d(SampleManager.logTag, "getPermissions, access fine location access granted.")
            }
            permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Log.d(SampleManager.logTag, "getPermissions, access coarse location access granted.")
            }
            permissions.getOrDefault(android.Manifest.permission.BLUETOOTH_SCAN, false) -> {
                Log.d(SampleManager.logTag, "getPermissions, bluetooth scan access granted.")
            }
            permissions.getOrDefault(android.Manifest.permission.BLUETOOTH_CONNECT, false) -> {
                Log.d(SampleManager.logTag, "getPermissions, bluetooth connect access granted.")
            }
            else -> {
                Log.d(SampleManager.logTag, "getPermissions, No permissions access granted.")
            }
        }
    }

    /**
     * 블루투스 활성화
     *
     * 블루투스 어댑터 초기화를 위해 호출되어야 한다.
     */
    private fun enableBluetooth () {
        var bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if(bluetoothAdapter?.isEnabled == false) {
            var enableBluetoothIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            launcher.launch(enableBluetoothIntent)
        }

        Toast.makeText(this, "enable Bluetooth", Toast.LENGTH_SHORT).show()
    }

    // ActivityResult 설정 (about bluetooth)
    private val launcher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        when (activityResult.resultCode) {
            RESULT_OK -> {
                Log.d(SampleManager.logTag, "enableBluetooth, bluetooth activityResult.resultCode : RESULT_OK")
            }
            else -> {
                Log.d(SampleManager.logTag, "enableBluetooth, bluetooth activityResult.resultCode : ${activityResult.resultCode}")
            }
        }
    }

    /**
     * 연결 가능한 다비이스 정보 획득
     *
     * 이전에 이미 페어링 하였던 단말 목록을 반환
     */
    private fun findPairableBluetoothDevice(): ArrayList<SampleManager.PairableDeviceInfo> {
        var pairableDeviceList = ArrayList<SampleManager.PairableDeviceInfo>()
        var bluetoothDevice: Set<BluetoothDevice>

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "need to request bluetooth permissions", Toast.LENGTH_SHORT).show()
        }
        else {
            bluetoothAdapter?.let {
                bluetoothDevice = it.bondedDevices as Set<BluetoothDevice>  // bondedDevices : 페어링된 기계

                if(bluetoothDevice.isNotEmpty()) {
                    pairableDeviceList.clear()

                    for(device in bluetoothDevice) {
                        var deviceInfo = SampleManager.PairableDeviceInfo(device.name, device.address)
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

        Log.d(SampleManager.logTag, "findPairableBluetoothDevice, device size : " + pairableDeviceList.size)
        return pairableDeviceList
    }

    /**
     * 페어링된 기기 목록 다이얼로그 노출
     */
    fun showDeviceListDialog(deviceList: ArrayList<SampleManager.PairableDeviceInfo>) {
        dialog = DeviceListDialog(this, deviceList, object : SampleManager.DeviceListSelectionListener {
            override fun onSelection(deviceInfo: SampleManager.PairableDeviceInfo) {
                // TODO :
            }
        })
        dialog?.setContentView(applicationContext.resources.getIdentifier("devicelist_dialog",
            "layout", applicationContext.packageName))
        dialog?.createDialogUI()
        dialog?.show()
    }

    // TODO :
    private fun connectBluetoothDevice() {
        //
    }

    private fun registerDeviceFinder() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {

            Log.d(SampleManager.logTag, "registerDeviceFinder, not have permission")
            return
        } else {
            bluetoothAdapter?.let {
                if(it.isDiscovering) {
                    it.cancelDiscovery()
                }
                else {
                    if(it.isEnabled) {
                        it.startDiscovery()     // 검색 시작

                        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                        registerReceiver(deviceInfoReceiver, filter)    // 리시버 등록
                    } else {
                        Log.d(SampleManager.logTag, "registerDeviceFinder, need to enable bluetoothAdapter")
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

                var newDeviceInfo = getPairableDeviceInfo(device)

                dialog?.let {
                    if(it.isShowing) {
                        it.reDrawNewDeviceListUI(newDeviceInfo)
                    }
                }

            }
        }
    }

    private fun getPairableDeviceInfo(device: BluetoothDevice?): SampleManager.PairableDeviceInfo? {
        return if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {

            null
        } else {
            SampleManager.PairableDeviceInfo(device?.name, device?.address)
        }
    }

    protected override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(deviceInfoReceiver)
    }
}



package com.mysticagit.bluetooth.sample

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.InputStream
import java.io.OutputStream

object BTConnectManager {

    var bluetoothAdapter: BluetoothAdapter? = null
    var connectedDevice: BluetoothDevice? = null

    var connectThread: ConnectThread? = null
    var connectedThread: ConnectedThread? = null
    var acceptThread: AcceptThread? = null

    private var btReadListener: SampleDataManager.BluetoothReadMessageListener? = null
    private var btStateListener: SampleDataManager.BluetoothConnectionStateListener? = null

    enum class ConnectionState(val value: Int) {
        None(0),
        Listen(1),
        Connecting(2),
        Connected(3),
    }
    var connectionState: ConnectionState = ConnectionState.None


    /**
     * 리스너 설정
     */
    fun setBluetoothReadListener(listener: SampleDataManager.BluetoothReadMessageListener) {
        btReadListener = listener
    }

    fun setBluetoothStateListener(listener: SampleDataManager.BluetoothConnectionStateListener) {
        btStateListener = listener
    }

    fun initBluetoothAdapter() {
        var bluetoothManager = MainActivity.context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }


    /**
     *
     */
    fun getBluetoothIntent(): Intent {
        var enableBluetoothIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        // 300초 동안 검색 허용
        enableBluetoothIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)

        return enableBluetoothIntent
    }

    fun startConnectWithDeviceInfo(deviceInfo: SampleDataManager.PairableDeviceInfo) {
        deviceInfo.deviceAddress?.let {
            connectedDevice = bluetoothAdapter?.getRemoteDevice(it)

            connect()
        }
    }

    fun getPairableBluetoothDevice(): ArrayList<SampleDataManager.PairableDeviceInfo> {
        var pairableDeviceList = ArrayList<SampleDataManager.PairableDeviceInfo>()
        var bluetoothDevice: Set<BluetoothDevice>

        if (ActivityCompat.checkSelfPermission(MainActivity.context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {

            Log.d(SampleDataManager.logTag, "getPairableBluetoothDevice, need to request bluetooth permissions")
        }
        else {
            bluetoothAdapter?.let {
                bluetoothDevice = it.bondedDevices as Set<BluetoothDevice>  // bondedDevices : 페어링된 기계

                if(bluetoothDevice.isNotEmpty()) {
                    pairableDeviceList.clear()

                    for(device in bluetoothDevice) {
                        var deviceInfo = SampleDataManager.PairableDeviceInfo(device.name, device.address)
                        pairableDeviceList.add(deviceInfo)
                    }
                }
                else {
                    Log.d(SampleDataManager.logTag, "getPairableBluetoothDevice, not found bluetooth deivce")
                }
            } ?: {
                Log.d(SampleDataManager.logTag, "getPairableBluetoothDevice, need to enable Bluetooth")
            }
        }

        Log.d(SampleDataManager.logTag, "findPairableBluetoothDevice, device size : " + pairableDeviceList.size)
        return pairableDeviceList
    }

    fun getBluetoothFinderFilter(): IntentFilter? {
        if (ActivityCompat.checkSelfPermission(MainActivity.context, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {

            Log.d(SampleDataManager.logTag, "registerDeviceFinder, not have permission")
        } else {
            bluetoothAdapter?.let {
                if(it.isDiscovering) {
                    it.cancelDiscovery()
                }
                else {
                    if(it.isEnabled) {
                        it.startDiscovery()     // 검색 시작

                        return IntentFilter(BluetoothDevice.ACTION_FOUND)
                    } else {
                        Log.d(SampleDataManager.logTag, "registerDeviceFinder, need to enable bluetoothAdapter")
                    }
                }
            }
        }
        return null
    }


    /**
     *
     */
    class ConnectThread : Thread {
        var device: BluetoothDevice? = null
        var socket: BluetoothSocket? = null

        constructor(bDevice: BluetoothDevice?) {
            device = bDevice

            var tmpSocket: BluetoothSocket? = null
            if (ActivityCompat.checkSelfPermission(MainActivity.context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(SampleDataManager.logTag, "ConnectThread, no permissions")
            }
            else {
                try {
                    device?.let {
                        tmpSocket = it.createRfcommSocketToServiceRecord(SampleDataManager.myUUID)
                    }
                } catch (e: Exception) {
                    Log.d(SampleDataManager.logTag, "ConnectThread, exception")
                }
                socket = tmpSocket
            }

        }

        override fun run() {
            socket?.let {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED)
                    {
                        Log.d(SampleDataManager.logTag, "ConnectThread run, no permissions")
                        return
                    } else {
                        Log.d(SampleDataManager.logTag, "ConnectThread run, connect")
                        it.connect()
                    }
                } catch (e: Exception) {
                    Log.d(SampleDataManager.logTag, "ConnectThread run, exception")

                    try {
                        it.close()
                    } catch (e: Exception) {

                    }

                    connectionFailed()
                    return
                }

                synchronized(BTConnectManager) { connectThread = null }

                connected(it)
            }

        }

        fun cancel() {
            try {
                Log.d(SampleDataManager.logTag, "ConnectThread cancel, socket close")
                socket?.close()
            } catch (e: Exception) {
                Log.d(SampleDataManager.logTag, "ConnectThread cancel, exception")
            }
        }
    }


    class ConnectedThread : Thread {
        var socket: BluetoothSocket? = null
        var input: InputStream? = null
        var output: OutputStream? = null

        constructor(bSocket: BluetoothSocket) {
            socket = bSocket

            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            try {
                tmpIn = bSocket.inputStream
                tmpOut = bSocket.outputStream
            } catch (e: Exception) {
                Log.d(SampleDataManager.logTag, "ConnectedThread, exception")
            }

            input = tmpIn
            output = tmpOut
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int = 0

            try {
                while(true) {   // read 탐지를 지속적으로 할 수 있게 while 처리 (while 없을 경우 1회 탐지하고 완료됨)
                    input?.let {
                        bytes = it.read(buffer)

                        if (bytes != 0) {
                            var readData: String? = String(buffer, 0, bytes)
                            Log.d(SampleDataManager.logTag, "ConnectedThread run, readData : $readData")

                            btReadListener?.onReadMessage(readData)     // 리스너로 읽은 메시지 전달
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(SampleDataManager.logTag, "ConnectedThread run, exception : $e")
                connectionLost()
            }
        }

        fun sendMessage(message: String) {
            try {
                if (output != null) {
                    Log.d(SampleDataManager.logTag, "ConnectedThread sendMessage, message : $message")
                    output?.write(message.toByteArray())
                }
            } catch (e: Exception) {
                Log.d(SampleDataManager.logTag, "ConnectedThread sendMessage, exception")
            }
        }

        fun cancel() {
            if(socket != null) {
                try {
                    socket?.close()
                } catch (e: Exception) {
                    Log.d(SampleDataManager.logTag, "ConnectedThread cancel, exception")
                }
            }
        }
    }


    class AcceptThread : Thread {
        var serverSocket: BluetoothServerSocket? = null

        constructor() {
            var tmpSocket: BluetoothServerSocket? = null

            if (ActivityCompat.checkSelfPermission(MainActivity.context,Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED)
            {
                return
            }
            else {
                Log.d(SampleDataManager.logTag, "AcceptThread, set serverSocket")
                tmpSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("BluetoothSample", SampleDataManager.myUUID)
            }

            serverSocket = tmpSocket
        }

        override fun run() {
            var socket: BluetoothSocket? = null

            try {
                Log.d(SampleDataManager.logTag, "AcceptThread run")
                socket = serverSocket?.accept()
            } catch (e: Exception) {
                Log.d(SampleDataManager.logTag, "AcceptThread run, accept exception")

                try {
                    serverSocket?.close()
                } catch (e: Exception) {

                }
            }

            if(socket != null) {
                when(connectionState) {
                    ConnectionState.Listen, ConnectionState.Connecting -> {
                        Log.d(SampleDataManager.logTag, "AcceptThread run, to connected")
                        connected(socket)
                    }
                    ConnectionState.None, ConnectionState.Connected -> {
                        try {
                            Log.d(SampleDataManager.logTag, "AcceptThread run, socket close")
                            socket.close()

                        } catch (e: Exception) {
                            Log.d(SampleDataManager.logTag, "AcceptThread run, close exception")
                        }
                    }
                }
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: Exception) {

            }
        }
    }

    private fun connect() {
        cancelDeviceDiscovery()     // 탐색 중지

        if(connectionState == ConnectionState.Connecting) {
            connectThread?.cancel()
            connectThread = null
        }

        connectThread = ConnectThread(connectedDevice)
        connectThread?.start()

        if (connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }

        connectionState = ConnectionState.Connecting
        btStateListener?.onState(ConnectionState.Connecting, "")
    }

    @Synchronized
    fun connectionFailed() {
        Log.d(SampleDataManager.logTag, "connectionFailed")

        startListening()
    }

    @Synchronized
    fun connected(socket: BluetoothSocket) {
        Log.d(SampleDataManager.logTag, "connected")

        if(connectThread != null) {
            connectThread?.cancel()
            connectThread = null
        }

        if(connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }

        connectedThread = ConnectedThread(socket)
        connectedThread?.start()

        connectionState = ConnectionState.Connected
        btStateListener?.onState(ConnectionState.Connected, "")
    }

    fun connectionLost() {
        Log.d(SampleDataManager.logTag, "connectionLost")
        startListening()
    }

    private fun startListening() {
        Log.d(SampleDataManager.logTag, "startListening")

        if(connectThread != null) {
            connectThread?.cancel()
            connectThread = null
        }

        if(acceptThread == null) {
            acceptThread = AcceptThread()
            acceptThread?.start()
        }

        if(connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }

        connectionState = ConnectionState.Listen
        btStateListener?.onState(ConnectionState.Listen, "")
    }

    fun stopEverything() {
        Log.d(SampleDataManager.logTag, "stopEverything")

        if(connectThread != null) {
            connectThread?.cancel()
            connectThread = null
        }

        if(acceptThread == null) {
            acceptThread?.cancel()
            acceptThread = null
        }

        if(connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }

        connectionState = ConnectionState.None
        btStateListener?.onState(ConnectionState.None, "")
    }

    private fun cancelDeviceDiscovery() {
        if (ActivityCompat.checkSelfPermission(MainActivity.context, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED)
        {
            return
        } else {
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    fun sendMessage(text: String) {
        connectedThread?.sendMessage(text)
    }

    // end of class
}
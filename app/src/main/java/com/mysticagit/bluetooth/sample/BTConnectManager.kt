package com.mysticagit.bluetooth.sample

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.Intent
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

    fun getBluetoothIntent(): Intent {
        var enableBluetoothIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        // 300초 동안 검색 허용
        enableBluetoothIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)

        return enableBluetoothIntent
    }

    class ConnectThread : Thread {
        var device: BluetoothDevice? = null
        var socket: BluetoothSocket? = null

        constructor(bDevice: BluetoothDevice?) {
            device = bDevice

            var tmpSocket: BluetoothSocket? = null
            if (ActivityCompat.checkSelfPermission(
                MainActivity.context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
            ) {

            } else {
                try {
                    device?.let {
                        tmpSocket = it.createRfcommSocketToServiceRecord(SampleDataManager.myUUID)
                    }
                } catch (e: Exception) {

                }
                socket = tmpSocket
            }

        }

        override fun run() {
            socket?.let {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            MainActivity.context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
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
                socket?.close()
                Log.d(SampleDataManager.logTag, "ConnectThread cancel, socket close")
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
            Log.d(SampleDataManager.logTag, "ConnectedThread, 001")

                try {
                    Log.d(SampleDataManager.logTag, "ConnectedThread, 002")
                    while(true) {
                        input?.let {
                            Log.d(SampleDataManager.logTag, "ConnectedThread, 003")
                            bytes = it.read(buffer)
                            Log.d(SampleDataManager.logTag, "ConnectedThread, 004")
                            if (bytes != 0) {
                                Log.d(SampleDataManager.logTag, "ConnectedThread, 005")
                                var readData: String? = String(buffer, 0, bytes)
                                Log.d(SampleDataManager.logTag, "ConnectedThread, 006")
                                Log.d(
                                    SampleDataManager.logTag,
                                    "ConnectedThread run, readData : $readData"
                                )

                                btReadListener?.onReadMessage(readData)
                                Log.d(SampleDataManager.logTag, "ConnectedThread, 007")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(SampleDataManager.logTag, "ConnectedThread, 008, : $e")
                    connectionLost()
                }
        }

        fun sendMessage(message: String) {
            try {
                if (output != null) {
                    Log.d(
                        SampleDataManager.logTag,
                        "ConnectedThread sendMessage, message : $message"
                    )
//                val buffer = message.toByteArray()
//                var writeData: String = String(buffer, 0, message)
                    output?.write(message.toByteArray())
                }
            } catch (e: Exception) {
                Log.d(
                    SampleDataManager.logTag,
                    "ConnectedThread sendMessage, exception")
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

        }

        init {
            initSocket()
        }

        fun initSocket() {
            var tmpSocket: BluetoothServerSocket? = null

            if (ActivityCompat.checkSelfPermission(
                    MainActivity.context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            } else {
                tmpSocket =
                    BTConnectManager.bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                        "BluetoothSample",
                        SampleDataManager.myUUID
                    )
                Log.d(SampleDataManager.logTag, "AcceptThread, set socket")
            }

            serverSocket = tmpSocket
        }

        override fun run() {
            var socket: BluetoothSocket? = null
            try {
                socket = serverSocket?.accept()
                Log.d(SampleDataManager.logTag, "AcceptThread run")
            } catch (e: Exception) {
                try {
                    serverSocket?.close()
                } catch (e: Exception) {
                    Log.d(SampleDataManager.logTag, "AcceptThread run, accept exception")
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
                            socket.close()
                            Log.d(SampleDataManager.logTag, "AcceptThread run, socket close")
                        } catch (e: Exception) {
                            Log.d(SampleDataManager.logTag, "AcceptThread run, case exception")
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

    fun connect() {
        cancelDeviceDiscovery()

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

    fun startListening() {
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

    fun cancelDeviceDiscovery() {
        if (ActivityCompat.checkSelfPermission(
                MainActivity.context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        } else {
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    fun sendMessage(text: String) {
//        var tmpConnectedThread: ConnectedThread? = null
//
//        if(connectionState != ConnectionState.Connected) {
//            tmpConnectedThread = connectedThread
//        }
//
//        tmpConnectedThread?.sendMessage(text)
        connectedThread?.sendMessage(text)
    }
}
package com.example.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.Jamaah
import com.example.data.Kehadiran
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*

class LocalSyncManager private constructor(private val context: Context) {

    private val repository: AppRepository by lazy {
        AppRepository(AppDatabase.getDatabase(context))
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _peers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val peers: StateFlow<List<PeerDevice>> = _peers.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _syncStatus = MutableStateFlow<String>("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val syncPacketAdapter = moshi.adapter(SyncPacket::class.java)

    private var serverSocket: ServerSocket? = null
    private var udpSocket: DatagramSocket? = null

    val deviceName: String by lazy {
        val model = Build.MODEL
        val manufacture = Build.MANUFACTURER
        val cleanModel = if (model.startsWith(manufacture, ignoreCase = true)) {
            model
        } else {
            "$manufacture $model"
        }
        val randomSuffix = (Build.ID.hashCode() and 0xFFFF).toString(16).uppercase()
        "$cleanModel ($randomSuffix)"
    }

    companion object {
        private const val TCP_PORT = 8888
        private const val UDP_PORT = 8889
        private const val TAG = "LocalSyncManager"

        @Volatile
        private var INSTANCE: LocalSyncManager? = null

        fun getInstance(context: Context): LocalSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalSyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun startSyncServices() {
        if (_isServerRunning.value) return
        _isServerRunning.value = true
        _syncStatus.value = "Menjalankan layanan sinkronisasi..."

        // Start TCP Server
        scope.launch {
            runTcpServer()
        }

        // Start UDP Listener for Discovery
        scope.launch {
            runUdpListener()
        }

        // Start UDP Broadcaster
        scope.launch {
            runUdpBroadcaster()
        }

        // Periodically prune stale peers (older than 10 seconds)
        scope.launch {
            while (isActive && _isServerRunning.value) {
                delay(5000)
                val now = System.currentTimeMillis()
                val currentList = _peers.value
                val updatedList = currentList.filter { now - it.lastSeen < 10000 }
                if (updatedList.size != currentList.size) {
                    _peers.value = updatedList
                }
            }
        }
    }

    fun stopSyncServices() {
        _isServerRunning.value = false
        _syncStatus.value = "Layanan dihentikan."
        try {
            serverSocket?.close()
            udpSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing sockets", e)
        }
    }

    private suspend fun runTcpServer() {
        try {
            serverSocket = ServerSocket(TCP_PORT).apply {
                reuseAddress = true
            }
            Log.d(TAG, "TCP Server started on port $TCP_PORT")
            while (coroutineContext.isActive && _isServerRunning.value) {
                val socket = serverSocket?.accept() ?: break
                scope.launch {
                    handleClientConnection(socket)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TCP Server crashed or closed", e)
        }
    }

    private suspend fun handleClientConnection(socket: Socket) {
        socket.use { s ->
            try {
                Log.d(TAG, "Client connected from: ${s.inetAddress.hostAddress}")
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val writer = PrintWriter(s.getOutputStream(), true)

                // Read client payload line by line until closed
                val payloadBuilder = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    payloadBuilder.append(line)
                }

                val clientPayload = payloadBuilder.toString()
                if (clientPayload.isNotEmpty()) {
                    val clientPacket = withContext(Dispatchers.Default) {
                        syncPacketAdapter.fromJson(clientPayload)
                    }

                    if (clientPacket != null) {
                        Log.d(TAG, "Received ${clientPacket.jamaahList.size} Jamaah and ${clientPacket.kehadiranList.size} Kehadiran from ${clientPacket.deviceName}")
                        
                        // Merge Client data into Server database
                        mergeDatabase(clientPacket.jamaahList, clientPacket.kehadiranList)

                        // Prepare server current merged state to reply
                        val serverJamaah = repository.getAllJamaahIncludingDeleted()
                        val serverKehadiran = repository.getAllKehadiranIncludingDeleted()
                        val responsePacket = SyncPacket(deviceName, serverJamaah, serverKehadiran)

                        val responseJson = withContext(Dispatchers.Default) {
                            syncPacketAdapter.toJson(responsePacket)
                        }

                        // Write back response
                        writer.print(responseJson)
                        writer.flush()
                        
                        _syncStatus.value = "Sinkronisasi berhasil dengan ${clientPacket.deviceName}."
                        
                        // Try to trigger UI reload or live changes
                        triggerSyncFinished()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client connection", e)
            }
        }
    }

    private suspend fun runUdpListener() {
        try {
            udpSocket = DatagramSocket(UDP_PORT).apply {
                reuseAddress = true
                broadcast = true
            }
            val buffer = ByteArray(1024)
            Log.d(TAG, "UDP Listener started on port $UDP_PORT")
            while (coroutineContext.isActive && _isServerRunning.value) {
                val packet = DatagramPacket(buffer, buffer.size)
                udpSocket?.receive(packet)
                val senderIp = packet.address.hostAddress ?: continue
                
                // Skip if it's our own broadcast
                if (senderIp == getLocalIpAddress()) continue

                val message = String(packet.data, 0, packet.length).trim()
                if (message.startsWith("PRESENCE_BEACON:")) {
                    val parts = message.split(":")
                    if (parts.size >= 3) {
                        val peerName = parts[1]
                        val peerPort = parts[2].toIntOrNull() ?: TCP_PORT
                        val peer = PeerDevice(peerName, senderIp, peerPort, System.currentTimeMillis())

                        // Add or update peer
                        val currentList = _peers.value.toMutableList()
                        val index = currentList.indexOfFirst { it.ipAddress == senderIp }
                        if (index >= 0) {
                            currentList[index] = peer
                        } else {
                            currentList.add(peer)
                        }
                        _peers.value = currentList
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "UDP Listener crashed or closed", e)
        }
    }

    private suspend fun runUdpBroadcaster() {
        while (coroutineContext.isActive && _isServerRunning.value) {
            try {
                val localIp = getLocalIpAddress()
                if (localIp != null) {
                    val message = "PRESENCE_BEACON:$deviceName:$TCP_PORT"
                    val data = message.toByteArray()
                    
                    val broadcastAddr = InetAddress.getByName("255.255.255.255")
                    val packet = DatagramPacket(data, data.size, broadcastAddr, UDP_PORT)
                    
                    val tempSocket = DatagramSocket()
                    tempSocket.broadcast = true
                    tempSocket.send(packet)
                    tempSocket.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "UDP Beacon broadcast failed", e)
            }
            delay(2000) // Broadcast every 2 seconds
        }
    }

    suspend fun syncWithPeer(peer: PeerDevice): Boolean {
        _syncStatus.value = "Menghubungkan ke ${peer.name}..."
        return withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(peer.ipAddress, peer.port), 5000)

                _syncStatus.value = "Mengirim data ke ${peer.name}..."
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                // Fetch local state to push
                val localJamaah = repository.getAllJamaahIncludingDeleted()
                val localKehadiran = repository.getAllKehadiranIncludingDeleted()
                val clientPacket = SyncPacket(deviceName, localJamaah, localKehadiran)

                val jsonPayload = withContext(Dispatchers.Default) {
                    syncPacketAdapter.toJson(clientPacket)
                }

                // Send to server
                writer.print(jsonPayload)
                writer.flush()
                socket.shutdownOutput() // Signal end of transmission to the server

                _syncStatus.value = "Menerima balasan dari ${peer.name}..."
                
                // Read response
                val responseBuilder = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    responseBuilder.append(line)
                }

                val serverPayload = responseBuilder.toString()
                if (serverPayload.isNotEmpty()) {
                    val serverPacket = withContext(Dispatchers.Default) {
                        syncPacketAdapter.fromJson(serverPayload)
                    }

                    if (serverPacket != null) {
                        _syncStatus.value = "Menggabungkan data..."
                        mergeDatabase(serverPacket.jamaahList, serverPacket.kehadiranList)
                        _syncStatus.value = "Sinkronisasi berhasil dengan ${peer.name}!"
                        triggerSyncFinished()
                        true
                    } else {
                        _syncStatus.value = "Gagal memproses data balasan."
                        false
                    }
                } else {
                    _syncStatus.value = "Tidak ada respons dari ${peer.name}."
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed with peer: ${peer.name}", e)
                _syncStatus.value = "Gagal sinkronisasi: ${e.localizedMessage}"
                false
            } finally {
                try {
                    socket?.close()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun triggerAutoSync() {
        // Feature 14: Auto Sync
        // When local data changes, if we are in server mode, automatically push data to all active peers in background!
        scope.launch {
            val activePeers = _peers.value
            if (activePeers.isNotEmpty()) {
                Log.d(TAG, "Triggering background auto sync to ${activePeers.size} peers...")
                activePeers.forEach { peer ->
                    launch {
                        syncWithPeer(peer)
                    }
                }
            }
        }
    }

    private suspend fun mergeDatabase(receivedJamaah: List<Jamaah>, receivedKehadiran: List<Kehadiran>) {
        // Load existing
        val localJamaah = repository.getAllJamaahIncludingDeleted()
        val localJamaahMap = localJamaah.associateBy { it.id }
        val jamaahToUpsert = mutableListOf<Jamaah>()

        for (received in receivedJamaah) {
            val local = localJamaahMap[received.id]
            if (local == null) {
                // Not on our device yet
                jamaahToUpsert.add(received)
            } else if (received.lastUpdated > local.lastUpdated) {
                // Received is newer
                jamaahToUpsert.add(received)
            }
        }

        if (jamaahToUpsert.isNotEmpty()) {
            repository.upsertJamaahListFromSync(jamaahToUpsert)
            Log.d(TAG, "Merged ${jamaahToUpsert.size} Jamaah into local DB.")
        }

        // Merge Kehadiran
        val localKehadiran = repository.getAllKehadiranIncludingDeleted()
        val localKehadiranMap = localKehadiran.associateBy { it.id }
        val kehadiranToUpsert = mutableListOf<Kehadiran>()

        for (received in receivedKehadiran) {
            val local = localKehadiranMap[received.id]
            if (local == null) {
                kehadiranToUpsert.add(received)
            } else if (received.lastUpdated > local.lastUpdated) {
                kehadiranToUpsert.add(received)
            }
        }

        if (kehadiranToUpsert.isNotEmpty()) {
            repository.upsertKehadiranListFromSync(kehadiranToUpsert)
            Log.d(TAG, "Merged ${kehadiranToUpsert.size} Kehadiran into local DB.")
        }
    }

    private fun triggerSyncFinished() {
        // We can post a broad internal notification or flow update if UI needs to react instantly
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error getting local IP address", ex)
        }
        return null
    }
}

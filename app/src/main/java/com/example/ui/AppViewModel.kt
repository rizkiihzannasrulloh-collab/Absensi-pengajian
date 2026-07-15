package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.sync.LocalSyncManager
import com.example.sync.PeerDevice
import com.example.utils.AudioUtils
import com.example.utils.ExportUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db)
    private val syncManager = LocalSyncManager.getInstance(application)

    // Reactive streams from DB
    val allActiveJamaah: StateFlow<List<Jamaah>> = repository.allActiveJamaah
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActiveKehadiran: StateFlow<List<Kehadiran>> = repository.allActiveKehadiran
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Peers & Sync state
    val availablePeers: StateFlow<List<PeerDevice>> = syncManager.peers
    val isSyncServerRunning: StateFlow<Boolean> = syncManager.isServerRunning
    val syncStatus: StateFlow<String> = syncManager.syncStatus
    val deviceName: String = syncManager.deviceName

    // Security PIN authorization state
    private val _isAdminAuthorized = MutableStateFlow(false)
    val isAdminAuthorized = _isAdminAuthorized.asStateFlow()

    fun setAdminAuthorized(authorized: Boolean) {
        _isAdminAuthorized.value = authorized
    }

    // Attendance Log History Search Query and Combined Stream
    private val _searchLogQuery = MutableStateFlow("")
    val searchLogQuery = _searchLogQuery.asStateFlow()

    fun setSearchLogQuery(query: String) {
        _searchLogQuery.value = query
    }

    val allKehadiranWithJamaah: StateFlow<List<Pair<Kehadiran, Jamaah?>>> = combine(allActiveKehadiran, allActiveJamaah) { kehadiran, jamaah ->
        val jamaahMap = jamaah.associateBy { it.id }
        kehadiran.map { k -> Pair(k, jamaahMap[k.jamaahId]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Search & Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterDate = MutableStateFlow(getCurrentDateString())
    val filterDate = _filterDate.asStateFlow()

    private val _filterMonth = MutableStateFlow("") // Format: "MM"
    private val _filterYear = MutableStateFlow("")  // Format: "yyyy"

    // Scanning & Dialog Results
    private val _scanResult = MutableStateFlow<ScanStatus>(ScanStatus.Idle)
    val scanResult = _scanResult.asStateFlow()

    // Dashboard State
    val dashboardStats = combine(allActiveJamaah, allActiveKehadiran, _filterDate) { jamaah, kehadiran, todayDate ->
        val totalJamaah = jamaah.size
        val hadirHariIni = kehadiran.filter { it.tanggal == todayDate }.map { it.jamaahId }.distinct().size
        val belumHadir = kotlin.math.max(0, totalJamaah - hadirHariIni)
        
        // Month start & overall count
        val cal = Calendar.getInstance()
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
        val hadirBulanIni = kehadiran.filter { it.tanggal.startsWith(currentMonthStr) }.size
        val totalKehadiranKeseluruhan = kehadiran.size

        DashboardStats(
            totalJamaah = totalJamaah,
            hadirHariIni = hadirHariIni,
            belumHadir = belumHadir,
            hadirBulanIni = hadirBulanIni,
            totalKehadiran = totalKehadiranKeseluruhan
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    init {
        // Automatically start local discovery services on startup
        startSyncManager()
    }

    // Helper date utilities
    fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getCurrentTimeString(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    // Jamaah Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterDate(dateStr: String) {
        _filterDate.value = dateStr
    }

    fun addJamaah(nama: String, noHp: String, alamat: String, keterangan: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val jamaah = Jamaah(
                nama = nama,
                noHp = noHp,
                alamat = alamat,
                keterangan = keterangan
            )
            repository.insertJamaah(jamaah)
            triggerAutoSync()
        }
    }

    fun updateJamaah(jamaah: Jamaah) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateJamaah(jamaah)
            triggerAutoSync()
        }
    }

    fun deleteJamaah(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteJamaahSoft(id)
            triggerAutoSync()
        }
    }

    // Attendance Scanning
    fun processQrScan(uuidCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _scanResult.value = ScanStatus.Processing

            // 1. Verify if Jamaah exists and is not deleted
            val jamaah = repository.getJamaahById(uuidCode)
            if (jamaah == null || jamaah.isDeleted) {
                withContext(Dispatchers.Main) {
                    AudioUtils.playErrorBeep()
                    _scanResult.value = ScanStatus.Error("QR Code tidak dikenal atau jamaah telah dihapus.")
                }
                return@launch
            }

            // 2. Prevent double attendance for same day
            val today = getCurrentDateString()
            val existing = repository.getCheckedInToday(jamaah.id, today)
            if (existing != null) {
                withContext(Dispatchers.Main) {
                    AudioUtils.playErrorBeep()
                    _scanResult.value = ScanStatus.Error("Jamaah \"${jamaah.nama}\" sudah melakukan absensi hari ini.")
                }
                return@launch
            }

            // 3. Record attendance
            val kehadiran = Kehadiran(
                jamaahId = jamaah.id,
                tanggal = today,
                waktu = getCurrentTimeString()
            )
            repository.insertKehadiran(kehadiran)

            withContext(Dispatchers.Main) {
                AudioUtils.playSuccessBeep()
                _scanResult.value = ScanStatus.Success(
                    nama = jamaah.nama,
                    noHp = jamaah.noHp,
                    waktu = kehadiran.waktu,
                    tanggal = kehadiran.tanggal
                )
            }

            // Trigger P2P Auto sync background
            triggerAutoSync()
        }
    }

    fun resetScanStatus() {
        _scanResult.value = ScanStatus.Idle
    }

    // Direct deletion of attendance records
    fun deleteKehadiran(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteKehadiranSoft(id)
            triggerAutoSync()
        }
    }

    // P2P Sync services
    fun startSyncManager() {
        syncManager.startSyncServices()
    }

    fun stopSyncManager() {
        syncManager.stopSyncServices()
    }

    fun syncWithPeerDevice(peer: PeerDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            syncManager.syncWithPeer(peer)
        }
    }

    private fun triggerAutoSync() {
        syncManager.triggerAutoSync()
    }

    // Admin & Security PIN verification
    suspend fun verifyAdminPin(pin: String): Boolean {
        return repository.verifyAdminPin(pin)
    }

    fun updateAdminPin(oldPin: String, newPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val verified = repository.verifyAdminPin(oldPin)
            if (verified) {
                repository.saveAdminPin(newPin)
                withContext(Dispatchers.Main) { onResult(true) }
            } else {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun resetAppDatabase(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val verified = repository.verifyAdminPin(pin)
            if (verified) {
                repository.resetDatabase()
                withContext(Dispatchers.Main) { onResult(true) }
            } else {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    // Local IP address fetching
    fun getLocalIpAddress(): String {
        return syncManager.getLocalIpAddress() ?: "Tidak terhubung ke Wi-Fi / Hotspot"
    }

    // Document & Sheet Sharing Interfaces
    fun exportReportCsv(type: String): File? {
        val jamaahList = allActiveJamaah.value
        val kehadiranList = allActiveKehadiran.value
        return ExportUtils.exportToCsv(getApplication(), jamaahList, kehadiranList, type)
    }

    fun exportReportPdf(): File? {
        val jamaahList = allActiveJamaah.value
        val kehadiranList = allActiveKehadiran.value
        return ExportUtils.exportToPdf(getApplication(), jamaahList, kehadiranList)
    }

    fun exportDatabaseBackup(): File? {
        val jamaahList = allActiveJamaah.value
        val kehadiranList = allActiveKehadiran.value
        return ExportUtils.exportToJsonBackup(getApplication(), jamaahList, kehadiranList)
    }

    fun restoreDatabaseBackup(uri: android.net.Uri, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = ExportUtils.restoreFromJsonBackup(getApplication(), uri)
            withContext(Dispatchers.Main) {
                callback(success)
            }
        }
    }
}

data class DashboardStats(
    val totalJamaah: Int = 0,
    val hadirHariIni: Int = 0,
    val belumHadir: Int = 0,
    val hadirBulanIni: Int = 0,
    val totalKehadiran: Int = 0
)

sealed class ScanStatus {
    object Idle : ScanStatus()
    object Processing : ScanStatus()
    data class Success(val nama: String, val noHp: String, val waktu: String, val tanggal: String) : ScanStatus()
    data class Error(val message: String) : ScanStatus()
}

package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AppRepository(private val db: AppDatabase) {
    private val jamaahDao = db.jamaahDao()
    private val kehadiranDao = db.kehadiranDao()
    private val configDao = db.configDao()

    val allActiveJamaah: Flow<List<Jamaah>> = jamaahDao.getAllActive()
    val allActiveKehadiran: Flow<List<Kehadiran>> = kehadiranDao.getAllActive()

    fun getKehadiranByDate(tanggal: String): Flow<List<Kehadiran>> = kehadiranDao.getByDate(tanggal)
    fun getKehadiranByJamaahId(jamaahId: String): Flow<List<Kehadiran>> = kehadiranDao.getByJamaahId(jamaahId)

    suspend fun getJamaahById(id: String): Jamaah? = jamaahDao.getById(id)

    suspend fun insertJamaah(jamaah: Jamaah) {
        val updatedJamaah = jamaah.copy(lastUpdated = System.currentTimeMillis())
        jamaahDao.upsert(updatedJamaah)
    }

    suspend fun updateJamaah(jamaah: Jamaah) {
        val updatedJamaah = jamaah.copy(lastUpdated = System.currentTimeMillis())
        jamaahDao.upsert(updatedJamaah)
    }

    suspend fun deleteJamaahSoft(id: String) {
        jamaahDao.deleteSoft(id, System.currentTimeMillis())
        // Also soft delete all of this jamaah's attendance records to keep sync state consistent
        // For simplicity, we can let syncing sync them, or just delete them soft
    }

    suspend fun getCheckedInToday(jamaahId: String, tanggal: String): Kehadiran? {
        return kehadiranDao.getCheckedInToday(jamaahId, tanggal)
    }

    suspend fun insertKehadiran(kehadiran: Kehadiran) {
        val updatedKehadiran = kehadiran.copy(lastUpdated = System.currentTimeMillis())
        kehadiranDao.upsert(updatedKehadiran)
    }

    suspend fun deleteKehadiranSoft(id: String) {
        kehadiranDao.deleteSoft(id, System.currentTimeMillis())
    }

    suspend fun getAllJamaahIncludingDeleted(): List<Jamaah> = jamaahDao.getAllIncludingDeleted()
    suspend fun getAllKehadiranIncludingDeleted(): List<Kehadiran> = kehadiranDao.getAllIncludingDeleted()

    suspend fun upsertJamaahListFromSync(list: List<Jamaah>) {
        jamaahDao.upsertAll(list)
    }

    suspend fun upsertKehadiranListFromSync(list: List<Kehadiran>) {
        kehadiranDao.upsertAll(list)
    }

    // Config & Security
    suspend fun getAdminPin(): String {
        return configDao.getValue("admin_pin") ?: "1234"
    }

    suspend fun saveAdminPin(newPin: String) {
        configDao.setValue(Config("admin_pin", newPin))
    }

    suspend fun verifyAdminPin(pin: String): Boolean {
        return getAdminPin() == pin
    }

    suspend fun resetDatabase() {
        jamaahDao.hardDeleteAll()
        kehadiranDao.hardDeleteAll()
        configDao.deleteKey("admin_pin") // Resets to "1234"
    }
}

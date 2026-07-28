package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class DailyAttendanceStat(
    val tanggal: String,
    val totalHadir: Int
)

data class JamaahMonthlyStat(
    val jamaahId: String,
    val totalHadir: Int
)

@Dao
interface JamaahDao {
    @Query("SELECT * FROM jamaah WHERE isDeleted = 0 ORDER BY nama ASC")
    fun getAllActive(): Flow<List<Jamaah>>

    @Query("SELECT * FROM jamaah ORDER BY lastUpdated DESC")
    suspend fun getAllIncludingDeleted(): List<Jamaah>

    @Query("SELECT * FROM jamaah WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Jamaah?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(jamaah: Jamaah)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(jamaahList: List<Jamaah>)

    @Query("UPDATE jamaah SET isDeleted = 1, lastUpdated = :lastUpdated WHERE id = :id")
    suspend fun deleteSoft(id: String, lastUpdated: Long = System.currentTimeMillis())

    @Query("DELETE FROM jamaah")
    suspend fun hardDeleteAll()
}

@Dao
interface KehadiranDao {
    @Query("SELECT * FROM kehadiran WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllActive(): Flow<List<Kehadiran>>

    @Query("SELECT * FROM kehadiran ORDER BY lastUpdated DESC")
    suspend fun getAllIncludingDeleted(): List<Kehadiran>

    @Query("SELECT * FROM kehadiran WHERE tanggal = :tanggal AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getByDate(tanggal: String): Flow<List<Kehadiran>>

    @Query("SELECT * FROM kehadiran WHERE jamaahId = :jamaahId AND tanggal = :tanggal AND isDeleted = 0 LIMIT 1")
    suspend fun getCheckedInToday(jamaahId: String, tanggal: String): Kehadiran?

    @Query("SELECT * FROM kehadiran WHERE jamaahId = :jamaahId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getByJamaahId(jamaahId: String): Flow<List<Kehadiran>>

    @Query("SELECT tanggal, COUNT(DISTINCT jamaahId) as totalHadir FROM kehadiran WHERE tanggal LIKE :yearMonth || '%' AND isDeleted = 0 GROUP BY tanggal ORDER BY tanggal ASC")
    fun getDailyAttendanceStats(yearMonth: String): Flow<List<DailyAttendanceStat>>

    @Query("SELECT jamaahId, COUNT(DISTINCT tanggal) as totalHadir FROM kehadiran WHERE tanggal LIKE :yearMonth || '%' AND isDeleted = 0 GROUP BY jamaahId")
    fun getMonthlyStatsPerJamaah(yearMonth: String): Flow<List<JamaahMonthlyStat>>

    @Query("SELECT COUNT(DISTINCT tanggal) FROM kehadiran WHERE tanggal LIKE :yearMonth || '%' AND isDeleted = 0")
    fun getTotalSessionsInMonth(yearMonth: String): Flow<Int>

    @Query("SELECT COUNT(DISTINCT jamaahId) FROM kehadiran WHERE tanggal = :tanggal AND isDeleted = 0")
    fun getTodayHadirCount(tanggal: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(kehadiran: Kehadiran)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(kehadiranList: List<Kehadiran>)

    @Query("UPDATE kehadiran SET isDeleted = 1, lastUpdated = :lastUpdated WHERE id = :id")
    suspend fun deleteSoft(id: String, lastUpdated: Long = System.currentTimeMillis())

    @Query("DELETE FROM kehadiran")
    suspend fun hardDeleteAll()
}

@Dao
interface ConfigDao {
    @Query("SELECT value FROM config WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM config WHERE `key` = 'nama_panitia' LIMIT 1")
    fun getNamaPanitiaFlow(): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(config: Config)

    @Query("DELETE FROM config WHERE `key` = :key")
    suspend fun deleteKey(key: String)
}

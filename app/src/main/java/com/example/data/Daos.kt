package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(config: Config)

    @Query("DELETE FROM config WHERE `key` = :key")
    suspend fun deleteKey(key: String)
}

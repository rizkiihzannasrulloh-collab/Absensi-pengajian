package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "jamaah")
data class Jamaah(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nama: String,
    val noHp: String,
    val alamat: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val keterangan: String = ""
)

@Entity(tableName = "kehadiran")
data class Kehadiran(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val jamaahId: String,
    val tanggal: String, // format: "yyyy-MM-dd"
    val waktu: String,   // format: "HH:mm:ss"
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "config")
data class Config(
    @PrimaryKey val key: String,
    val value: String
)

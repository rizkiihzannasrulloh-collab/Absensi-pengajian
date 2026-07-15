package com.example.sync

import com.example.data.Jamaah
import com.example.data.Kehadiran
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncPacket(
    val deviceName: String,
    val jamaahList: List<Jamaah>,
    val kehadiranList: List<Kehadiran>,
    val timestamp: Long = System.currentTimeMillis()
)

data class PeerDevice(
    val name: String,
    val ipAddress: String,
    val port: Int,
    val lastSeen: Long = System.currentTimeMillis()
)

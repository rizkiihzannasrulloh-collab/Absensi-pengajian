package com.example.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.AppRepository
import com.example.data.Jamaah
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@JsonClass(generateAdapter = true)
data class JamaahDto(
    val id: String,
    val nama: String,
    val noHp: String,
    val alamat: String,
    val createdAt: Long,
    val lastUpdated: Long,
    val isDeleted: Boolean = false,
    val keterangan: String = ""
)

@JsonClass(generateAdapter = true)
data class JamaahPackageDto(
    val version: Int = 1,
    val senderDeviceName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalCount: Int,
    val jamaahList: List<JamaahDto>
)

enum class ImportDuplicateStrategy {
    MERGE_KEEP_LATEST, // Gabungkan: update jika incoming lastUpdated > local
    OVERWRITE_ALL,     // Timpa: ganti semua data lokal dengan data baru
    SKIP_EXISTING      // Lewati: abaikan jika ID/QR sudah ada di lokal
}

data class ImportSummary(
    val totalInPackage: Int,
    val insertedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val duplicateIdsCount: Int
)

/**
 * Abstraction Interface for Jamaah Transport
 * Allows swapping transport mechanisms (Bluetooth, Wi-Fi Direct, Local Socket, P2P Share)
 * without altering import/export business logic.
 */
interface IJamaahTransportManager {
    suspend fun createPackage(jamaahList: List<Jamaah>, senderName: String): JamaahPackageDto
    suspend fun exportPackageCompressedFile(context: Context, jamaahList: List<Jamaah>, senderName: String): File?
    suspend fun parsePackageFromFile(context: Context, uri: Uri): JamaahPackageDto?
    suspend fun parsePackageFromJson(jsonString: String): JamaahPackageDto?
    suspend fun analyzePackageDuplicates(repository: AppRepository, pkg: JamaahPackageDto): ImportSummary
    suspend fun importPackage(
        repository: AppRepository,
        pkg: JamaahPackageDto,
        strategy: ImportDuplicateStrategy
    ): ImportSummary
}

class JamaahTransportManager : IJamaahTransportManager {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(JamaahPackageDto::class.java)

    override suspend fun createPackage(jamaahList: List<Jamaah>, senderName: String): JamaahPackageDto {
        val dtos = jamaahList.map { j ->
            JamaahDto(
                id = j.id,
                nama = j.nama,
                noHp = j.noHp,
                alamat = j.alamat,
                createdAt = j.createdAt,
                lastUpdated = j.lastUpdated,
                isDeleted = j.isDeleted,
                keterangan = j.keterangan
            )
        }
        return JamaahPackageDto(
            version = 1,
            senderDeviceName = senderName,
            timestamp = System.currentTimeMillis(),
            totalCount = dtos.size,
            jamaahList = dtos
        )
    }

    override suspend fun exportPackageCompressedFile(context: Context, jamaahList: List<Jamaah>, senderName: String): File? {
        return try {
            val pkg = createPackage(jamaahList, senderName)
            val jsonStr = adapter.toJson(pkg)
            
            // Compress with GZIP for fast offline Bluetooth/Wi-Fi transfer
            val bos = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(bos)
            gzip.write(jsonStr.toByteArray(Charsets.UTF_8))
            gzip.close()
            val compressedBytes = bos.toByteArray()

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(context.cacheDir, "JamaahData_$dateStr.jamaah.json")
            val fos = FileOutputStream(file)
            fos.write(compressedBytes)
            fos.close()
            file
        } catch (e: Exception) {
            Log.e("JamaahTransport", "Gagal ekspor compressed package", e)
            null
        }
    }

    override suspend fun parsePackageFromFile(context: Context, uri: Uri): JamaahPackageDto? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()

            // Try decompressing GZIP first; fallback to direct JSON string if uncompressed
            val jsonStr = try {
                val gzipStream = GZIPInputStream(ByteArrayInputStream(bytes))
                gzipStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (e: Exception) {
                String(bytes, Charsets.UTF_8)
            }

            adapter.fromJson(jsonStr)
        } catch (e: Exception) {
            Log.e("JamaahTransport", "Gagal parse package dari File", e)
            null
        }
    }

    override suspend fun parsePackageFromJson(jsonString: String): JamaahPackageDto? {
        return try {
            adapter.fromJson(jsonString)
        } catch (e: Exception) {
            Log.e("JamaahTransport", "Gagal parse package dari JSON", e)
            null
        }
    }

    override suspend fun analyzePackageDuplicates(repository: AppRepository, pkg: JamaahPackageDto): ImportSummary {
        val existingJamaah = repository.getAllJamaahIncludingDeleted().associateBy { it.id }
        var duplicates = 0

        for (dto in pkg.jamaahList) {
            if (existingJamaah.containsKey(dto.id)) {
                duplicates++
            }
        }

        return ImportSummary(
            totalInPackage = pkg.jamaahList.size,
            insertedCount = 0,
            updatedCount = 0,
            skippedCount = 0,
            duplicateIdsCount = duplicates
        )
    }

    override suspend fun importPackage(
        repository: AppRepository,
        pkg: JamaahPackageDto,
        strategy: ImportDuplicateStrategy
    ): ImportSummary {
        val existingMap = repository.getAllJamaahIncludingDeleted().associateBy { it.id }
        
        var inserted = 0
        var updated = 0
        var skipped = 0
        val toUpsert = mutableListOf<Jamaah>()

        for (dto in pkg.jamaahList) {
            val existing = existingMap[dto.id]
            val incomingJamaah = Jamaah(
                id = dto.id,
                nama = dto.nama,
                noHp = dto.noHp,
                alamat = dto.alamat,
                createdAt = dto.createdAt,
                lastUpdated = dto.lastUpdated,
                isDeleted = dto.isDeleted,
                keterangan = dto.keterangan
            )

            if (existing == null) {
                toUpsert.add(incomingJamaah)
                inserted++
            } else {
                when (strategy) {
                    ImportDuplicateStrategy.MERGE_KEEP_LATEST -> {
                        if (dto.lastUpdated >= existing.lastUpdated) {
                            toUpsert.add(incomingJamaah)
                            updated++
                        } else {
                            skipped++
                        }
                    }
                    ImportDuplicateStrategy.OVERWRITE_ALL -> {
                        toUpsert.add(incomingJamaah)
                        updated++
                    }
                    ImportDuplicateStrategy.SKIP_EXISTING -> {
                        skipped++
                    }
                }
            }
        }

        if (toUpsert.isNotEmpty()) {
            repository.upsertJamaahListFromSync(toUpsert)
        }

        return ImportSummary(
            totalInPackage = pkg.jamaahList.size,
            insertedCount = inserted,
            updatedCount = updated,
            skippedCount = skipped,
            duplicateIdsCount = pkg.jamaahList.size - inserted
        )
    }
}

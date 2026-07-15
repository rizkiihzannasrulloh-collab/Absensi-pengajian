package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.Jamaah
import com.example.data.Kehadiran
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val exportTime: Long = System.currentTimeMillis(),
    val jamaahList: List<Jamaah>,
    val kehadiranList: List<Kehadiran>
)

object ExportUtils {
    private const val TAG = "ExportUtils"

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val payloadAdapter = moshi.adapter(BackupPayload::class.java)

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Log.e(TAG, "Ggal share file", e)
        }
    }

    // Export to CSV
    fun exportToCsv(context: Context, jamaahList: List<Jamaah>, kehadiranList: List<Kehadiran>, type: String): File? {
        return try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(context.cacheDir, "Laporan_${type}_$dateStr.csv")
            val writer = FileOutputStream(file).bufferedWriter()

            if (type == "jamaah") {
                writer.write("ID,Nama Lengkap,Nomor HP,Alamat,Tanggal Dibuat,Keterangan\n")
                for (j in jamaahList) {
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(j.createdAt))
                    writer.write("\"${j.id}\",\"${j.nama}\",\"${j.noHp}\",\"${j.alamat}\",\"$date\",\"${j.keterangan}\"\n")
                }
            } else {
                writer.write("ID Kehadiran,Nama Jamaah,Nomor HP,Tanggal Hadir,Waktu Hadir,Timestamp\n")
                val jamaahMap = jamaahList.associateBy { it.id }
                for (k in kehadiranList) {
                    val j = jamaahMap[k.jamaahId]
                    val nama = j?.nama ?: "Tidak Dikenal"
                    val noHp = j?.noHp ?: "-"
                    writer.write("\"${k.id}\",\"$nama\",\"$noHp\",\"${k.tanggal}\",\"${k.waktu}\",\"${k.timestamp}\"\n")
                }
            }

            writer.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Gagal export CSV", e)
            null
        }
    }

    // Export to JSON Backup File
    fun exportToJsonBackup(context: Context, jamaahList: List<Jamaah>, kehadiranList: List<Kehadiran>): File? {
        return try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(context.cacheDir, "Backup_Absensi_$dateStr.json")
            val payload = BackupPayload(jamaahList = jamaahList, kehadiranList = kehadiranList)
            val jsonStr = payloadAdapter.toJson(payload)

            val stream = FileOutputStream(file)
            stream.write(jsonStr.toByteArray())
            stream.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Gagal export JSON backup", e)
            null
        }
    }

    // Restore from JSON Backup File
    suspend fun restoreFromJsonBackup(context: Context, fileUri: Uri): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(fileUri)
            val jsonStr = inputStream?.bufferedReader()?.use { it.readText() }

            if (jsonStr != null) {
                val payload = payloadAdapter.fromJson(jsonStr)
                if (payload != null) {
                    val repository = AppRepository(AppDatabase.getDatabase(context))
                    // Merging data
                    repository.upsertJamaahListFromSync(payload.jamaahList)
                    repository.upsertKehadiranListFromSync(payload.kehadiranList)
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Gagal restore JSON backup", e)
            false
        }
    }

    // Export to beautiful PDF
    fun exportToPdf(context: Context, jamaahList: List<Jamaah>, kehadiranList: List<Kehadiran>): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        val headerPaint = Paint()

        return try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(context.cacheDir, "Laporan_Absensi_$dateStr.pdf")

            // PDF dimensions (A4 size: 595 x 842 points)
            val pageWidth = 595
            val pageHeight = 842

            // Prepare list of rows
            val jamaahMap = jamaahList.associateBy { it.id }
            val rows = kehadiranList.map { k ->
                val j = jamaahMap[k.jamaahId]
                Triple(j?.nama ?: "Tidak Dikenal", k.tanggal, k.waktu)
            }.sortedByDescending { "${it.second} ${it.third}" }

            val itemsPerPage = 25
            val totalPages = kotlin.math.max(1, (rows.size + itemsPerPage - 1) / itemsPerPage)

            for (pageNumber in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Draw Islamic Green Banner
                paint.color = Color.parseColor("#1B5E20") // Deep Forest Green
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paint)

                // 2. Header Text
                titlePaint.color = Color.WHITE
                titlePaint.textSize = 18f
                titlePaint.isFakeBoldText = true
                canvas.drawText("ABSENSI PENGAJIAN OFFLINE", 30f, 40f, titlePaint)

                titlePaint.textSize = 10f
                titlePaint.isFakeBoldText = false
                val printDate = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault()).format(Date())
                canvas.drawText("Laporan Kehadiran Jamaah | Dicetak: $printDate", 30f, 65f, titlePaint)

                // 3. Draw Table Headers
                var yPos = 130f
                paint.color = Color.parseColor("#E8F5E9") // Very Light Green Background
                canvas.drawRect(20f, yPos - 20f, pageWidth - 20f, yPos + 10f, paint)

                headerPaint.color = Color.parseColor("#1B5E20")
                headerPaint.textSize = 11f
                headerPaint.isFakeBoldText = true
                canvas.drawText("No", 30f, yPos, headerPaint)
                canvas.drawText("Nama Lengkap", 70f, yPos, headerPaint)
                canvas.drawText("Tanggal", 320f, yPos, headerPaint)
                canvas.drawText("Waktu", 470f, yPos, headerPaint)

                // Divider line
                paint.color = Color.parseColor("#4CAF50")
                paint.strokeWidth = 1.5f
                canvas.drawLine(20f, yPos + 12f, pageWidth - 20f, yPos + 12f, paint)

                yPos += 30f

                // 4. Draw Rows
                paint.color = Color.BLACK
                paint.textSize = 10f
                paint.isFakeBoldText = false

                val startIndex = pageNumber * itemsPerPage
                val endIndex = kotlin.math.min(startIndex + itemsPerPage, rows.size)

                for (i in startIndex until endIndex) {
                    val row = rows[i]
                    val indexNo = (i + 1).toString()

                    // Alternating background
                    if (i % 2 == 1) {
                        val rowBgPaint = Paint().apply { color = Color.parseColor("#F9F9F9") }
                        canvas.drawRect(20f, yPos - 15f, pageWidth - 20f, yPos + 10f, rowBgPaint)
                    }

                    canvas.drawText(indexNo, 30f, yPos, paint)
                    // Truncate name if too long to fit beautifully
                    val name = if (row.first.length > 30) row.first.substring(0, 27) + "..." else row.first
                    canvas.drawText(name, 70f, yPos, paint)
                    canvas.drawText(row.second, 320f, yPos, paint)
                    canvas.drawText(row.third, 470f, yPos, paint)

                    // Thin grid line
                    val thinLinePaint = Paint().apply {
                        color = Color.parseColor("#E0E0E0")
                        strokeWidth = 0.5f
                    }
                    canvas.drawLine(20f, yPos + 10f, pageWidth - 20f, yPos + 10f, thinLinePaint)

                    yPos += 24f
                }

                // 5. Draw Footer
                val footerPaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 9f
                }
                canvas.drawText("Halaman ${pageNumber + 1} dari $totalPages", (pageWidth - 100).toFloat(), 810f, footerPaint)
                canvas.drawText("Aplikasi Absensi Pengajian Offline - Panitia Syiar", 30f, 810f, footerPaint)

                pdfDocument.finishPage(page)
            }

            val stream = FileOutputStream(file)
            pdfDocument.writeTo(stream)
            stream.close()
            pdfDocument.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Gagal export PDF", e)
            pdfDocument.close()
            null
        }
    }
}

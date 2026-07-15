package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Jamaah
import com.example.data.Kehadiran
import com.example.ui.AppViewModel
import com.example.utils.ExportUtils
import java.io.File
import java.util.*

@Composable
fun RiwayatScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.allKehadiranWithJamaah.collectAsState()
    val searchLogQuery by viewModel.searchLogQuery.collectAsState()
    val context = LocalContext.current

    // Sorting State
    var isNewestFirst by remember { mutableStateOf(true) }

    // Filtered list
    val filteredLogs = remember(logs, searchLogQuery, isNewestFirst) {
        val q = searchLogQuery.lowercase(Locale.getDefault())
        val list = if (q.isEmpty()) {
            logs
        } else {
            logs.filter { (kehadiran, jamaah) ->
                jamaah?.nama?.lowercase(Locale.getDefault())?.contains(q) == true ||
                kehadiran.tanggal.contains(q) ||
                kehadiran.waktu.contains(q)
            }
        }
        if (isNewestFirst) {
            list.sortedByDescending { "${it.first.tanggal} ${it.first.waktu}" }
        } else {
            list.sortedBy { "${it.first.tanggal} ${it.first.waktu}" }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search Filter Section & Sorting Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchLogQuery,
                onValueChange = { viewModel.setSearchLogQuery(it) },
                placeholder = { Text("Cari log absen (Nama/Tanggal)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchLogQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchLogQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            IconButton(
                onClick = { isNewestFirst = !isNewestFirst },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (isNewestFirst) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = "Urutkan",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Export Controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    // Export to Excel / CSV
                    val dataForExport = filteredLogs.map { it.first }
                    val jamaahList = logs.mapNotNull { it.second }.distinctBy { it.id }
                    val file = ExportUtils.exportToCsv(context, jamaahList, dataForExport, "kehadiran")
                    if (file != null) {
                        ExportUtils.shareFile(context, file, "text/csv", "Share File Absensi Excel/CSV")
                    } else {
                        Toast.makeText(context, "Gagal export CSV", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Excel Green
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ListAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Excel/CSV", fontSize = 11.sp, color = Color.White)
            }

            Button(
                onClick = {
                    // Export to PDF report
                    val dataForExport = filteredLogs.map { it.first }
                    val jamaahList = logs.mapNotNull { it.second }.distinctBy { it.id }
                    val file = ExportUtils.exportToPdf(context, jamaahList, dataForExport)
                    if (file != null) {
                        ExportUtils.shareFile(context, file, "application/pdf", "Share Laporan PDF")
                    } else {
                        Toast.makeText(context, "Gagal export PDF", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)), // PDF Red
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export PDF", fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // History logs List
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchLogQuery.isEmpty()) "Belum ada riwayat absensi hari ini." else "Riwayat tidak ditemukan.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs) { (kehadiran, jamaah) ->
                    RiwayatItemRow(kehadiran = kehadiran, jamaah = jamaah)
                }
            }
        }
    }
}

@Composable
fun RiwayatItemRow(
    kehadiran: Kehadiran,
    jamaah: Jamaah?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Colored circular presence indicator
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (kehadiran.isDeleted) Color.Red.copy(alpha = 0.12f)
                            else Color(0xFFE8F5E9)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (kehadiran.isDeleted) Icons.Default.Close else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (kehadiran.isDeleted) Color.Red else Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = jamaah?.nama ?: "Jamaah Tidak Dikenal",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "ID: ${kehadiran.jamaahId.take(8).uppercase()}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = kehadiran.tanggal,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = kehadiran.waktu,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

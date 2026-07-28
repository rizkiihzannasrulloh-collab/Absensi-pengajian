package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.AppViewModel
import com.example.ui.DashboardStats
import com.example.ui.ScanStatus
import com.example.ui.components.CameraScannerView
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val scanStatus by viewModel.scanResult.collectAsState()
    val namaPanitia by viewModel.namaPanitia.collectAsState()
    var isScanning by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Formatted Date in Indonesian locale
    val todayLong = remember {
        val cal = Calendar.getInstance()
        val indonesianLocale = Locale("id", "ID")
        SimpleDateFormat("EEEE, dd MMMM yyyy", indonesianLocale).format(cal.time)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Premium Islamic Hero Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GoldBorder.copy(alpha = 0.6f)),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    EmeraldPrimary,
                                    EmeraldDark
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Islamic Salam Greeting
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mosque,
                                    contentDescription = "Mosque Icon",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Assalamu'alaikum,",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextLightGold,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                )
                            }

                            // Badge Tag
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = GoldPrimary.copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Offline Syiar",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = GoldAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Reaktif Dynamic Nama Panitia
                        Text(
                            text = namaPanitia,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = 0.3.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Calendar & Date row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Calendar Icon",
                                tint = GoldAccent.copy(alpha = 0.9f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = todayLong,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            // 2. Premium "Absen Sekarang" Scanner Hero Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clickable { isScanning = true },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GoldBorder.copy(alpha = 0.4f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            EmeraldPrimary.copy(alpha = 0.15f),
                                            GoldPrimary.copy(alpha = 0.15f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "ABSEN SEKARANG",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "Pindai QR / Barcode Kartu Jamaah",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Open Scanner",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 3. Section Header: Grid Statistics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GoldAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Statistik Absensi Hari Ini",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }

            // Stat Cards Row 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedStatCard(
                    title = "Total Jamaah",
                    value = stats.totalJamaah,
                    unit = "Jamaah",
                    icon = Icons.Default.People,
                    accentColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                AnimatedStatCard(
                    title = "Jamaah Hadir",
                    value = stats.hadirHariIni,
                    unit = "Hadir",
                    icon = Icons.Default.CheckCircle,
                    accentColor = StatusSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            // Stat Cards Row 2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedStatCard(
                    title = "Belum Hadir",
                    value = stats.belumHadir,
                    unit = "Orang",
                    icon = Icons.Default.HourglassEmpty,
                    accentColor = StatusWarning,
                    modifier = Modifier.weight(1f)
                )
                val percentage = if (stats.totalJamaah > 0) {
                    (stats.hadirHariIni * 100) / stats.totalJamaah
                } else 0
                AnimatedStatCard(
                    title = "Persentase",
                    value = percentage,
                    unit = "%",
                    icon = Icons.Default.Star,
                    accentColor = GoldPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Premium Animated Progress Bar Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.5.dp, GoldBorder.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tingkat Kehadiran Hari Ini",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        val rawPercent = if (stats.totalJamaah > 0) {
                            (stats.hadirHariIni * 100f) / stats.totalJamaah.toFloat()
                        } else 0f

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GoldContainer
                        ) {
                            Text(
                                text = "${rawPercent.toInt()}%",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val targetProgress = if (stats.totalJamaah > 0) {
                        stats.hadirHariIni.toFloat() / stats.totalJamaah.toFloat()
                    } else 0f

                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        label = "progress"
                    )

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = EmeraldPrimary,
                        trackColor = SurfaceVariantCream
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stats.hadirHariIni} dari ${stats.totalJamaah} Jamaah Hadir",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = StatusSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${stats.belumHadir} Belum Hadir",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = StatusWarning,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // 5. Monthly & Accumulation Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.5.dp, GoldBorder.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Ringkasan Absensi Keseluruhan",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(GoldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Monthly",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sesi Bulan Ini",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${stats.hadirBulanIni} Sesi",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(56.dp)
                                .background(GoldBorder.copy(alpha = 0.5f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(GoldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FactCheck,
                                    contentDescription = "Total",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Akumulasi Absen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${stats.totalKehadiran} Record",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Camera Scanner Overlaid Screen
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                CameraScannerView(
                    onBarcodeScanned = { barcode ->
                        viewModel.processQrScan(barcode)
                    }
                )

                // Close Scanner Button
                IconButton(
                    onClick = {
                        isScanning = false
                        viewModel.resetScanStatus()
                    },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Scanner",
                        tint = Color.White
                    )
                }

                // Text Tip at Bottom
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 48.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Posisikan QR Code kartu jamaah di dalam kotak hijau.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Live Scan Result Dialog Handler
        when (val result = scanStatus) {
            is ScanStatus.Processing -> {
                Dialog(onDismissRequest = { /* Prevent cancellation during save */ }) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Memproses absensi...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            is ScanStatus.Success -> {
                Dialog(onDismissRequest = {
                    viewModel.resetScanStatus()
                }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ABSENSI BERHASIL!",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = StatusSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = result.nama,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "HP: ${result.noHp}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Tanggal", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(text = result.tanggal, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Waktu", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(text = result.waktu, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    viewModel.resetScanStatus()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Lanjutkan Absen", color = Color.White)
                            }
                        }
                    }
                }
            }
            is ScanStatus.Error -> {
                Dialog(onDismissRequest = {
                    viewModel.resetScanStatus()
                }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFEBEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning/Error",
                                    tint = StatusError,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ABSENSI DITOLAK",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = StatusError,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    viewModel.resetScanStatus()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Kembali", color = Color.White)
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun AnimatedStatCard(
    title: String,
    value: Int,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "statValue"
    )

    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.5.dp, GoldBorder.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (unit == "%") "$animatedValue%" else "$animatedValue",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (unit != "%" && unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

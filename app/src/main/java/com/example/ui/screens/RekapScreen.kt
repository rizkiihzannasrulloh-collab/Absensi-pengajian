package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyAttendanceStat
import com.example.data.Jamaah
import com.example.data.JamaahMonthlyStat
import com.example.data.Kehadiran
import com.example.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RekapScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsState()
    val dailyStats by viewModel.dailyAttendanceStats.collectAsState()
    val monthlyStats by viewModel.monthlyStatsPerJamaah.collectAsState()
    val totalSessions by viewModel.totalSessionsInMonth.collectAsState()

    val allJamaah by viewModel.allActiveJamaah.collectAsState()
    val allKehadiranWithJamaah by viewModel.allKehadiranWithJamaah.collectAsState()
    val dashboardStats by viewModel.dashboardStats.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Hari Ini, 1: Rekap Bulan Ini

    // Current selected Month Calendar formatting
    val currentCal = remember(selectedYearMonth) {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            cal.time = sdf.parse(selectedYearMonth) ?: Date()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cal
    }

    val monthDisplayName = remember(currentCal) {
        SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(currentCal.time)
    }

    // Filter today's attendance list
    val todayDateStr = remember { viewModel.getCurrentDateString() }
    val todayKehadiranList = remember(allKehadiranWithJamaah, todayDateStr) {
        allKehadiranWithJamaah.filter { it.first.tanggal == todayDateStr }
    }

    // Map monthly attendance per jamaah
    val jamaahMonthlyStatMap = remember(monthlyStats) {
        monthlyStats.associateBy { it.jamaahId }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header & Month Navigator
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    val cal = currentCal.clone() as Calendar
                    cal.add(Calendar.MONTH, -1)
                    val newYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
                    viewModel.setSelectedYearMonth(newYearMonth)
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan Sebelumnya")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = monthDisplayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(onClick = {
                    val cal = currentCal.clone() as Calendar
                    cal.add(Calendar.MONTH, 1)
                    val newYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
                    viewModel.setSelectedYearMonth(newYearMonth)
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Bulan Berikutnya")
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Overview Summary Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Today Percent Card
                    val todayPct = if (dashboardStats.totalJamaah > 0) {
                        (dashboardStats.hadirHariIni * 100) / dashboardStats.totalJamaah
                    } else 0

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Kehadiran Hari Ini",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$todayPct%",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${dashboardStats.hadirHariIni} dari ${dashboardStats.totalJamaah} Jamaah",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Month Session Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Sesi Pengajian Bulan Ini",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalSessions Sesi",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Total Jamaah Terdaftar: ${allJamaah.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Visual Donut Chart Component
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Persentase Kehadiran Hari Ini",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        AttendanceDonutChart(
                            hadirCount = dashboardStats.hadirHariIni,
                            totalCount = dashboardStats.totalJamaah,
                            modifier = Modifier.size(180.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Chart Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LegendItem(color = Color(0xFF2E7D32), label = "Hadir (${dashboardStats.hadirHariIni})")
                            LegendItem(color = Color(0xFFFFA000), label = "Belum Hadir (${dashboardStats.belumHadir})")
                        }
                    }
                }
            }

            // 3. Visual Bar Chart Component (Daily Trend in Month)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Tren Kehadiran Harian",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Jumlah jamaah hadir per tanggal pengajian ($monthDisplayName)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (dailyStats.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum ada data kehadiran untuk bulan ini",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            MonthlyAttendanceBarChart(
                                dailyStats = dailyStats,
                                totalJamaah = dashboardStats.totalJamaah,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }
                }
            }

            // 4. Tab Selector & Detailed List
            item {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                "Hadir Hari Ini (${todayKehadiranList.size})",
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Text(
                                "Rekap Bulan Ini",
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content 0: Jamaah Hadir Hari Ini
            if (activeTab == 0) {
                if (todayKehadiranList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum ada jamaah yang absen hari ini.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(todayKehadiranList, key = { it.first.id }) { item ->
                        val kehadiran = item.first
                        val jamaah = item.second

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE8F5E9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = jamaah?.nama ?: "Jamaah (ID: ${kehadiran.jamaahId.take(6)})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "HP: ${jamaah?.noHp ?: "-"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = kehadiran.waktu,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab Content 1: Rekap Bulan Ini per Jamaah
                if (allJamaah.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum ada data jamaah terdaftar.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(allJamaah, key = { it.id }) { jamaah ->
                        val stat = jamaahMonthlyStatMap[jamaah.id]
                        val sessionHadir = stat?.totalHadir ?: 0
                        val pct = if (totalSessions > 0) {
                            (sessionHadir * 100) / totalSessions
                        } else 0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = jamaah.nama,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "HP: ${jamaah.noHp}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (pct >= 75) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                                    ) {
                                        Text(
                                            text = "$sessionHadir / $totalSessions Sesi ($pct%)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (pct >= 75) Color(0xFF2E7D32) else Color(0xFFF57F17)
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val progress = if (totalSessions > 0) sessionHadir.toFloat() / totalSessions.toFloat() else 0f
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (pct >= 75) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AttendanceDonutChart(
    hadirCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val sweepHadirAngle = remember(hadirCount, totalCount) {
        if (totalCount > 0) (hadirCount.toFloat() / totalCount.toFloat()) * 360f else 0f
    }

    val animatedSweep = remember { Animatable(0f) }

    LaunchedEffect(sweepHadirAngle) {
        animatedSweep.animateTo(
            targetValue = sweepHadirAngle,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val percentText = if (totalCount > 0) "${((hadirCount * 100) / totalCount)}%" else "0%"

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 28.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            // Background arc (Belum hadir)
            drawArc(
                color = Color(0xFFFFECB3), // Light Amber
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Hadir arc
            drawArc(
                color = Color(0xFF2E7D32), // Emerald Green
                startAngle = -90f,
                sweepAngle = animatedSweep.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = percentText,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Tingkat Kehadiran",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MonthlyAttendanceBarChart(
    dailyStats: List<DailyAttendanceStat>,
    totalJamaah: Int,
    modifier: Modifier = Modifier
) {
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    val maxCount = remember(dailyStats, totalJamaah) {
        val highest = dailyStats.maxOfOrNull { it.totalHadir } ?: 1
        kotlin.math.max(highest, kotlin.math.max(totalJamaah, 1))
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dailyStats) {
                    detectTapGestures { tapOffset ->
                        val barWidth = size.width / dailyStats.size
                        val index = (tapOffset.x / barWidth).toInt()
                        if (index in dailyStats.indices) {
                            selectedBarIndex = if (selectedBarIndex == index) null else index
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val barCount = dailyStats.size
            val barWidth = (width / barCount) * 0.65f
            val spacing = (width / barCount) * 0.35f

            val bottomY = height - 24.dp.toPx()
            val availableHeight = bottomY - 20.dp.toPx()

            // Draw horizontal reference gridlines
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = bottomY - (availableHeight * (i.toFloat() / gridLines))
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }

            // Draw Bars
            dailyStats.forEachIndexed { index, stat ->
                val x = (index * (barWidth + spacing)) + (spacing / 2)
                val barHeight = (stat.totalHadir.toFloat() / maxCount.toFloat()) * availableHeight
                val topY = bottomY - barHeight

                val isSelected = selectedBarIndex == index
                val barColor = if (isSelected) Color(0xFF1B5E20) else Color(0xFF2E7D32)

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        // Selected bar tooltip popup
        selectedBarIndex?.let { index ->
            if (index in dailyStats.indices) {
                val stat = dailyStats[index]
                val dayStr = try {
                    stat.tanggal.substring(8)
                } catch (e: Exception) {
                    stat.tanggal
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Tanggal $dayStr: ${stat.totalHadir} Jamaah Hadir",
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
        )
    }
}

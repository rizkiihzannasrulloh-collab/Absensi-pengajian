package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.AppViewModel
import com.example.utils.ExportUtils
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val isAuthorized by viewModel.isAdminAuthorized.collectAsState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (!isAuthorized) {
            AdminPinGate(
                viewModel = viewModel,
                onAuthorized = { viewModel.setAdminAuthorized(true) }
            )
        } else {
            AdminControlPanel(
                viewModel = viewModel,
                onLock = { viewModel.setAdminAuthorized(false) }
            )
        }
    }
}

@Composable
fun AdminPinGate(
    viewModel: AppViewModel,
    onAuthorized: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, size = 32.dp, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "MENU KEAMANAN PANITIA",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Pengaturan dilindungi PIN. Gunakan PIN utama (Default: 1234)",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Circles indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..4) {
                val isFilled = pin.length >= i
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isFilled) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }

        if (errorMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Dynamic Native-feeling Keypad Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "⌫")
            )

            for (row in rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (char in row) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    scope.launch {
                                        when (char) {
                                            "C" -> {
                                                pin = ""
                                                errorMsg = ""
                                            }
                                            "⌫" -> {
                                                if (pin.isNotEmpty()) {
                                                    pin = pin.dropLast(1)
                                                    errorMsg = ""
                                                }
                                            }
                                            else -> {
                                                if (pin.length < 4) {
                                                    pin += char
                                                    errorMsg = ""
                                                    if (pin.length == 4) {
                                                        val success = viewModel.verifyAdminPin(pin)
                                                        if (success) {
                                                            onAuthorized()
                                                        } else {
                                                            pin = ""
                                                            errorMsg = "PIN keamanan salah!"
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminControlPanel(
    viewModel: AppViewModel,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Modify PIN forms
    var isChangePinVisible by remember { mutableStateOf(false) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var changePinError by remember { mutableStateOf("") }

    // Wiping database forms
    var isWipeDialogVisible by remember { mutableStateOf(false) }
    var wipeConfirmPin by remember { mutableStateOf("") }
    var wipeErrorMsg by remember { mutableStateOf("") }

    // Register Document Picker for JSON Restore
    val restorePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                viewModel.restoreDatabaseBackup(uri) { success ->
                    if (success) {
                        Toast.makeText(context, "Database Berhasil Dipulihkan!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Gagal memulihkan database. Cek format file JSON.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pengaturan Admin",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )

            Button(
                onClick = onLock,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, size = 14.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kunci Panel", fontSize = 12.sp, color = Color.White)
            }
        }

        // Section 1: Backup & Restore Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, size = 20.dp, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Backup & Restore Lokal (JSON)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Gunakan file cadangan offline ini untuk memindahkan database secara manual, menyimpan cadangan mingguan, atau mentransfer data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Backup trigger button
                    Button(
                        onClick = {
                            val file = viewModel.exportDatabaseBackup()
                            if (file != null) {
                                ExportUtils.shareFile(context, file, "application/json", "Bagikan Cadangan Database")
                            } else {
                                Toast.makeText(context, "Gagal membuat backup JSON", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cadangkan")
                    }

                    // Restore trigger button
                    OutlinedButton(
                        onClick = {
                            restorePickerLauncher.launch("application/json")
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pulihkan")
                    }
                }
            }
        }

        // Section 2: Security PIN Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, size = 20.dp, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Keamanan PIN Admin",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ganti PIN default untuk mencegah manipulasi data kehadiran oleh pihak yang tidak bertanggung jawab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(20.dp))

                if (!isChangePinVisible) {
                    Button(
                        onClick = { isChangePinVisible = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ganti PIN Keamanan")
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = oldPin,
                            onValueChange = { if (it.length <= 4) oldPin = it },
                            label = { Text("PIN Saat Ini") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 4) newPin = it },
                            label = { Text("PIN Baru (4 Digit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 4) confirmPin = it },
                            label = { Text("Konfirmasi PIN Baru") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (changePinError.isNotEmpty()) {
                            Text(text = changePinError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                isChangePinVisible = false
                                oldPin = ""
                                newPin = ""
                                confirmPin = ""
                                changePinError = ""
                            }) {
                                Text("Batal")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (newPin.length != 4) {
                                        changePinError = "PIN baru harus 4 digit."
                                    } else if (newPin != confirmPin) {
                                        changePinError = "Konfirmasi PIN baru tidak cocok."
                                    } else {
                                        viewModel.updateAdminPin(oldPin, newPin) { success ->
                                            if (success) {
                                                Toast.makeText(context, "PIN Admin Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                                                isChangePinVisible = false
                                                oldPin = ""
                                                newPin = ""
                                                confirmPin = ""
                                                changePinError = ""
                                            } else {
                                                changePinError = "PIN saat ini salah."
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Simpan PIN")
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Hard Reset Application Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, size = 20.dp, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hard Reset Aplikasi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tindakan ini akan MENGHAPUS seluruh database jamaah, seluruh log absensi, dan mereset PIN keamanan kembali ke '1234'. Tindakan ini permanen!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { isWipeDialogVisible = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus Seluruh Data", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Hard Reset Warning Dialog
    if (isWipeDialogVisible) {
        Dialog(onDismissRequest = { isWipeDialogVisible = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, size = 28.dp, tint = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Apakah Anda Sangat Yakin?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Seluruh data jamaah dan riwayat kehadiran di HP ini akan dihilangkan permanen. Ketik PIN Admin untuk mengonfirmasi hard-reset.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = wipeConfirmPin,
                        onValueChange = { if (it.length <= 4) wipeConfirmPin = it },
                        label = { Text("Ketik PIN Admin") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (wipeErrorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = wipeErrorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        TextButton(onClick = {
                            isWipeDialogVisible = false
                            wipeConfirmPin = ""
                            wipeErrorMsg = ""
                        }) {
                            Text("Batal")
                        }
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            onClick = {
                                viewModel.resetAppDatabase(wipeConfirmPin) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Aplikasi di-reset total!", Toast.LENGTH_LONG).show()
                                        isWipeDialogVisible = false
                                        wipeConfirmPin = ""
                                        wipeErrorMsg = ""
                                        onLock() // Lock panel again
                                    } else {
                                        wipeErrorMsg = "PIN konfirmasi salah."
                                    }
                                }
                            }
                        ) {
                            Text("Hard Reset", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Jamaah
import com.example.ui.AppViewModel
import com.example.utils.BarcodeUtils
import com.example.utils.ExportUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JamaahScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val jamaahList by viewModel.allActiveJamaah.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedJamaahForCard by remember { mutableStateOf<Jamaah?>(null) }
    var isAddDialogVisible by remember { mutableStateOf(false) }
    var jamaahToEdit by remember { mutableStateOf<Jamaah?>(null) }

    // Admin Pin verification for delete
    var jamaahToDeleteId by remember { mutableStateOf<String?>(null) }
    var isPinVerificationVisible by remember { mutableStateOf(false) }

    // Search filter logic
    val filteredList = remember(jamaahList, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            jamaahList
        } else {
            val q = searchQuery.lowercase(Locale.getDefault())
            jamaahList.filter {
                it.nama.lowercase(Locale.getDefault()).contains(q) ||
                it.noHp.contains(q) ||
                it.alamat.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar & Add Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Cari jamaah (Nama/HP/Alamat)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
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

                Button(
                    onClick = { isAddDialogVisible = true },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Jamaah")
                }
            }

            // List of Jamaah
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Belum ada data jamaah." else "Jamaah tidak ditemukan.",
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
                    items(filteredList) { jamaah ->
                        JamaahItemRow(
                            jamaah = jamaah,
                            onCardClick = { selectedJamaahForCard = jamaah },
                            onEditClick = { jamaahToEdit = jamaah },
                            onDeleteClick = {
                                jamaahToDeleteId = jamaah.id
                                isPinVerificationVisible = true
                            }
                        )
                    }
                }
            }
        }

        // Add Dialog
        if (isAddDialogVisible) {
            AddEditJamaahDialog(
                onDismiss = { isAddDialogVisible = false },
                onSave = { name, hp, address, note ->
                    viewModel.addJamaah(name, hp, address, note)
                    Toast.makeText(context, "Jamaah berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Edit Dialog
        if (jamaahToEdit != null) {
            val original = jamaahToEdit!!
            AddEditJamaahDialog(
                title = "Edit Data Jamaah",
                initialName = original.nama,
                initialHp = original.noHp,
                initialAddress = original.alamat,
                initialNote = original.keterangan,
                onDismiss = { jamaahToEdit = null },
                onSave = { name, hp, address, note ->
                    viewModel.updateJamaah(original.copy(nama = name, noHp = hp, alamat = address, keterangan = note))
                    Toast.makeText(context, "Data jamaah diperbarui!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Pin Verification for Delete Action
        if (isPinVerificationVisible) {
            AdminPinGateDialog(
                viewModel = viewModel,
                onDismiss = {
                    isPinVerificationVisible = false
                    jamaahToDeleteId = null
                },
                onSuccess = {
                    jamaahToDeleteId?.let { id ->
                        viewModel.deleteJamaah(id)
                        Toast.makeText(context, "Data jamaah dihapus.", Toast.LENGTH_SHORT).show()
                    }
                    isPinVerificationVisible = false
                    jamaahToDeleteId = null
                }
            )
        }

        // Member Card Viewer Modal
        if (selectedJamaahForCard != null) {
            MemberCardDialog(
                jamaah = selectedJamaahForCard!!,
                context = context,
                onDismiss = { selectedJamaahForCard = null }
            )
        }
    }
}

@Composable
fun JamaahItemRow(
    jamaah: Jamaah,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = jamaah.nama,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, size = 14.dp, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = jamaah.noHp, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, size = 14.dp, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (jamaah.alamat.length > 30) jamaah.alamat.substring(0, 27) + "..." else jamaah.alamat,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCardClick) {
                    Icon(Icons.Default.AccountBox, contentDescription = "Member Card", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                }
            }
        }
    }
}

@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size)
    )
}

@Composable
fun AddEditJamaahDialog(
    title: String = "Tambah Jamaah Baru",
    initialName: String = "",
    initialHp: String = "",
    initialAddress: String = "",
    initialNote: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, hp: String, address: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var hp by remember { mutableStateOf(initialHp) }
    var address by remember { mutableStateOf(initialAddress) }
    var note by remember { mutableStateOf(initialNote) }

    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = hp,
                    onValueChange = { hp = it },
                    label = { Text("Nomor HP / WhatsApp") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat Tinggal") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Keterangan Tambahan") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (name.trim().isEmpty() || hp.trim().isEmpty() || address.trim().isEmpty()) {
                                errorMsg = "Harap isi semua kolom wajib (Nama, HP, Alamat)."
                            } else {
                                onSave(name.trim(), hp.trim(), address.trim(), note.trim())
                                onDismiss()
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPinGateDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
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
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, size = 24.dp, tint = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Konfirmasi PIN Admin",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tindakan ini memerlukan verifikasi PIN keamanan panitia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    placeholder = { Text("Masukkan 4 digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 20.sp, letterSpacing = 4.sp)
                )

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            scope.launch {
                                val verified = viewModel.verifyAdminPin(pin)
                                if (verified) {
                                    onSuccess()
                                } else {
                                    errorMsg = "PIN yang dimasukkan salah."
                                }
                            }
                        }
                    ) {
                        Text("Hapus Data", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MemberCardDialog(
    jamaah: Jamaah,
    context: Context,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(jamaah.id) { BarcodeUtils.generateQRCode(jamaah.id, 400, 400) }
    val barcodeBitmap = remember(jamaah.id) { BarcodeUtils.generateBarcode(jamaah.id, 400, 120) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header of member card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, size = 24.dp, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("KARTU ANGGOTA", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold))
                            Text("PENGAJIAN SYIAR OFFLINE", style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name and Profile block
                Text(
                    text = jamaah.nama,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Nomor HP: ${jamaah.noHp}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(text = "Alamat: ${jamaah.alamat}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(20.dp))

                // QR Code
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color.White)
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Barcode
                if (barcodeBitmap != null) {
                    Image(
                        bitmap = barcodeBitmap.asImageBitmap(),
                        contentDescription = "Barcode",
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = jamaah.id.take(18).uppercase() + "...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup", color = Color(0xFF1B5E20))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        onClick = {
                            if (qrBitmap != null) {
                                val uri = BarcodeUtils.saveAndShareBitmap(context, qrBitmap, "QR_${jamaah.nama.replace(" ", "_")}", "Share QR Code")
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, "Assalamu'alaikum ${jamaah.nama}, berikut adalah kartu QR Code absensi Pengajian Anda. Silakan tunjukkan saat memasuki ruangan.")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Bagikan QR Code"))
                                } else {
                                    Toast.makeText(context, "Gagal membagikan gambar", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bagikan QR", color = Color.White)
                    }
                }
            }
        }
    }
}

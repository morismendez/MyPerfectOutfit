package com.myperfectoutfit.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.myperfectoutfit.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myperfectoutfit.ui.viewmodel.UserViewModel
import android.net.Uri
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.myperfectoutfit.ui.state.CategoryFilter
import kotlinx.coroutines.launch

import com.myperfectoutfit.ui.components.ImageFramingDialog

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    var isRestoring by remember { mutableStateOf(false) }
    
    // Nuevo estado para el encuadre
    var imageToFrame by remember { mutableStateOf<Uri?>(null) }

    val selectableCategories = CategoryFilter.entries.filter { 
        it != CategoryFilter.ALL && it != CategoryFilter.LAUNDRY 
    }
    val activeCategories = remember { mutableStateListOf<CategoryFilter>().apply {
        addAll(listOf(CategoryFilter.SHIRTS, CategoryFilter.PANTS, CategoryFilter.SHOES))
    }}

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) imageToFrame = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) imageToFrame = tempCameraUri
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isRestoring = true
                val success = viewModel.restoreBackupFile(it)
                isRestoring = false
                if (success) {
                    Toast.makeText(context, "Restauración exitosa. Reiniciando...", Toast.LENGTH_LONG).show()
                    // Reiniciar la app de forma limpia
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    val mainIntent = android.content.Intent.makeRestartActivityTask(intent?.component)
                    context.startActivity(mainIntent)
                    Runtime.getRuntime().exit(0)
                } else {
                    Toast.makeText(context, "Fallo en la restauración.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val tempFile = File(context.cacheDir, "temp_camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            // Se podría mostrar un mensaje
        }
    }

    if (isRestoring) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { Text(stringResource(R.string.restoring_data_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    Text(stringResource(R.string.restoring_data_sub), style = MaterialTheme.typography.bodySmall)
                }
            }
        )
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text(stringResource(R.string.select_image_title)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.btn_take_photo)) },
                        leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showSourceDialog = false
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.btn_choose_gallery)) },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    imageToFrame?.let { uri ->
        ImageFramingDialog(
            imageUri = uri,
            onDismiss = { imageToFrame = null },
            onImageFramed = { framedUri ->
                selectedImageUri = framedUri
                imageToFrame = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .clickable { showSourceDialog = true },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Agregar foto",
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.welcome_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = stringResource(R.string.setup_profile_subtitle),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.full_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.customize_wardrobe_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = stringResource(R.string.select_used_categories),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectableCategories.forEach { category ->
                FilterChip(
                    selected = activeCategories.contains(category),
                    onClick = {
                        if (activeCategories.contains(category)) {
                            if (activeCategories.size > 1) activeCategories.remove(category)
                        } else {
                            activeCategories.add(category)
                        }
                    },
                    label = { Text(stringResource(category.displayNameRes)) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { 
                val profilePicPath = selectedImageUri?.let { uri ->
                    saveProfileImageToInternalStorage(context, uri)
                }
                val categoriesStr = activeCategories.joinToString(",") { it.name }
                viewModel.registerUser(
                    name = name, 
                    email = email, 
                    profilePictureUrl = profilePicPath, 
                    activeCategories = categoriesStr
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && email.contains("@") && activeCategories.isNotEmpty()
        ) {
            Text(stringResource(R.string.btn_ready))
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = stringResource(R.string.already_user_prompt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { restoreLauncher.launch("application/zip") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.CloudDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.restore_backup_btn))
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

private fun saveProfileImageToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

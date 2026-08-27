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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myperfectoutfit.ui.viewmodel.UserViewModel
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.myperfectoutfit.ui.state.CategoryFilter

import com.myperfectoutfit.ui.components.ImageFramingDialog

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    
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

    if (showSourceDialog) {
        // ... (contenido existente del diálogo de origen)
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Seleccionar Imagen") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Tomar Foto") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showSourceDialog = false
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Elegir de Galería") },
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
                TextButton(onClick = { showSourceDialog = false }) { Text("Cancelar") }
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
            text = "¡Hola!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Configura tu perfil para comenzar",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Gemini API Key (Opcional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (apiKey.isEmpty()) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation()
        )
        TextButton(
            onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(
                text = "Obtén tu llave gratuita en Google AI Studio aquí",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Personaliza tu Armario",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = "Selecciona las categorías que utilizas:",
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
                    label = { Text(category.displayName) }
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
                viewModel.registerUser(name, email, profilePicPath, categoriesStr, apiKey.trim()) 
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && email.contains("@") && activeCategories.isNotEmpty()
        ) {
            Text("¡Todo listo!")
        }

        Spacer(modifier = Modifier.height(24.dp))
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

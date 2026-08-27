package com.myperfectoutfit.ui.screens

import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import java.io.File
import com.myperfectoutfit.ui.navigation.Screen
import com.myperfectoutfit.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.myperfectoutfit.ui.components.ImageFramingDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userViewModel: UserViewModel = hiltViewModel()
) {
    val userState by userViewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showEditProfile by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    var tempBackupFile by remember { mutableStateOf<File?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            tempBackupFile?.let { file ->
                scope.launch {
                    try {
                        context.contentResolver.openOutputStream(targetUri)?.use { output ->
                            file.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        Toast.makeText(context, "Respaldo guardado exitosamente.", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al guardar el archivo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val success = userViewModel.restoreBackupFile(it)
                if (success) {
                    Toast.makeText(context, "Restauración exitosa. Reinicia la app.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Fallo en la restauración.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (userState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (userState.user == null) {
        RegistrationScreen()
        return
    }

    val items = listOf(
        Screen.Wardrobe,
        Screen.AiAdvisor,
        Screen.History,
        Screen.Laundry
    )

    if (showEditProfile) {
        EditProfileDialog(
            user = userState.user!!,
            styleRules = userState.styleRules,
            currentGeminiKey = userViewModel.getGeminiApiKey(),
            onDismiss = { showEditProfile = false },
            onSave = { name, email, photo, categories, apiKey ->
                userViewModel.updateUser(name, email, photo, categories, apiKey)
                showEditProfile = false
            },
            onAddRule = { title, desc -> userViewModel.addStyleRule(title, desc) },
            onToggleRule = { id, active -> userViewModel.toggleStyleRule(id, active) },
            onDeleteRule = { rule -> userViewModel.deleteStyleRule(rule) }
        )
    }

    val onBackup = {
        scope.launch {
            val file = userViewModel.createBackupFile()
            if (file != null) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Guardar Respaldo"))
            } else {
                Toast.makeText(context, "Error al crear respaldo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val currentScreen = items.find { it.route == currentRoute } ?: Screen.AiAdvisor

            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEditProfile = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userState.user?.profilePictureUrl != null) {
                                AsyncImage(
                                    model = userState.user?.profilePictureUrl,
                                    contentDescription = "Perfil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.padding(8.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = userState.user?.name ?: "",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = currentScreen.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Compartir Respaldo") },
                                leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onBackup()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Guardar en Carpeta") },
                                leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        val file = userViewModel.createBackupFile()
                                        if (file != null) {
                                            tempBackupFile = file
                                            createDocumentLauncher.launch("myperfectoutfit_backup_${System.currentTimeMillis()}.zip")
                                        } else {
                                            Toast.makeText(context, "Error al crear respaldo.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Restaurar desde Archivo") },
                                leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    restoreLauncher.launch("application/zip")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Google Drive") },
                                leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    Toast.makeText(context, "Configura Drive en Google Cloud.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.AiAdvisor.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Wardrobe.route) {
                WardrobeScreen()
            }
            composable(Screen.AiAdvisor.route) {
                OutfitGeneratorScreen()
            }
            composable(Screen.Laundry.route) {
                LaundryScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    user: com.myperfectoutfit.data.local.entities.UserEntity,
    styleRules: List<com.myperfectoutfit.data.local.entities.StyleRuleEntity> = emptyList(),
    currentGeminiKey: String? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, photo: String?, categories: String, apiKey: String?) -> Unit,
    onAddRule: (String, String) -> Unit,
    onToggleRule: (Long, Boolean) -> Unit,
    onDeleteRule: (com.myperfectoutfit.data.local.entities.StyleRuleEntity) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var geminiKey by remember { mutableStateOf(currentGeminiKey ?: "") }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { 
        mutableStateOf(user.profilePictureUrl?.let { Uri.parse(it) }) 
    }
    
    val selectableCategories = com.myperfectoutfit.ui.state.CategoryFilter.entries.filter { 
        it != com.myperfectoutfit.ui.state.CategoryFilter.ALL && it != com.myperfectoutfit.ui.state.CategoryFilter.LAUNDRY 
    }
    val activeCategories = remember { 
        val saved = user.activeCategories.split(",").mapNotNull { name ->
            try { com.myperfectoutfit.ui.state.CategoryFilter.valueOf(name) } catch(e: Exception) { null }
        }
        mutableStateListOf<com.myperfectoutfit.ui.state.CategoryFilter>().apply { addAll(saved) }
    }

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var showSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    // Nuevo estado para el encuadre
    var imageToFrame by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri ?: selectedImageUri
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
            val tempFile = File(context.cacheDir, "profile_edit_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    if (showSourceDialog) {
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

    if (showAddRuleDialog) {
        AddStyleRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onSave = { title, desc ->
                onAddRule(title, desc)
                showAddRuleDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .clickable { showSourceDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    label = { Text("Gemini API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (geminiKey.isEmpty()) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                TextButton(
                    onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Obtén tu llave gratuita aquí",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider()

                Text(text = "Categorías de Armario", style = MaterialTheme.typography.titleSmall)
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

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Reglas de Estilo", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { showAddRuleDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir Regla", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                styleRules.forEach { rule ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (rule.isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rule.isActive,
                                onCheckedChange = { onToggleRule(rule.id, it) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = rule.title, style = MaterialTheme.typography.labelLarge)
                                Text(text = rule.description, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onDeleteRule(rule) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val photoPath = if (selectedImageUri?.toString() != user.profilePictureUrl) {
                        selectedImageUri?.let { saveProfileImageToInternalStorage(context, it) }
                    } else user.profilePictureUrl
                    val categoriesStr = activeCategories.joinToString(",") { it.name }
                    onSave(name, email, photoPath, categoriesStr, geminiKey.trim()) 
                },
                enabled = name.isNotBlank() && email.contains("@") && activeCategories.isNotEmpty()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AddStyleRuleDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Regla de Estilo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    placeholder = { Text("Ej: Viernes Casual") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descripción") },
                    placeholder = { Text("Ej: Sin corbata y usar jeans...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, desc) }, enabled = title.isNotBlank() && desc.isNotBlank()) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun saveProfileImageToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "profile_update_${System.currentTimeMillis()}.jpg")
        val outputStream = java.io.FileOutputStream(file)
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

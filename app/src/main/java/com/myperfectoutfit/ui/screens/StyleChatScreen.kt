package com.myperfectoutfit.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myperfectoutfit.ui.viewmodel.ChatMessage
import com.myperfectoutfit.ui.viewmodel.StyleChatViewModel
import com.myperfectoutfit.ui.viewmodel.SuitcaseItem
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleChatScreen(
    viewModel: StyleChatViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Borrar Historial") },
            text = { Text("¿Estás seguro de que quieres borrar toda la conversación? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChat()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            selectedBitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            selectedImageUri = tempCameraUri
            tempCameraUri?.let {
                selectedBitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val file = File(context.cacheDir, "chat_camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Mostrar errores en snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Scroll al último mensaje automáticamente
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asesoría de Estilo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Borrar historial", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Lista de mensajes
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.messages) { message ->
                    ChatBubble(message)
                }
                
                if (uiState.isLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Vista previa de imagen seleccionada
            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .size(100.dp)
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { 
                            selectedImageUri = null
                            selectedBitmap = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Quitar", 
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Entrada de texto
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) }) {
                        Icon(
                            Icons.Default.CameraAlt, 
                            contentDescription = "Tomar foto",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(
                            Icons.Default.AddPhotoAlternate, 
                            contentDescription = "Adjuntar foto",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Escribe tu duda de estilo...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FilledIconButton(
                        onClick = {
                            viewModel.sendMessage(inputText, selectedImageUri, selectedBitmap)
                            inputText = ""
                            selectedImageUri = null
                            selectedBitmap = null
                        },
                        enabled = (inputText.isNotBlank() || selectedImageUri != null) && !uiState.isLoading,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            tonalElevation = 1.dp
        ) {
            Column {
                if (message.imageUri != null) {
                    AsyncImage(
                        model = message.imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(4.dp)
                            .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (message.suggestedItems.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = contentColor.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "Prendas sugeridas:",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(message.suggestedItems) { item ->
                            SuggestedItemCard(item, contentColor)
                        }
                    }
                }
            }
        }
        Text(
            text = if (message.isUser) "Tú" else "Asesor IA",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SuggestedItemCard(item: SuitcaseItem, contentColor: Color) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(130.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f),
            contentColor = contentColor
        ),
        border = BorderStroke(0.5.dp, contentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (item.imageUrl != null) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Checkroom, 
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(32.dp),
                        tint = contentColor.copy(alpha = 0.5f)
                    )
                }
                
                if (item.isDirty) {
                    Icon(
                        Icons.Default.LocalLaundryService,
                        contentDescription = "Sucia",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = item.name,
                style = MaterialTheme.labelExtraSmall(),
                modifier = Modifier.padding(4.dp),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun MaterialTheme.labelExtraSmall() = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "El asesor está escribiendo...",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        LinearProgressIndicator(
            modifier = Modifier.width(40.dp).height(2.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

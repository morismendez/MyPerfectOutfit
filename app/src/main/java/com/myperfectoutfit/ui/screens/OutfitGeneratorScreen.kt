package com.myperfectoutfit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.ui.viewmodel.OutfitViewModel
import com.myperfectoutfit.ui.viewmodel.RecommendedOutfit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitGeneratorScreen(
    viewModel: OutfitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedOutfitToConfirm by remember { mutableStateOf<RecommendedOutfit?>(null) }
    var showGarmentPicker by remember { mutableStateOf(false) }

    // Diálogo para seleccionar prenda base
    if (showGarmentPicker) {
        GarmentSelectionDialog(
            availableGarments = uiState.availableGarments,
            selectedGarments = uiState.baseGarments,
            onDismiss = { showGarmentPicker = false },
            onToggleGarment = { viewModel.toggleBaseGarment(it) }
        )
    }

    // Diálogo de confirmación antes de mover a lavandería
    if (selectedOutfitToConfirm != null) {
        AlertDialog(
            onDismissRequest = { selectedOutfitToConfirm = null },
            icon = { Icon(Icons.Default.DryCleaning, contentDescription = null) },
            title = { Text("Confirmar Outfit de Hoy") },
            text = { Text("Al seleccionar 'Usar Hoy', las prendas elegidas pasarán automáticamente a la sección de Lavandería.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedOutfitToConfirm?.let { viewModel.confirmOutfit(it) }
                        selectedOutfitToConfirm = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedOutfitToConfirm = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Entrada de preferencia puntual del usuario
        OutlinedTextField(
            value = uiState.userInstruction,
            onValueChange = { viewModel.onInstructionChanged(it) },
            label = { Text("Preferencia o enfoque para hoy (Opcional)") },
            placeholder = { Text("Ej: Quiero usar mi camisa lila...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sección de Prendas Base
        Text(text = "Prendas Base (Opcional)", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showGarmentPicker = true }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Añadir Prenda Base", tint = MaterialTheme.colorScheme.primary)
            }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.baseGarments) { item ->
                    Box {
                        Card(
                            modifier = Modifier.size(50.dp),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            val imageUrl = when (item) {
                                is ShirtEntity -> item.imageUrl
                                is PantEntity -> item.imageUrl
                                is ShoeEntity -> item.imageUrl
                                is TieEntity -> item.imageUrl
                                is WatchEntity -> item.imageUrl
                                is FragranceEntity -> item.imageUrl
                                is JacketEntity -> item.imageUrl
                                is BagEntity -> item.imageUrl
                                is DressEntity -> item.imageUrl
                                is SkirtEntity -> item.imageUrl
                                else -> ""
                            }
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Quitar",
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                                .clickable { viewModel.toggleBaseGarment(item) },
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.consultAiForOutfit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (uiState.recommendations.isEmpty()) "Generar Recomendación" else "Generar otra opción")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.weight(1f)
        ) {
            if (uiState.isLoading && uiState.recommendations.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Analizando disponibilidad y combinaciones...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (uiState.isOutfitConfirmed) {
                // Estado de éxito tras confirmar
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "¡Outfit registrado con éxito!",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Las prendas seleccionadas han sido movidas a la lavandería.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "Error al consultar la IA",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState.isLoading) {
                        item {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }

                    items(uiState.recommendations, key = { it.id }) { outfit ->
                        RecommendationCard(
                            outfit = outfit,
                            onUseToday = { selectedOutfitToConfirm = outfit }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GarmentSelectionDialog(
    availableGarments: Map<String, List<Any>>,
    selectedGarments: List<Any>,
    onDismiss: () -> Unit,
    onToggleGarment: (Any) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Prenda Base") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                availableGarments.forEach { (category, garments) ->
                    if (garments.isNotEmpty()) {
                        item {
                            Text(text = category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        items(garments) { garment ->
                            val isSelected = selectedGarments.contains(garment)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleGarment(garment) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(modifier = Modifier.size(40.dp)) {
                                    val imageUrl = when (garment) {
                                        is ShirtEntity -> garment.imageUrl
                                        is PantEntity -> garment.imageUrl
                                        is ShoeEntity -> garment.imageUrl
                                        is TieEntity -> garment.imageUrl
                                        is WatchEntity -> garment.imageUrl
                                        is FragranceEntity -> garment.imageUrl
                                        is JacketEntity -> garment.imageUrl
                                        is BagEntity -> garment.imageUrl
                                        is DressEntity -> garment.imageUrl
                                        is SkirtEntity -> garment.imageUrl
                                        else -> ""
                                    }
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = when (garment) {
                                        is ShirtEntity -> "${garment.brand} ${garment.primaryColor}"
                                        is PantEntity -> "${garment.brand ?: "Pantalón"} ${garment.primaryColor}"
                                        is ShoeEntity -> "${garment.brand} ${garment.color}"
                                        is TieEntity -> "Corbata ${garment.colorRange}"
                                        is WatchEntity -> "${garment.brand} ${garment.model}"
                                        is FragranceEntity -> "${garment.brand} ${garment.name}"
                                        is JacketEntity -> "${garment.brand ?: "Chaqueta"} ${garment.color}"
                                        is BagEntity -> "${garment.brand ?: "Bolso"} ${garment.color}"
                                        is DressEntity -> "${garment.brand ?: "Vestido"} ${garment.color}"
                                        is SkirtEntity -> "${garment.brand ?: "Falda"} ${garment.color}"
                                        else -> "Prenda"
                                    },
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Checkbox(checked = isSelected, onCheckedChange = { onToggleGarment(garment) })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Listo") }
        }
    )
}

@Composable
fun RecommendationCard(
    outfit: RecommendedOutfit,
    onUseToday: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Propuesta Sugerida",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sección de Imágenes de prendas recomendadas
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedItems = listOfNotNull(
                    outfit.shirt,
                    outfit.pant,
                    outfit.shoe,
                    outfit.tie,
                    outfit.watch,
                    outfit.fragrance,
                    outfit.jacket,
                    outfit.bag,
                    outfit.dress,
                    outfit.skirt
                )

                items(selectedItems) { item ->
                    Card(
                        modifier = Modifier.size(width = 80.dp, height = 100.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        val imageUrl = when (item) {
                            is ShirtEntity -> item.imageUrl
                            is PantEntity -> item.imageUrl
                            is ShoeEntity -> item.imageUrl
                            is TieEntity -> item.imageUrl
                            is WatchEntity -> item.imageUrl
                            is FragranceEntity -> item.imageUrl
                            is JacketEntity -> item.imageUrl
                            is BagEntity -> item.imageUrl
                            is DressEntity -> item.imageUrl
                            is SkirtEntity -> item.imageUrl
                            else -> ""
                        }

                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = outfit.text,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onUseToday,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DryCleaning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Usar este Outfit Hoy")
            }
        }
    }
}

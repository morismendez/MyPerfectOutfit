package com.myperfectoutfit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myperfectoutfit.ui.viewmodel.LaundryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaundryScreen(
    viewModel: LaundryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.isEmpty) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.DryCleaning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "La lavandería está vacía",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Todas tus prendas están limpias y disponibles en el armario.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Botón alternativo si no hay TopAppBar dedicado
                Button(onClick = { viewModel.markAllAsClean() }, enabled = !uiState.isEmpty) {
                    Text("Marcar todo como limpio")
                }
            }
        } else {
            Column {
                // Botón rápido de limpieza
                TextButton(
                    onClick = { viewModel.markAllAsClean() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Limpiar Todo")
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Seccion Camisas
                    if (uiState.laundryShirts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Camisas (${uiState.laundryShirts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(uiState.laundryShirts, key = { "shirt_${it.id}" }) { shirt ->
                            LaundryItemCard(
                                title = "${shirt.brand} - ${shirt.primaryColor}",
                                subtitle = "Estampado: ${shirt.pattern}",
                                imageUri = shirt.imageUrl,
                                onMarkClean = { viewModel.markShirtClean(shirt.id) }
                            )
                        }
                    }

                    // Seccion Pantalones
                    if (uiState.laundryPants.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pantalones (${uiState.laundryPants.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(uiState.laundryPants, key = { "pant_${it.id}" }) { pant ->
                            LaundryItemCard(
                                title = "${pant.brand ?: "Pantalón"} - ${pant.primaryColor}",
                                subtitle = "${pant.subType} - ${pant.material}",
                                imageUri = pant.imageUrl,
                                onMarkClean = { viewModel.markPantClean(pant.id) }
                            )
                        }
                    }

                    // Seccion Corbatas
                    if (uiState.laundryTies.isNotEmpty()) {
                        item {
                            Text(
                                text = "Corbatas (${uiState.laundryTies.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(uiState.laundryTies, key = { "tie_${it.id}" }) { tie ->
                            LaundryItemCard(
                                title = "Corbata ${tie.colorRange}",
                                subtitle = "Diseño: ${tie.pattern}",
                                imageUri = tie.imageUrl,
                                onMarkClean = { viewModel.markTieClean(tie.id) }
                            )
                        }
                    }

                    // Seccion Chaquetas
                    if (uiState.laundryJackets.isNotEmpty()) {
                        item {
                            Text(
                                text = "Chaquetas (${uiState.laundryJackets.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(uiState.laundryJackets, key = { "jacket_${it.id}" }) { jacket ->
                            LaundryItemCard(
                                title = "${jacket.brand ?: "Chaqueta"} - ${jacket.color}",
                                subtitle = "Tipo: ${jacket.type}",
                                imageUri = jacket.imageUrl,
                                onMarkClean = { viewModel.markJacketClean(jacket.id) }
                            )
                        }
                    }

                    // Seccion Vestidos
                    if (uiState.laundryDresses.isNotEmpty()) {
                        item {
                            Text(
                                text = "Vestidos (${uiState.laundryDresses.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(uiState.laundryDresses, key = { "dress_${it.id}" }) { dress ->
                            LaundryItemCard(
                                title = "${dress.brand ?: "Vestido"} - ${dress.color}",
                                subtitle = "Estilo: ${dress.length}",
                                imageUri = dress.imageUrl,
                                onMarkClean = { viewModel.markDressClean(dress.id) }
                            )
                        }
                    }

                    // Seccion Faldas
                    if (uiState.laundrySkirts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Faldas (${uiState.laundrySkirts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(uiState.laundrySkirts, key = { "skirt_${it.id}" }) { skirt ->
                            LaundryItemCard(
                                title = "${skirt.brand ?: "Falda"} - ${skirt.color}",
                                subtitle = "Estilo: ${skirt.style}",
                                imageUri = skirt.imageUrl,
                                onMarkClean = { viewModel.markSkirtClean(skirt.id) }
                            )
                        }
                    }

                    // Seccion Personalizada
                    if (uiState.laundryCustomGarments.isNotEmpty()) {
                        item {
                            Text(
                                text = "Otros (${uiState.laundryCustomGarments.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(uiState.laundryCustomGarments, key = { "custom_${it.id}" }) { garment ->
                            LaundryItemCard(
                                title = garment.attributeValues.split("|").firstOrNull()?.split(":")?.getOrNull(1) ?: "Prenda",
                                subtitle = garment.attributeValues.replace("|", " - "),
                                imageUri = garment.imageUrl,
                                onMarkClean = { viewModel.markCustomGarmentClean(garment) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LaundryItemCard(
    title: String,
    subtitle: String,
    imageUri: String?,
    onMarkClean: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(60.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onMarkClean) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Marcar como limpia",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

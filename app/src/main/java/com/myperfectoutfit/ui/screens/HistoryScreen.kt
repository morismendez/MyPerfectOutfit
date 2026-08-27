package com.myperfectoutfit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myperfectoutfit.data.local.dao.HistoryWithDetails
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Configuración del calendario integrado
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    // Sincronizar selección del calendario con el ViewModel
    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            // El DatePicker de M3 trabaja en UTC. 
            // Obtenemos la cadena YYYY-MM-DD interpretando esos millis como UTC
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val dateStr = sdf.format(Date(millis))
            viewModel.onDateSelected(dateStr)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Calendario Integrado
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.ui.graphics.RectangleShape
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null,
                headline = null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // Lista de Outfits
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.outfitsForDate.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sin registros para el ${uiState.selectedDate}",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.outfitsForDate) { history ->
                        HistoryItemCard(history, uiState.customGarments)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(history: HistoryWithDetails, allCustoms: List<CustomGarmentEntity>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Outfit usado:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Ver menos" else "Ver detalles"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val standardItems = listOfNotNull(
                    history.shirt, history.pant, history.shoe, history.tie,
                    history.watch, history.fragrance, history.jacket,
                    history.bag, history.dress, history.skirt
                )
                
                val customIds = history.history.customGarmentIds?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
                val customItems = allCustoms.filter { customIds.contains(it.id) }

                items(standardItems) { garment ->
                    GarmentSmallCard(garment)
                }
                
                items(customItems) { garment ->
                    GarmentSmallCard(garment)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Explicación del estilo:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = history.history.summaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun GarmentSmallCard(garment: Any) {
    Card(modifier = Modifier.size(60.dp)) {
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
            is CustomGarmentEntity -> garment.imageUrl
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

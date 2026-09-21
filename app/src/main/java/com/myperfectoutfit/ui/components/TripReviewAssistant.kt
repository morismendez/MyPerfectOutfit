package com.myperfectoutfit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myperfectoutfit.data.local.entities.SuitcaseEntity
import com.myperfectoutfit.ui.viewmodel.SuitcaseItem
import com.myperfectoutfit.ui.viewmodel.TripReviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostTripReviewAssistant(
    suitcase: SuitcaseEntity,
    items: List<SuitcaseItem>,
    viewModel: TripReviewViewModel
) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissTripReview() },
        title = {
            Column {
                Text("¡Bienvenido de nuevo!", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tu viaje a '${suitcase.tripName}' ha finalizado. Vamos a organizar tu ropa de regreso.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                Text(
                    "Marca las prendas que deben ir a lavandería:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items) { item ->
                        Surface(
                            onClick = { viewModel.toggleReviewItem(item.id) },
                            shape = MaterialTheme.shapes.medium,
                            color = if (item.markAsDirty) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            border = if (item.markAsDirty) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.markAsDirty,
                                    onCheckedChange = { viewModel.toggleReviewItem(item.id) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(item.categoryDisplay, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (item.markAsDirty) {
                                    Icon(
                                        Icons.Default.LocalLaundryService,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.confirmTripReview() }) {
                Text("Finalizar Revisión")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissTripReview() }) {
                Text("Después")
            }
        }
    )
}

package com.myperfectoutfit.ui.screens

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.myperfectoutfit.R
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.ui.viewmodel.OutfitViewModel
import com.myperfectoutfit.ui.viewmodel.RecommendedOutfit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitGeneratorScreen(
    viewModel: OutfitViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedOutfitToConfirm by remember { mutableStateOf<RecommendedOutfit?>(null) }
    var showGarmentPicker by remember { mutableStateOf(value = false) }

    var speakingOutfitId by remember { mutableStateOf<String?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // Inicialización de TextToSpeech con listener de reproducción
    val tts = remember {
        lateinit var textToSpeech: TextToSpeech
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val deviceLocale = Locale.getDefault()
                val result = textToSpeech.setLanguage(deviceLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech.language = Locale.US
                }
            }
        }
        textToSpeech
    }

    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    speakingOutfitId = utteranceId
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    if (speakingOutfitId == utteranceId) {
                        speakingOutfitId = null
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    if (speakingOutfitId == utteranceId) {
                        speakingOutfitId = null
                    }
                }
            }
        })

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val toggleSpeak = { outfitId: String, text: String ->
        if (speakingOutfitId == outfitId && tts.isSpeaking) {
            tts.stop()
            speakingOutfitId = null
        } else {
            tts.stop()
            speakingOutfitId = outfitId
            val params = android.os.Bundle()
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, outfitId)
        }
    }

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
            title = { Text(stringResource(R.string.confirm_outfit_title)) },
            text = { Text(stringResource(R.string.confirm_outfit_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        selectedOutfitToConfirm?.let { viewModel.confirmOutfit(it) }
                        selectedOutfitToConfirm = null
                    }
                ) {
                    Text(stringResource(R.string.btn_accept))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedOutfitToConfirm = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (uiState.isTripModeActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.active_trip_mode, uiState.activeTripName ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Entrada de preferencia puntual del usuario
        OutlinedTextField(
            value = uiState.userInstruction,
            onValueChange = { viewModel.onInstructionChanged(it) },
            label = { Text(stringResource(R.string.user_instruction_label)) },
            placeholder = { Text(stringResource(R.string.user_instruction_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sección de Prendas Base
        Text(text = stringResource(R.string.base_garments_title), style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showGarmentPicker = true }) {
                Icon(Icons.Default.AddCircle, contentDescription = stringResource(R.string.add_base_garment), tint = MaterialTheme.colorScheme.primary)
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
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = stringResource(R.string.remove_garment),
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
            Text(stringResource(R.string.btn_generate_outfit))
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
                    Text(stringResource(R.string.analyzing_outfits), style = MaterialTheme.typography.bodyMedium)
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
                            text = stringResource(R.string.outfit_registered_success),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.outfit_moved_to_laundry),
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

                    itemsIndexed(uiState.recommendations, key = { _, outfit -> outfit.id }) { index, outfit ->
                        val proposalNumber = uiState.recommendations.size - index
                        RecommendationCard(
                            proposalNumber = proposalNumber,
                            outfit = outfit,
                            isSpeaking = speakingOutfitId == outfit.id,
                            onUseToday = { selectedOutfitToConfirm = outfit },
                            onToggleSpeak = { toggleSpeak(outfit.id, outfit.text) }
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
    selectedGarments: List<com.myperfectoutfit.domain.model.GarmentItem>,
    onDismiss: () -> Unit,
    onToggleGarment: (com.myperfectoutfit.domain.model.GarmentItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_base_garment)) },
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
                            Text(
                                text = if (category == "PERSONALIZADO" || category == "CUSTOM") category else stringResource(com.myperfectoutfit.ui.state.getCategoryStringRes(category)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(garments) { entity ->
                            val garmentItem = com.myperfectoutfit.domain.model.GarmentItem.fromEntity(entity)
                            if (garmentItem != null) {
                                val isSelected = selectedGarments.any { it.id == garmentItem.id && it.categoryTag == garmentItem.categoryTag }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleGarment(garmentItem) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Card(modifier = Modifier.size(40.dp)) {
                                        AsyncImage(
                                            model = garmentItem.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = garmentItem.formattedDescription,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Checkbox(checked = isSelected, onCheckedChange = { onToggleGarment(garmentItem) })
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.btn_ready)) }
        }
    )
}

@Composable
fun RecommendationCard(
    proposalNumber: Int,
    outfit: RecommendedOutfit,
    isSpeaking: Boolean,
    onUseToday: () -> Unit,
    onToggleSpeak: () -> Unit
) {
    var isExplanationExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.proposal_number_title, proposalNumber),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleSpeak) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) stringResource(R.string.stop_speaking) else stringResource(R.string.speak_outfit),
                            tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { isExplanationExpanded = !isExplanationExpanded }) {
                        Icon(
                            imageVector = if (isExplanationExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExplanationExpanded) stringResource(R.string.show_less) else stringResource(R.string.show_details),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

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
                ) + outfit.customGarments

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
                            is CustomGarmentEntity -> item.imageUrl
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

            // Explicación colapsable
            AnimatedVisibility(visible = isExplanationExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = outfit.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (!isExplanationExpanded) {
                TextButton(
                    onClick = { isExplanationExpanded = true },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.show_explanation_btn),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onUseToday,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DryCleaning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.confirm_use_today))
            }
        }
    }
}

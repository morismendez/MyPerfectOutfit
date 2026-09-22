package com.myperfectoutfit.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myperfectoutfit.ui.enums.AdvisorFeature

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAdvisorScreen() {
    var selectedFeature by remember { mutableStateOf<AdvisorFeature?>(null) }

    if (selectedFeature == null) {
        AdvisorMenu(onFeatureSelected = { selectedFeature = it })
    } else {
        BackHandler {
            selectedFeature = null
        }
        
        when (selectedFeature) {
            AdvisorFeature.DAILY_OUTFIT -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(AdvisorFeature.DAILY_OUTFIT.title) },
                            navigationIcon = {
                                IconButton(onClick = { selectedFeature = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        OutfitGeneratorScreen()
                    }
                }
            }
            AdvisorFeature.SUITCASE_IA -> SuitcaseScreen { selectedFeature = null }
            AdvisorFeature.FUTURE_PLANNER -> PlanningScreen { selectedFeature = null }
            AdvisorFeature.CONSULTATIONS -> StyleChatScreen { selectedFeature = null }
            else -> {}
        }
    }
}

@Composable
fun AdvisorMenu(onFeatureSelected: (AdvisorFeature) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Asesor de Moda IA",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(AdvisorFeature.entries) { feature ->
                FeatureCard(feature = feature, onClick = { onFeatureSelected(feature) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCard(feature: AdvisorFeature, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 3
            )
        }
    }
}


package com.myperfectoutfit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.myperfectoutfit.R
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myperfectoutfit.data.local.dao.PlannedOutfitWithDetails
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.local.enums.LaundryState
import com.myperfectoutfit.ui.viewmodel.PlanningViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlanningScreen(
    viewModel: PlanningViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val locale = Locale.getDefault()
    val pattern = if (locale.language == "es") "EEEE, d 'de' MMMM" else "EEEE, MMMM d"
    val sdfDisplay = SimpleDateFormat(pattern, locale)
    val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var showAddDialog by remember { mutableStateOf(false) }
    var showAiPlanningDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.proposedOutfits) {
        if (uiState.proposedOutfits.isNotEmpty()) {
            showReviewDialog = true
        }
    }

    if (showReviewDialog) {
        AiReviewDialog(
            proposedOutfits = uiState.proposedOutfits,
            onDismiss = { 
                viewModel.discardProposedOutfits()
                showReviewDialog = false 
            },
            onConfirm = { selectedList ->
                viewModel.confirmSelectedOutfits(selectedList)
                showReviewDialog = false
            }
        )
    }

    if (showAiPlanningDialog) {
        val dateRangePickerState = rememberDateRangePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Solo permitir fechas de hoy en adelante (considerando zona horaria)
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    return utcTimeMillis >= calendar.timeInMillis
                }
            }
        )
        
        BasicAlertDialog(
            onDismissRequest = { showAiPlanningDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.plan_ai_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.weight(1f),
                        title = { Text(stringResource(R.string.select_period_title)) },
                        headline = { Text(stringResource(R.string.when_use_outfits_prompt)) },
                        showModeToggle = false
                    )
                    
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showAiPlanningDialog = false }) {
                                Text(stringResource(R.string.btn_cancel))
                            }
                            Button(
                                onClick = {
                                    val start = dateRangePickerState.selectedStartDateMillis
                                    val end = dateRangePickerState.selectedEndDateMillis
                                    if (start != null && end != null) {
                                        viewModel.generateAiPlanningForRange(start, end)
                                        showAiPlanningDialog = false
                                    }
                                },
                                enabled = dateRangePickerState.selectedEndDateMillis != null
                            ) {
                                Text(stringResource(R.string.btn_generate))
                            }
                        }
                    }
            }
        }
    }

    if (showAddDialog) {
        val todayStr = sdfDb.format(Date())
        val selectedDateStr = sdfDb.format(uiState.selectedDate.time)
        val isToday = todayStr == selectedDateStr

        PlanningGarmentSelectionDialog(
            availableGarments = uiState.availableGarments,
            isToday = isToday,
            onDismiss = { showAddDialog = false },
            onSave = { garments ->
                // Mapeo simple por ahora (uno de cada categoría)
                val shirt = garments.filterIsInstance<ShirtEntity>().firstOrNull()
                val pant = garments.filterIsInstance<PantEntity>().firstOrNull()
                val shoe = garments.filterIsInstance<ShoeEntity>().firstOrNull()
                val jacket = garments.filterIsInstance<JacketEntity>().firstOrNull()
                val tie = garments.filterIsInstance<TieEntity>().firstOrNull()
                val watch = garments.filterIsInstance<WatchEntity>().firstOrNull()
                val bag = garments.filterIsInstance<BagEntity>().firstOrNull()
                val dress = garments.filterIsInstance<DressEntity>().firstOrNull()
                val skirt = garments.filterIsInstance<SkirtEntity>().firstOrNull()
                val customs = garments.filterIsInstance<CustomGarmentEntity>()

                viewModel.savePlannedOutfit(
                    date = sdfDb.format(uiState.selectedDate.time),
                    shirt = shirt,
                    pant = pant,
                    shoe = shoe,
                    jacket = jacket,
                    tie = tie,
                    watch = watch,
                    bag = bag,
                    dress = dress,
                    skirt = skirt,
                    customGarments = customs
                )
                showAddDialog = false
            }
        )
    }

    val isPastDate = remember(uiState.selectedDate) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        uiState.selectedDate.before(today)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.planner_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    if (!isPastDate) {
                        IconButton(onClick = { showAiPlanningDialog = true }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.plan_ai_title), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isPastDate) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.plan_future_outfit))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (uiState.isTripModeActive) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.active_trip_mode, uiState.activeTripName ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Instrucción IA
            OutlinedTextField(
                value = uiState.userInstruction,
                onValueChange = { viewModel.onInstructionChanged(it) },
                label = { Text(stringResource(R.string.ai_instruction_label)) },
                placeholder = { Text(stringResource(R.string.ai_instruction_placeholder)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                trailingIcon = {
                    if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.moveDate(-1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.btn_previous))
                }
                Text(
                    text = sdfDisplay.format(uiState.selectedDate.time),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { viewModel.moveDate(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.btn_next))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.plannedOutfits.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(stringResource(R.string.no_planned_outfits), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                } else {
                    items(uiState.plannedOutfits) { outfit ->
                        val todayStr = sdfDb.format(Date())
                        val isToday = outfit.plannedOutfit.dateString == todayStr

                        PlanningCard(
                            outfit = outfit,
                            conflicts = viewModel.getConflicts(outfit),
                            isToday = isToday,
                            onDelete = { viewModel.deletePlannedOutfit(outfit.plannedOutfit) },
                            onConfirmUse = { viewModel.confirmPlannedOutfit(outfit) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanningCard(
    outfit: PlannedOutfitWithDetails,
    conflicts: List<String>,
    isToday: Boolean,
    onDelete: () -> Unit,
    onConfirmUse: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = outfit.plannedOutfit.dateString, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (outfit.plannedOutfit.notes != null) {
                Text(text = outfit.plannedOutfit.notes, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Lista Detallada de Prendas (mantenemos las miniaturas grandes de 56dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOfNotNull(
                    outfit.shirt, outfit.pant, outfit.shoe, 
                    outfit.jacket, outfit.dress, outfit.skirt,
                    outfit.tie, outfit.bag, outfit.watch, outfit.fragrance
                ).forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GarmentThumbnail(imageUrl = getGarmentImageUrl(item), size = 56.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = getGarmentIdentifyingDescription(item),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (conflicts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        conflicts.forEach { conflict ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = conflict, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            if (isToday) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onConfirmUse,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.confirm_use_today))
                }
            }
        }
    }
}

@Composable
fun GarmentThumbnail(imageUrl: String, size: androidx.compose.ui.unit.Dp = 40.dp) {
    Card(
        modifier = Modifier.size(size),
        shape = MaterialTheme.shapes.small
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

private fun getGarmentImageUrl(item: Any?): String {
    return when(item) {
        is ShirtEntity -> item.imageUrl
        is PantEntity -> item.imageUrl
        is ShoeEntity -> item.imageUrl
        is JacketEntity -> item.imageUrl
        is DressEntity -> item.imageUrl
        is SkirtEntity -> item.imageUrl
        is WatchEntity -> item.imageUrl
        is FragranceEntity -> item.imageUrl
        is BagEntity -> item.imageUrl
        is TieEntity -> item.imageUrl
        is CustomGarmentEntity -> item.imageUrl
        else -> ""
    }
}

@Composable
private fun getGarmentIdentifyingDescription(item: Any?): String {
    return when(item) {
        is ShirtEntity -> "${item.subType} ${item.primaryColor} - ${item.brand}"
        is PantEntity -> "${item.subType} ${item.primaryColor} - ${item.brand ?: "S/M"}"
        is ShoeEntity -> "${item.subType} ${item.color} - ${item.brand}"
        is JacketEntity -> "${item.type} ${item.color} - ${item.brand ?: "S/M"}"
        is DressEntity -> "${stringResource(R.string.cat_dresses)} ${item.color} - ${item.brand ?: "S/M"}"
        is SkirtEntity -> "${stringResource(R.string.cat_skirts)} ${item.color} - ${item.brand ?: "S/M"}"
        is WatchEntity -> "${stringResource(R.string.cat_watches)} ${item.brand} ${item.model}"
        is FragranceEntity -> "${stringResource(R.string.cat_fragrances)} ${item.brand} ${item.name}"
        is BagEntity -> "${stringResource(R.string.cat_bags)} ${item.color} - ${item.brand ?: "S/M"}"
        is TieEntity -> "${stringResource(R.string.cat_ties)} ${item.colorRange} ${item.pattern}"
        is CustomGarmentEntity -> stringResource(R.string.custom_garment_label)
        else -> stringResource(R.string.garment_label)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiReviewDialog(
    proposedOutfits: List<PlannedOutfitWithDetails>,
    onDismiss: () -> Unit,
    onConfirm: (List<PlannedOutfitWithDetails>) -> Unit
) {
    val selectedItems = remember { mutableStateListOf<PlannedOutfitWithDetails>().apply { addAll(proposedOutfits) } }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.review_proposal_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(R.string.review_proposal_sub),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(proposedOutfits) { outfit ->
                        val isSelected = selectedItems.contains(outfit)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (isSelected) selectedItems.remove(outfit) else selectedItems.add(outfit)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = if (isSelected) 
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { 
                                        if (it) selectedItems.add(outfit) else selectedItems.remove(outfit)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = outfit.plannedOutfit.dateString,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = outfit.plannedOutfit.notes ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 3
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Lista Detallada de Prendas
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOfNotNull(
                                            outfit.shirt, outfit.pant, outfit.shoe, 
                                            outfit.jacket, outfit.dress, outfit.skirt,
                                            outfit.tie, outfit.bag, outfit.watch, outfit.fragrance
                                        ).forEach { item ->
                                            val url = getGarmentImageUrl(item)
                                            val desc = getGarmentIdentifyingDescription(item)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                GarmentThumbnail(imageUrl = url, size = 56.dp)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = desc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.selected_count, selectedItems.size),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Button(
                        onClick = { onConfirm(selectedItems.toList()) },
                        enabled = selectedItems.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.save_selected_btn))
                    }
                }
            }
        }
    }
}

@Composable
fun PlanningGarmentSelectionDialog(
    availableGarments: Map<String, List<Any>>,
    isToday: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<Any>) -> Unit
) {
    val selectedGarments = remember { mutableStateListOf<Any>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isToday) stringResource(R.string.select_today_outfit) else stringResource(R.string.plan_future_outfit)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableGarments.forEach { (category, garments) ->
                    if (garments.isNotEmpty()) {
                        item {
                            Text(
                                text = if (category == "CUSTOM" || category == "PERSONALIZADO") category else stringResource(com.myperfectoutfit.ui.state.getCategoryStringRes(category)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(garments) { garment ->
                            val isSelected = selectedGarments.contains(garment)
                            
                            val laundryState = when(garment) {
                                is ShirtEntity -> garment.laundryState
                                is PantEntity -> garment.laundryState
                                is ShoeEntity -> if (garment.isAvailable) LaundryState.CLEAN else LaundryState.IN_LAUNDRY
                                is JacketEntity -> garment.laundryState
                                is DressEntity -> garment.laundryState
                                is SkirtEntity -> garment.laundryState
                                is CustomGarmentEntity -> garment.laundryState
                                else -> LaundryState.CLEAN
                            }

                            val isLocked = isToday && laundryState == LaundryState.IN_LAUNDRY
                            
                            val imageUrl = when(garment) {
                                is ShirtEntity -> garment.imageUrl
                                is PantEntity -> garment.imageUrl
                                is ShoeEntity -> garment.imageUrl
                                is JacketEntity -> garment.imageUrl
                                is DressEntity -> garment.imageUrl
                                is SkirtEntity -> garment.imageUrl
                                is WatchEntity -> garment.imageUrl
                                is FragranceEntity -> garment.imageUrl
                                is BagEntity -> garment.imageUrl
                                is TieEntity -> garment.imageUrl
                                is CustomGarmentEntity -> garment.imageUrl
                                else -> ""
                            }
                            val name = when(garment) {
                                is ShirtEntity -> garment.brand
                                is PantEntity -> garment.brand ?: "Pantalón"
                                is ShoeEntity -> garment.brand
                                is JacketEntity -> garment.brand ?: "Chaqueta"
                                is DressEntity -> garment.brand ?: "Vestido"
                                is SkirtEntity -> garment.brand ?: "Falda"
                                else -> "Prenda"
                            }
                            
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        text = name,
                                        color = if (isLocked) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else Color.Unspecified
                                    ) 
                                },
                                supportingContent = {
                                    if (laundryState == LaundryState.IN_LAUNDRY) {
                                        Text("En lavandería", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                leadingContent = { GarmentThumbnail(imageUrl) },
                                trailingContent = {
                                    if (!isLocked) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { 
                                                if (it) selectedGarments.add(garment) else selectedGarments.remove(garment)
                                            }
                                        )
                                    } else {
                                        Icon(Icons.Default.Lock, contentDescription = "Bloqueado", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                },
                                modifier = Modifier.clickable(enabled = !isLocked) { 
                                    if (!isSelected) selectedGarments.add(garment) else selectedGarments.remove(garment)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedGarments.toList()) }) {
                Text(if (isToday) "Usar Outfit" else "Guardar Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

package com.myperfectoutfit.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.myperfectoutfit.R
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.myperfectoutfit.ui.enums.SpecialEvent
import com.myperfectoutfit.ui.enums.SuitcaseType
import com.myperfectoutfit.ui.enums.TripStyle
import com.myperfectoutfit.ui.viewmodel.SuitcaseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuitcaseScreen(
    viewModel: SuitcaseViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableIntStateOf(1) }
    
    var showReplaceSheet by remember { mutableStateOf(false) }
    var garmentToReplace by remember { mutableStateOf<com.myperfectoutfit.ui.viewmodel.SuitcaseItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar(context.getString(R.string.suitcase_saved_success))
            viewModel.clearForm()
            currentStep = 1
            viewModel.resetSaveStatus()
        }
    }

    if (showReplaceSheet && garmentToReplace != null) {
        ReplaceGarmentSheet(
            garment = garmentToReplace!!,
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showReplaceSheet = false },
            onSelect = { newGarment ->
                viewModel.replaceGarment(garmentToReplace!!.id, newGarment)
                showReplaceSheet = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.suitcase_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (currentStep > 1) currentStep-- else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Indicador de pasos
            LinearProgressIndicator(
                progress = { currentStep / 3f },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "StepTransition"
            ) { step ->
                when (step) {
                    1 -> Step1BasicParams(uiState, viewModel) { currentStep = it }
                    2 -> Step2ContextAndStyle(uiState, viewModel) { currentStep++ }
                    3 -> Step3Generation(uiState, viewModel) { garment ->
                        garmentToReplace = garment
                        showReplaceSheet = true
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step1BasicParams(
    state: com.myperfectoutfit.ui.viewmodel.SuitcaseUiState,
    viewModel: SuitcaseViewModel,
    onStepChange: (Int) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var suitcaseToDelete by remember { mutableStateOf<com.myperfectoutfit.data.local.entities.SuitcaseEntity?>(null) }

    if (suitcaseToDelete != null) {
        AlertDialog(
            onDismissRequest = { suitcaseToDelete = null },
            title = { Text(stringResource(R.string.delete_plan_title)) },
            text = { Text(stringResource(R.string.delete_plan_confirm, suitcaseToDelete!!.tripName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSuitcase(suitcaseToDelete!!)
                        suitcaseToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { suitcaseToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    val dateRangePickerState = rememberDateRangePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return utcTimeMillis >= calendar.timeInMillis
            }
        }
    )

    if (showDatePicker) {
        DateRangePickerModal(
            state = dateRangePickerState,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                viewModel.updateTripDetails(
                    state.tripName,
                    dateRangePickerState.selectedStartDateMillis,
                    dateRangePickerState.selectedEndDateMillis
                )
                showDatePicker = false
            }
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.suitcases.isNotEmpty()) {
            item {
                Text(stringResource(R.string.saved_plans_title), style = MaterialTheme.typography.titleMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.suitcases) { suitcase ->
                        OutlinedCard(
                            onClick = { 
                                viewModel.loadSavedSuitcase(suitcase)
                                onStepChange(if (!suitcase.aiSuggestions.isNullOrBlank()) 3 else 2)
                            },
                            modifier = Modifier.width(180.dp)
                        ) {
                            Box {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Text(
                                        text = suitcase.tripName,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = suitcase.startDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = suitcase.suitcaseType,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                IconButton(
                                    onClick = { suitcaseToDelete = suitcase },
                                    modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close, 
                                        contentDescription = stringResource(R.string.btn_delete),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }

        item {
            Text(stringResource(R.string.step1_title), style = MaterialTheme.typography.titleLarge)
        }
        item {
            OutlinedTextField(
                value = state.tripName,
                onValueChange = { viewModel.updateTripDetails(it, state.startDate, state.endDate) },
                label = { Text(stringResource(R.string.trip_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.trip_name_placeholder)) }
            )
        }
        item {
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (state.dateOverlapWarning != null) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                ),
                border = if (state.dateOverlapWarning != null) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange, 
                        contentDescription = null,
                        tint = if (state.dateOverlapWarning != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val dateText = if (state.startDate != null && state.endDate != null) {
                        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        "${sdf.format(Date(state.startDate))} - ${sdf.format(Date(state.endDate))}"
                    } else stringResource(R.string.select_dates)
                    Text(
                        dateText,
                        color = if (state.dateOverlapWarning != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            state.dateOverlapWarning?.let { warning ->
                Text(
                    text = warning,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
        item {
            Text(stringResource(R.string.luggage_type_label), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuitcaseType.entries.forEach { type ->
                    FilterChip(
                        selected = state.suitcaseType == type,
                        onClick = { viewModel.updateSuitcaseType(type) },
                        label = { Text(stringResource(type.titleRes)) },
                        leadingIcon = { Icon(type.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            Button(
                onClick = { onStepChange(2) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                enabled = state.tripName.isNotBlank() && state.startDate != null
            ) {
                Text(stringResource(R.string.btn_next))
            }
        }
    }
}

@Composable
fun Step2ContextAndStyle(
    state: com.myperfectoutfit.ui.viewmodel.SuitcaseUiState,
    viewModel: SuitcaseViewModel,
    onNext: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(stringResource(R.string.step2_title), style = MaterialTheme.typography.titleLarge)
        }
        item {
            Text(stringResource(R.string.trip_style_prompt), style = MaterialTheme.typography.titleMedium)
        }
        
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TripStyle.entries.chunked(2).forEach { rowStyles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowStyles.forEach { style ->
                            val isSelected = state.selectedStyles.contains(style)
                            OutlinedCard(
                                onClick = { viewModel.toggleTripStyle(style) },
                                modifier = Modifier.weight(1f).height(100.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = style.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(style.titleRes),
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(stringResource(R.string.special_event_prompt), style = MaterialTheme.typography.titleMedium)
        }

        items(SpecialEvent.entries) { event ->
            val isSelected = state.selectedEvents.contains(event)
            Column {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleSpecialEvent(event) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { viewModel.toggleSpecialEvent(event) }
                        )
                        Text(text = stringResource(event.titleRes), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                
                if (event == SpecialEvent.OTHER && isSelected) {
                    OutlinedTextField(
                        value = state.customEvent,
                        onValueChange = { viewModel.updateCustomEvent(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, end = 16.dp, bottom = 8.dp),
                        label = { Text(stringResource(R.string.what_event_label)) },
                        placeholder = { Text(stringResource(R.string.event_placeholder)) },
                        singleLine = true
                    )
                }
            }
        }

        item {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Text(stringResource(R.string.btn_next))
            }
        }
    }
}

@Composable
fun Step3Generation(
    state: com.myperfectoutfit.ui.viewmodel.SuitcaseUiState,
    viewModel: SuitcaseViewModel,
    onChangeGarment: (com.myperfectoutfit.ui.viewmodel.SuitcaseItem) -> Unit
) {
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.packing_suitcase_loading))
            }
        }
    } else if (state.aiJustification != null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = if (state.isCompleted) stringResource(R.string.completed_trip_summary) else stringResource(R.string.suitcase_ready_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(state.aiJustification, style = MaterialTheme.typography.bodyMedium)
            }

            if (!state.isCompleted) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.from_your_wardrobe),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        TextButton(
                            onClick = { viewModel.togglePackingMode() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (state.isPackingMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                if (state.isPackingMode) Icons.Default.Checklist else Icons.Default.Inventory,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.isPackingMode) stringResource(R.string.edit_mode) else stringResource(R.string.pack_suitcase))
                        }
                    }
                }
            } else {
                item {
                    Text(
                        stringResource(R.string.used_garments),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            items(state.selectedGarments) { garment ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    border = when {
                        garment.isPacked -> BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
                        garment.isDirty -> BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (garment.isPacked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .clickable(enabled = state.isPackingMode && !state.isCompleted) { 
                                viewModel.toggleItemPacked(garment.id) 
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isPackingMode && !state.isCompleted) {
                            Checkbox(
                                checked = garment.isPacked,
                                onCheckedChange = { viewModel.toggleItemPacked(garment.id) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (garment.imageUrl != null) {
                                AsyncImage(
                                    model = garment.imageUrl,
                                    contentDescription = garment.name,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (garment.isDirty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(garment.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = garment.categoryDisplay,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (garment.isDirty) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocalLaundryService,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            stringResource(R.string.in_laundry),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            if (!state.isPackingMode && !state.isCompleted) {
                                IconButton(onClick = { onChangeGarment(garment) }) {
                                    Icon(
                                        Icons.Default.SwapHoriz,
                                        contentDescription = stringResource(R.string.change_btn),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else if (garment.isPacked || state.isCompleted) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (state.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

            if (state.missingSuggestions.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.suggest_buying), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                }
                items(state.missingSuggestions) { suggestion ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = state.isPackingMode && !state.isCompleted) { viewModel.toggleSuggestionChecked(suggestion.name) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (suggestion.isChecked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        ),
                        border = if (suggestion.isChecked && !state.isCompleted) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (state.isPackingMode && !state.isCompleted) {
                                Checkbox(
                                    checked = suggestion.isChecked,
                                    onCheckedChange = { viewModel.toggleSuggestionChecked(suggestion.name) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(
                                if (suggestion.isChecked || state.isCompleted) Icons.Default.CheckCircle else Icons.Default.AddShoppingCart, 
                                contentDescription = null, 
                                tint = if (suggestion.isChecked || state.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                suggestion.name, 
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (suggestion.isChecked || state.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (state.essentials.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.dont_forget_essentials), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.essentials.forEach { essential ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = state.isPackingMode && !state.isCompleted) { viewModel.toggleEssentialChecked(essential.name) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    if (state.isPackingMode && !state.isCompleted) {
                                        Checkbox(
                                            checked = essential.isChecked,
                                            onCheckedChange = { viewModel.toggleEssentialChecked(essential.name) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Icon(
                                        if (essential.isChecked || state.isCompleted) Icons.Default.CheckCircle else Icons.Default.TaskAlt, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(18.dp), 
                                        tint = if (essential.isChecked || state.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        essential.name, 
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (essential.isChecked || state.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            if (!state.isCompleted) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.generateSuitcase() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.regenerate_btn))
                        }
                        OutlinedButton(
                            onClick = { viewModel.saveSuitcase() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_save))
                        }
                    }
                }
            } else {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.trip_concluded_msg), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    text = stringResource(R.string.all_set_prompt),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.review_details_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SummaryRow(
                            icon = Icons.Default.Place,
                            label = stringResource(R.string.destination_label),
                            value = state.tripName
                        )

                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val dateRange = if (state.startDate != null && state.endDate != null) {
                            "${sdf.format(Date(state.startDate))} - ${sdf.format(Date(state.endDate))}"
                        } else stringResource(R.string.dates_not_set)
                        
                        SummaryRow(
                            icon = Icons.Default.DateRange,
                            label = stringResource(R.string.period_label),
                            value = dateRange
                        )
                        
                        state.dateOverlapWarning?.let { warning ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = warning,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        SummaryRow(
                            icon = state.suitcaseType.icon,
                            label = stringResource(R.string.luggage_type_label),
                            value = stringResource(state.suitcaseType.titleRes)
                        )

                        SummaryRow(
                            icon = Icons.Default.Style,
                            label = stringResource(R.string.selected_styles_label),
                            value = state.selectedStyles.map { stringResource(it.titleRes) }.joinToString()
                        )

                        val events = state.selectedEvents
                            .filter { it != SpecialEvent.OTHER }
                            .map { stringResource(it.titleRes) }
                            .toMutableList()
                        
                        if (state.selectedEvents.contains(SpecialEvent.OTHER) && state.customEvent.isNotBlank()) {
                            events.add(state.customEvent)
                        }

                        if (events.isNotEmpty()) {
                            SummaryRow(
                                icon = Icons.Default.Celebration,
                                label = stringResource(R.string.special_events_label),
                                value = events.joinToString(", ")
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.generateSuitcase() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.generate_suitcase_btn))
                    }
                    
                    OutlinedButton(
                        onClick = { viewModel.saveSuitcase() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.save_trip_plan_btn))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplaceGarmentSheet(
    garment: com.myperfectoutfit.ui.viewmodel.SuitcaseItem,
    uiState: com.myperfectoutfit.ui.viewmodel.SuitcaseUiState,
    viewModel: SuitcaseViewModel,
    onDismiss: () -> Unit,
    onSelect: (com.myperfectoutfit.ui.viewmodel.SuitcaseItem) -> Unit
) {
    val alternatives = remember(garment.category, garment.id, uiState.fullInventory, uiState.selectedGarments) { 
        viewModel.getAlternativesFor(garment.category, garment.id) 
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.change_garment_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            // Botón de IA
            if (uiState.pendingAiSuggestion == null) {
                Button(
                    onClick = { viewModel.requestAiReplacement(garment) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    enabled = !uiState.isReplacingAi && alternatives.isNotEmpty()
                ) {
                    if (uiState.isReplacingAi) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.ai_choose_for_me))
                    }
                }
            } else {
                // Propuesta de cambio de IA
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.ai_proposal_title), style = MaterialTheme.typography.titleMedium)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Prenda Antigua
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (garment.imageUrl != null) {
                                    AsyncImage(
                                        model = garment.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Text(stringResource(R.string.current_label), style = MaterialTheme.typography.labelSmall)
                            }
                            
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(horizontal = 8.dp))
                            
                            // Prenda Nueva
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (uiState.pendingAiSuggestion.imageUrl != null) {
                                    AsyncImage(
                                        model = uiState.pendingAiSuggestion.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Text(uiState.pendingAiSuggestion.name, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, maxLines = 1)
                                Text(stringResource(R.string.proposal_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        
                        uiState.aiSuggestionReason?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.clearAiSuggestion() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.discard_btn))
                            }
                            Button(
                                onClick = { 
                                    viewModel.confirmAiReplacement(garment.id)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.btn_accept))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            if (alternatives.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_other_garments_in_category))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(alternatives) { item ->
                        val isCurrent = item.id == garment.id
                        OutlinedCard(
                            onClick = { if (!isCurrent) onSelect(item) },
                            modifier = Modifier.fillMaxWidth(),
                            border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.imageUrl != null) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                    if (item.isDirty) {
                                        Text(stringResource(R.string.in_laundry), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (isCurrent) {
                                    Text(stringResource(R.string.selected_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerModal(
    state: DateRangePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = state.selectedEndDateMillis != null
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DateRangePicker(
            state = state,
            title = { 
                Text(
                    text = "Selecciona el periodo del viaje",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.labelLarge
                ) 
            },
            headline = { 
                val startDate = state.selectedStartDateMillis
                val endDate = state.selectedEndDateMillis
                Box(modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)) {
                    if (startDate != null && endDate != null) {
                        val sdf = SimpleDateFormat("d 'de' MMM", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        Text(
                            text = "${sdf.format(Date(startDate))} - ${sdf.format(Date(endDate))}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    } else {
                        Text(
                            text = "Fecha inicio - fin",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            },
            showModeToggle = false,
            modifier = Modifier.fillMaxWidth().height(450.dp)
        )
    }
}

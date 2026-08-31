package com.myperfectoutfit.ui.screens

import android.net.Uri
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import org.json.JSONObject
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.myperfectoutfit.ui.viewmodel.RecommendedOutfit
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.local.enums.LaundryState
import com.myperfectoutfit.ui.state.CategoryFilter
import com.myperfectoutfit.ui.viewmodel.WardrobeViewModel
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.palette.graphics.Palette
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeScreen(
    viewModel: WardrobeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<Any?>(null) }
    var itemToEdit by remember { mutableStateOf<Any?>(null) }
    var zoomImageUri by remember { mutableStateOf<String?>(null) }

    if (showAddCategoryDialog || categoryToEdit != null) {
        AddCategoryDialog(
            initialCategory = categoryToEdit,
            onDismiss = { 
                showAddCategoryDialog = false 
                categoryToEdit = null
            },
            onSave = { name, attrs, needsLaundry ->
                if (categoryToEdit != null) {
                    viewModel.updateCustomCategory(categoryToEdit!!.copy(
                        name = name, 
                        attributeNames = attrs,
                        needsLaundry = needsLaundry
                    ))
                } else {
                    viewModel.insertCustomCategory(name, attrs, needsLaundry)
                }
                showAddCategoryDialog = false
                categoryToEdit = null
            }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar Categoría?") },
            text = { Text("Esta acción eliminará la categoría '${categoryToDelete?.name}' y TODAS las prendas contenidas en ella de forma permanente.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        categoryToDelete?.let { viewModel.deleteCustomCategory(it) }
                        categoryToDelete = null
                    }
                ) {
                    Text("Eliminar Todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    AnimatedVisibility(visible = false) {
        // Marcador para futura limpieza de imagen
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            val activeCategories = uiState.user?.activeCategories?.split(",")?.mapNotNull { name ->
                try { CategoryFilter.valueOf(name) } catch(e: Exception) { null }
            } ?: CategoryFilter.entries.filter { it != CategoryFilter.ALL && it != CategoryFilter.LAUNDRY }

            CategoryFilterBar(
                selectedCategory = uiState.selectedCategory,
                selectedCustomCategoryId = uiState.selectedCustomCategoryId,
                activeCategories = activeCategories,
                customCategories = uiState.customCategories,
                onCategorySelected = { viewModel.selectCategory(it) },
                onCustomCategorySelected = { viewModel.selectCustomCategory(it) },
                onAddCategoryRequested = { showAddCategoryDialog = true },
                onEditCustomCategory = { categoryToEdit = it },
                onDeleteCustomCategory = { categoryToDelete = it }
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (uiState.selectedCategory) {
                    CategoryFilter.SHIRTS -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.shirts) { shirt ->
                                PrendaItemCard(
                                    title = shirt.brand,
                                    subtitle = shirt.pattern,
                                    imageUri = shirt.imageUrl,
                                    laundryState = shirt.laundryState,
                                    onDeleteRequested = { itemToDelete = shirt },
                                    onEditRequested = { itemToEdit = shirt },
                                    onImageClick = { zoomImageUri = shirt.imageUrl }
                                )
                            }
                        }
                    }

                    CategoryFilter.PANTS -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.pants) { pant ->
                                PrendaItemCard(
                                    title = pant.brand ?: "Sin Marca",
                                    subtitle = "${pant.subType} - ${pant.material}",
                                    imageUri = pant.imageUrl,
                                    laundryState = pant.laundryState,
                                    onDeleteRequested = { itemToDelete = pant },
                                    onEditRequested = { itemToEdit = pant },
                                    onImageClick = { zoomImageUri = pant.imageUrl }
                                )
                            }
                        }
                    }

                    CategoryFilter.SHOES -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.shoes) { shoe ->
                                PrendaItemCard(
                                    title = shoe.brand,
                                    subtitle = "${shoe.style} - ${shoe.color}",
                                    imageUri = shoe.imageUrl,
                                    laundryState = if (shoe.isAvailable) LaundryState.CLEAN else LaundryState.IN_LAUNDRY,
                                    onDeleteRequested = { itemToDelete = shoe },
                                    onEditRequested = { itemToEdit = shoe },
                                    onImageClick = { zoomImageUri = shoe.imageUrl }
                                )
                            }
                        }
                    }

                    CategoryFilter.TIES -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.ties) { tie ->
                                PrendaItemCard(
                                    title = tie.colorRange,
                                    subtitle = tie.pattern,
                                    imageUri = tie.imageUrl,
                                    laundryState = tie.laundryState,
                                    onDeleteRequested = { itemToDelete = tie },
                                    onEditRequested = { itemToEdit = tie },
                                    onImageClick = { zoomImageUri = tie.imageUrl }
                                )
                            }
                        }
                    }

                    CategoryFilter.WATCHES -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.watches) { watch ->
                                PrendaItemCard(
                                    title = watch.brand,
                                    subtitle = watch.model,
                                    imageUri = watch.imageUrl,
                                    laundryState = if (watch.isAvailable) LaundryState.CLEAN else LaundryState.IN_LAUNDRY,
                                    onDeleteRequested = { itemToDelete = watch },
                                    onEditRequested = { itemToEdit = watch },
                                    onImageClick = { zoomImageUri = watch.imageUrl }
                                )
                            }
                        }
                    }

                    CategoryFilter.FRAGRANCES -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.fragrances) { fragrance ->
                                PrendaItemCard(
                                    title = fragrance.brand,
                                    subtitle = fragrance.name,
                                    imageUri = fragrance.imageUrl,
                                    laundryState = LaundryState.CLEAN,
                                    onDeleteRequested = { itemToDelete = fragrance },
                                    onEditRequested = { itemToEdit = fragrance },
                                    onImageClick = { zoomImageUri = fragrance.imageUrl }
                                )
                            }
                        }
                    }

                    CategoryFilter.JACKETS -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.jackets) { jacket ->
                                PrendaItemCard(
                                    title = jacket.brand ?: "Sin Marca",
                                    subtitle = jacket.color,
                                    imageUri = jacket.imageUrl,
                                    laundryState = jacket.laundryState,
                                    onDeleteRequested = { itemToDelete = jacket },
                                    onEditRequested = { itemToEdit = jacket },
                                    onImageClick = { zoomImageUri = jacket.imageUrl }
                                )
                            }
                        }
                    }

                    CategoryFilter.BAGS -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.bags) { bag ->
                                PrendaItemCard(
                                    title = bag.brand ?: "Sin Marca",
                                    subtitle = "${bag.style} - ${bag.color}",
                                    imageUri = bag.imageUrl,
                                    laundryState = if (bag.isAvailable) LaundryState.CLEAN else LaundryState.IN_LAUNDRY,
                                    onDeleteRequested = { itemToDelete = bag },
                                    onEditRequested = { itemToEdit = bag },
                                    onImageClick = { zoomImageUri = bag.imageUrl }
                                )
                            }
                        }
                    }

                    CategoryFilter.DRESSES -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.dresses) { dress ->
                                PrendaItemCard(
                                    title = dress.brand ?: "Sin Marca",
                                    subtitle = "${dress.color} - ${dress.pattern}",
                                    imageUri = dress.imageUrl,
                                    laundryState = dress.laundryState,
                                    onDeleteRequested = { itemToDelete = dress },
                                    onEditRequested = { itemToEdit = dress },
                                    onImageClick = { zoomImageUri = dress.imageUrl }
                                )
                            }
                        }
                    }

                    CategoryFilter.SKIRTS -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.skirts) { skirt ->
                                PrendaItemCard(
                                    title = skirt.brand ?: "Sin Marca",
                                    subtitle = "${skirt.style} - ${skirt.color}",
                                    imageUri = skirt.imageUrl,
                                    laundryState = skirt.laundryState,
                                    onDeleteRequested = { itemToDelete = skirt },
                                    onEditRequested = { itemToEdit = skirt },
                                    onImageClick = { zoomImageUri = skirt.imageUrl }
                                )
                            }
                        }
                    }

                    else -> {
                        if (uiState.selectedCustomCategoryId != null) {
                            val customGarments = uiState.customGarments.filter { it.categoryId == uiState.selectedCustomCategoryId }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(customGarments) { garment ->
                                    PrendaItemCard(
                                        title = garment.attributeValues.split("|").firstOrNull()?.split(":")?.getOrNull(1) ?: "Prenda",
                                        subtitle = garment.attributeValues.replace("|", " - "),
                                        imageUri = garment.imageUrl,
                                        laundryState = garment.laundryState,
                                        onDeleteRequested = { itemToDelete = garment },
                                        onEditRequested = { itemToEdit = garment },
                                        onImageClick = { zoomImageUri = garment.imageUrl }
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (activeCategories.contains(CategoryFilter.SHIRTS)) {
                                    items(uiState.shirts) { shirt ->
                                        PrendaItemCard(
                                            title = shirt.brand,
                                            subtitle = shirt.pattern,
                                            imageUri = shirt.imageUrl,
                                            laundryState = shirt.laundryState,
                                            onDeleteRequested = { itemToDelete = shirt },
                                            onEditRequested = { itemToEdit = shirt },
                                            onImageClick = { zoomImageUri = shirt.imageUrl }
                                        )
                                    }
                                }
                                if (activeCategories.contains(CategoryFilter.PANTS)) {
                                    items(uiState.pants) { pant ->
                                        PrendaItemCard(
                                            title = pant.brand ?: "Sin Marca",
                                            subtitle = "${pant.subType} - ${pant.material}",
                                            imageUri = pant.imageUrl,
                                            laundryState = pant.laundryState,
                                            onDeleteRequested = { itemToDelete = pant },
                                            onEditRequested = { itemToEdit = pant },
                                            onImageClick = { zoomImageUri = pant.imageUrl }
                                        )
                                    }
                                }
                                if (activeCategories.contains(CategoryFilter.SHOES)) {
                                    items(uiState.shoes) { shoe ->
                                        PrendaItemCard(
                                            title = shoe.brand,
                                            subtitle = "${shoe.style} - ${shoe.color}",
                                            imageUri = shoe.imageUrl,
                                            laundryState = if (shoe.isAvailable) LaundryState.CLEAN else LaundryState.IN_LAUNDRY,
                                            onDeleteRequested = { itemToDelete = shoe },
                                            onEditRequested = { itemToEdit = shoe },
                                            onImageClick = { zoomImageUri = shoe.imageUrl }
                                        )
                                    }
                                }
                                if (activeCategories.contains(CategoryFilter.TIES)) {
                                    items(uiState.ties) { tie ->
                                        PrendaItemCard(
                                            title = tie.colorRange,
                                            subtitle = tie.pattern,
                                            imageUri = tie.imageUrl,
                                            laundryState = tie.laundryState,
                                            onDeleteRequested = { itemToDelete = tie },
                                            onEditRequested = { itemToEdit = tie },
                                            onImageClick = { zoomImageUri = tie.imageUrl }
                                        )
                                    }
                                }
                                if (activeCategories.contains(CategoryFilter.WATCHES)) {
                                    items(uiState.watches) { watch ->
                                        PrendaItemCard(
                                            title = watch.brand,
                                            subtitle = watch.model,
                                            imageUri = watch.imageUrl,
                                            laundryState = if (watch.isAvailable) LaundryState.CLEAN else LaundryState.IN_LAUNDRY,
                                            onDeleteRequested = { itemToDelete = watch },
                                            onEditRequested = { itemToEdit = watch },
                                            onImageClick = { zoomImageUri = watch.imageUrl }
                                        )
                                    }
                                }
                                if (activeCategories.contains(CategoryFilter.FRAGRANCES)) {
                                    items(uiState.fragrances) { fragrance ->
                                        PrendaItemCard(
                                            title = fragrance.brand,
                                            subtitle = fragrance.name,
                                            imageUri = fragrance.imageUrl,
                                            laundryState = LaundryState.CLEAN,
                                            onDeleteRequested = { itemToDelete = fragrance },
                                            onEditRequested = { itemToEdit = fragrance },
                                            onImageClick = { zoomImageUri = fragrance.imageUrl }
                                        )
                                    }
                                }
                                if (activeCategories.contains(CategoryFilter.JACKETS)) {
                                    items(uiState.jackets) { jacket ->
                                        PrendaItemCard(
                                            title = jacket.brand ?: "Sin Marca",
                                            subtitle = jacket.color,
                                            imageUri = jacket.imageUrl,
                                            laundryState = jacket.laundryState,
                                            onDeleteRequested = { itemToDelete = jacket },
                                            onEditRequested = { itemToEdit = jacket },
                                            onImageClick = { zoomImageUri = jacket.imageUrl }
                                        )
                                    }
                                }
                                if (activeCategories.contains(CategoryFilter.BAGS)) {
                                    items(uiState.bags) { bag ->
                                        PrendaItemCard(
                                            title = bag.brand ?: "Sin Marca",
                                            subtitle = "${bag.style} - ${bag.color}",
                                            imageUri = bag.imageUrl,
                                            laundryState = if (bag.isAvailable) LaundryState.CLEAN else LaundryState.IN_LAUNDRY,
                                            onDeleteRequested = { itemToDelete = bag },
                                            onEditRequested = { itemToEdit = bag },
                                            onImageClick = { zoomImageUri = bag.imageUrl }
                                        )
                                    }
                                }
                                if (activeCategories.contains(CategoryFilter.DRESSES)) {
                                    items(uiState.dresses) { dress ->
                                        PrendaItemCard(
                                            title = dress.brand ?: "Sin Marca",
                                            subtitle = "${dress.color} - ${dress.pattern}",
                                            imageUri = dress.imageUrl,
                                            laundryState = dress.laundryState,
                                            onDeleteRequested = { itemToDelete = dress },
                                            onEditRequested = { itemToEdit = dress },
                                            onImageClick = { zoomImageUri = dress.imageUrl }
                                        )
                                    }
                                }
                                if (activeCategories.contains(CategoryFilter.SKIRTS)) {
                                    items(uiState.skirts) { skirt ->
                                        PrendaItemCard(
                                            title = skirt.brand ?: "Sin Marca",
                                            subtitle = "${skirt.style} - ${skirt.color}",
                                            imageUri = skirt.imageUrl,
                                            laundryState = skirt.laundryState,
                                            onDeleteRequested = { itemToDelete = skirt },
                                            onEditRequested = { itemToEdit = skirt },
                                            onImageClick = { zoomImageUri = skirt.imageUrl }
                                        )
                                    }
                                }
                                
                                // Mostrar también prendas de categorías personalizadas en ALL
                                items(uiState.customGarments) { garment ->
                                    PrendaItemCard(
                                        title = garment.attributeValues.split("|").firstOrNull()?.split(":")?.getOrNull(1) ?: "Prenda",
                                        subtitle = garment.attributeValues.replace("|", " - "),
                                        imageUri = garment.imageUrl,
                                        laundryState = garment.laundryState,
                                        onDeleteRequested = { itemToDelete = garment },
                                        onEditRequested = { itemToEdit = garment },
                                        onImageClick = { zoomImageUri = garment.imageUrl }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.selectedCategory != CategoryFilter.ALL || uiState.selectedCustomCategoryId != null) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Prenda")
            }
        }
    }

    if (zoomImageUri != null) {
        Dialog(onDismissRequest = { zoomImageUri = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = zoomImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { zoomImageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
            }
        }
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("¿Eliminar Prenda?") },
            text = { Text("Esta acción eliminará la prenda permanentemente de tu armario.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        when (item) {
                            is ShirtEntity -> viewModel.deleteShirt(item)
                            is PantEntity -> viewModel.deletePant(item)
                            is ShoeEntity -> viewModel.deleteShoe(item)
                            is TieEntity -> viewModel.deleteTie(item)
                            is WatchEntity -> viewModel.deleteWatch(item)
                            is FragranceEntity -> viewModel.deleteFragrance(item)
                            is JacketEntity -> viewModel.deleteJacket(item)
                            is BagEntity -> viewModel.deleteBag(item)
                            is DressEntity -> viewModel.deleteDress(item)
                            is SkirtEntity -> viewModel.deleteSkirt(item)
                            is CustomGarmentEntity -> viewModel.deleteCustomGarment(item)
                        }
                        itemToDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showAddDialog || itemToEdit != null) {
        val initialData = itemToEdit?.let { item ->
            mutableMapOf<String, Any?>().apply {
                put("imageUrl", when(item) {
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
                })
                when(item) {
                    is ShirtEntity -> {
                        put("brand", item.brand)
                        put("subType", item.subType)
                        put("color", item.primaryColor)
                        put("secondaryColor", item.secondaryColor ?: "")
                        put("pattern", item.pattern)
                        put("sleeveLength", item.sleeveLength)
                        put("necklineStyle", item.necklineStyle)
                        put("material", item.material)
                        put("formalityLevel", item.formalityLevel)
                        put("fit", item.fit)
                    }
                    is PantEntity -> {
                        put("brand", item.brand ?: "")
                        put("subType", item.subType)
                        put("color", item.primaryColor)
                        put("secondaryColor", item.secondaryColor ?: "")
                        put("material", item.material)
                        put("lengthStyle", item.lengthStyle)
                        put("waistRise", item.waistRise)
                        put("fitStyle", item.fitStyle)
                        put("formalityLevel", item.formalityLevel)
                    }
                    is ShoeEntity -> {
                        put("brand", item.brand)
                        put("subType", item.subType)
                        put("style", item.style)
                        put("color", item.color)
                        put("secondaryColor", item.secondaryColor ?: "")
                        put("material", item.material)
                        put("heelHeightStyle", item.heelHeightStyle)
                        put("toeStyle", item.toeStyle)
                        put("closureType", item.closureType)
                        put("formalityLevel", item.formalityLevel)
                    }
                    is TieEntity -> {
                        put("color", item.colorRange)
                        put("pattern", item.pattern)
                        put("material", item.material)
                        put("width", item.widthCms.toString())
                    }
                    is WatchEntity -> {
                        put("brand", item.brand)
                        put("model", item.model)
                        put("dialColor", item.dialColor)
                        put("strapColor", item.strapColor)
                        put("strapMaterial", item.strapMaterial)
                    }
                    is FragranceEntity -> {
                        put("brand", item.brand)
                        put("name", item.name)
                        put("occasion", item.occasionTag)
                        put("profile", item.profile)
                    }
                    is JacketEntity -> {
                        put("brand", item.brand ?: "")
                        put("color", item.color)
                        put("type", item.type)
                        put("closureType", item.closureType)
                    }
                    is BagEntity -> {
                        put("brand", item.brand ?: "")
                        put("color", item.color)
                        put("style", item.style)
                        put("material", item.material)
                        put("size", item.size)
                    }
                    is DressEntity -> {
                        put("brand", item.brand ?: "")
                        put("color", item.color)
                        put("pattern", item.pattern)
                        put("length", item.length)
                        put("sleeveStyle", item.sleeveStyle)
                        put("material", item.material)
                    }
                    is SkirtEntity -> {
                        put("brand", item.brand ?: "")
                        put("color", item.color)
                        put("pattern", item.pattern)
                        put("length", item.length)
                        put("style", item.style)
                        put("material", item.material)
                    }
                    is CustomGarmentEntity -> {
                        put("customCategoryId", item.categoryId)
                        put("customAttributes", item.attributeValues)
                    }
                }
            }
        }

        PrendaDialog(
            category = if (itemToEdit != null) {
                when(itemToEdit) {
                    is ShirtEntity -> CategoryFilter.SHIRTS
                    is PantEntity -> CategoryFilter.PANTS
                    is ShoeEntity -> CategoryFilter.SHOES
                    is TieEntity -> CategoryFilter.TIES
                    is WatchEntity -> CategoryFilter.WATCHES
                    is FragranceEntity -> CategoryFilter.FRAGRANCES
                    is JacketEntity -> CategoryFilter.JACKETS
                    is BagEntity -> CategoryFilter.BAGS
                    is DressEntity -> CategoryFilter.DRESSES
                    is SkirtEntity -> CategoryFilter.SKIRTS
                    is CustomGarmentEntity -> CategoryFilter.ALL 
                    else -> uiState.selectedCategory
                }
            } else uiState.selectedCategory,
            initialCustomCategoryId = if (itemToEdit != null) {
                (itemToEdit as? CustomGarmentEntity)?.categoryId
            } else uiState.selectedCustomCategoryId,
            customCategories = uiState.customCategories,
            initialData = initialData,
            onDismiss = { 
                showAddDialog = false
                itemToEdit = null
            },
            onSave = { data ->
                val uriStr = data["imageUrl"] as? String
                val imageUrl = if (uriStr != null && uriStr.startsWith("content://")) {
                    saveImageToInternalStorage(context, Uri.parse(uriStr)) ?: uriStr
                } else uriStr ?: ""

                val brand = data["brand"] as? String ?: "Sin Marca"
                val color = data["color"] as? String ?: "Desconocido"
                
                if (data["isCustom"] == true) {
                    val catId = data["customCategoryId"] as Long
                    val attrValues = data["customAttributes"] as String
                    if (itemToEdit != null && itemToEdit is CustomGarmentEntity) {
                        viewModel.updateCustomGarment((itemToEdit as CustomGarmentEntity).copy(
                            imageUrl = imageUrl,
                            attributeValues = attrValues
                        ))
                    } else {
                        viewModel.insertCustomGarment(CustomGarmentEntity(
                            categoryId = catId,
                            imageUrl = imageUrl,
                            attributeValues = attrValues
                        ))
                    }
                } else if (itemToEdit != null) {
                    when (val item = itemToEdit) {
                        is ShirtEntity -> viewModel.updateShirt(item.copy(
                            brand = brand, 
                            subType = data["subType"] as? String ?: "Camisa",
                            primaryColor = color, 
                            secondaryColor = data["secondaryColor"] as? String,
                            pattern = data["pattern"] as? String ?: "Liso",
                            sleeveLength = data["sleeveLength"] as? String ?: "Manga larga",
                            necklineStyle = data["necklineStyle"] as? String ?: "Camisero",
                            material = data["material"] as? String ?: "Algodón",
                            formalityLevel = data["formalityLevel"] as? String ?: "Casual",
                            fit = data["fit"] as? String ?: "Regular",
                            imageUrl = imageUrl
                        ))
                        is PantEntity -> viewModel.updatePant(item.copy(
                            brand = brand, 
                            subType = data["subType"] as? String ?: "Pantalón",
                            primaryColor = color, 
                            secondaryColor = data["secondaryColor"] as? String,
                            material = data["material"] as? String ?: "Algodón",
                            lengthStyle = data["lengthStyle"] as? String ?: "Largo",
                            waistRise = data["waistRise"] as? String ?: "Tiro medio",
                            fitStyle = data["fitStyle"] as? String ?: "Recto",
                            formalityLevel = data["formalityLevel"] as? String ?: "Casual",
                            imageUrl = imageUrl
                        ))
                        is ShoeEntity -> viewModel.updateShoe(item.copy(
                            brand = brand, 
                            subType = data["subType"] as? String ?: "Calzado",
                            style = data["style"] as? String ?: "Casual",
                            color = color,
                            secondaryColor = data["secondaryColor"] as? String,
                            material = data["material"] as? String ?: "Cuero",
                            heelHeightStyle = data["heelHeightStyle"] as? String ?: "Plano",
                            toeStyle = data["toeStyle"] as? String ?: "Puntera lisa",
                            closureType = data["closureType"] as? String ?: "Cordones",
                            formalityLevel = data["formalityLevel"] as? String ?: "Casual",
                            imageUrl = imageUrl
                        ))
                        is TieEntity -> viewModel.updateTie(item.copy(
                            colorRange = color, pattern = data["pattern"] as? String ?: "Liso",
                            material = data["material"] as? String ?: "Seda",
                            widthCms = (data["width"] as? String)?.toDoubleOrNull() ?: 7.5,
                            imageUrl = imageUrl
                        ))
                        is WatchEntity -> viewModel.updateWatch(item.copy(
                            brand = brand, model = data["model"] as? String ?: "",
                            dialColor = data["dialColor"] as? String ?: "",
                            strapColor = data["strapColor"] as? String ?: "",
                            strapMaterial = data["strapMaterial"] as? String ?: "",
                            imageUrl = imageUrl
                        ))
                        is FragranceEntity -> viewModel.updateFragrance(item.copy(
                            brand = brand, name = data["name"] as? String ?: "",
                            occasionTag = data["occasion"] as? String ?: "",
                            profile = data["profile"] as? String ?: "",
                            imageUrl = imageUrl
                        ))
                        is JacketEntity -> viewModel.updateJacket(item.copy(
                            brand = brand, color = color,
                            type = data["type"] as? String ?: "",
                            closureType = data["closureType"] as? String ?: "",
                            imageUrl = imageUrl
                        ))
                        is BagEntity -> viewModel.updateBag(item.copy(
                            brand = brand, color = color,
                            style = data["style"] as? String ?: "",
                            material = data["material"] as? String ?: "",
                            size = data["size"] as? String ?: "Mediano",
                            imageUrl = imageUrl
                        ))
                        is DressEntity -> viewModel.updateDress(item.copy(
                            brand = brand, color = color,
                            pattern = data["pattern"] as? String ?: "Liso",
                            length = data["length"] as? String ?: "Midi",
                            sleeveStyle = data["sleeveStyle"] as? String ?: "Sin mangas",
                            material = data["material"] as? String ?: "",
                            imageUrl = imageUrl
                        ))
                        is SkirtEntity -> viewModel.updateSkirt(item.copy(
                            brand = brand, color = color,
                            pattern = data["pattern"] as? String ?: "Liso",
                            length = data["length"] as? String ?: "Corta",
                            style = data["style"] as? String ?: "",
                            material = data["material"] as? String ?: "",
                            imageUrl = imageUrl
                        ))
                    }
                } else {
                    val timestamp = System.currentTimeMillis()
                    val targetCategory = data["selectedCategory"] as? CategoryFilter ?: uiState.selectedCategory
                    when (targetCategory) {
                        CategoryFilter.SHIRTS -> viewModel.insertShirt(ShirtEntity(
                            code = "S$timestamp", 
                            brand = brand, 
                            subType = data["subType"] as? String ?: "Camisa",
                            primaryColor = color, 
                            secondaryColor = data["secondaryColor"] as? String,
                            pattern = data["pattern"] as? String ?: "Liso",
                            sleeveLength = data["sleeveLength"] as? String ?: "Manga larga",
                            necklineStyle = data["necklineStyle"] as? String ?: "Camisero",
                            material = data["material"] as? String ?: "Algodón",
                            formalityLevel = data["formalityLevel"] as? String ?: "Casual",
                            fit = data["fit"] as? String ?: "Regular",
                            imageUrl = imageUrl
                        ))
                        CategoryFilter.PANTS -> viewModel.insertPant(PantEntity(
                            code = "P$timestamp", 
                            brand = brand, 
                            subType = data["subType"] as? String ?: "Pantalón",
                            primaryColor = color, 
                            secondaryColor = data["secondaryColor"] as? String,
                            material = data["material"] as? String ?: "Algodón",
                            lengthStyle = data["lengthStyle"] as? String ?: "Largo",
                            waistRise = data["waistRise"] as? String ?: "Tiro medio",
                            fitStyle = data["fitStyle"] as? String ?: "Recto",
                            formalityLevel = data["formalityLevel"] as? String ?: "Casual",
                            imageUrl = imageUrl
                        ))
                        CategoryFilter.SHOES -> viewModel.insertShoe(ShoeEntity(
                            code = "SH$timestamp", 
                            brand = brand, 
                            subType = data["subType"] as? String ?: "Calzado",
                            style = data["style"] as? String ?: "Casual",
                            color = color,
                            secondaryColor = data["secondaryColor"] as? String,
                            material = data["material"] as? String ?: "Cuero",
                            heelHeightStyle = data["heelHeightStyle"] as? String ?: "Plano",
                            toeStyle = data["toeStyle"] as? String ?: "Puntera lisa",
                            closureType = data["closureType"] as? String ?: "Cordones",
                            formalityLevel = data["formalityLevel"] as? String ?: "Casual",
                            imageUrl = imageUrl
                        ))
                        CategoryFilter.TIES -> viewModel.insertTie(TieEntity(
                            code = "T$timestamp", colorRange = color, 
                            pattern = data["pattern"] as? String ?: "Liso",
                            material = data["material"] as? String ?: "Seda",
                            widthCms = (data["width"] as? String)?.toDoubleOrNull() ?: 7.5,
                            imageUrl = imageUrl
                        ))
                        CategoryFilter.WATCHES -> viewModel.insertWatch(WatchEntity(
                            code = "W$timestamp", brand = brand, 
                            model = data["model"] as? String ?: "Modelo",
                            dialColor = data["dialColor"] as? String ?: "Negro",
                            strapColor = data["strapColor"] as? String ?: "Negro",
                            strapMaterial = data["strapMaterial"] as? String ?: "Cuero",
                            imageUrl = imageUrl
                        ))
                        CategoryFilter.FRAGRANCES -> viewModel.insertFragrance(FragranceEntity(
                            code = "F$timestamp", brand = brand, 
                            name = data["name"] as? String ?: "Fragancia",
                            occasionTag = data["occasion"] as? String ?: "Diario",
                            profile = data["profile"] as? String ?: "Fresco",
                            imageUrl = imageUrl
                        ))
                        CategoryFilter.JACKETS -> viewModel.insertJacket(JacketEntity(
                            code = "J$timestamp", brand = brand, color = color,
                            type = data["type"] as? String ?: "Chaqueta",
                            closureType = data["closureType"] as? String ?: "Botones",
                            imageUrl = imageUrl
                        ))
                        CategoryFilter.BAGS -> viewModel.insertBag(BagEntity(
                            code = "B$timestamp", brand = brand, color = color,
                            style = data["style"] as? String ?: "Tote",
                            material = data["material"] as? String ?: "Cuero",
                            size = data["size"] as? String ?: "Mediano",
                            imageUrl = imageUrl
                        ))
                        CategoryFilter.DRESSES -> viewModel.insertDress(DressEntity(
                            code = "D$timestamp", brand = brand, color = color,
                            pattern = data["pattern"] as? String ?: "Liso",
                            length = data["length"] as? String ?: "Midi",
                            sleeveStyle = data["sleeveStyle"] as? String ?: "Corta",
                            material = data["material"] as? String ?: "Algodón",
                            imageUrl = imageUrl
                        ))
                        CategoryFilter.SKIRTS -> viewModel.insertSkirt(SkirtEntity(
                            code = "SK$timestamp", brand = brand, color = color,
                            pattern = data["pattern"] as? String ?: "Liso",
                            length = data["length"] as? String ?: "Corta",
                            style = data["style"] as? String ?: "Lápiz",
                            material = data["material"] as? String ?: "Algodón",
                            imageUrl = imageUrl
                        ))
                        else -> {
                            viewModel.insertShirt(
                                ShirtEntity(
                                    code = "S$timestamp",
                                    brand = brand,
                                    subType = "Camisa",
                                    primaryColor = color,
                                    pattern = "Liso",
                                    sleeveLength = "Manga larga",
                                    necklineStyle = "Camisero",
                                    material = "Algodón",
                                    formalityLevel = "Casual",
                                    fit = "Regular",
                                    imageUrl = imageUrl
                                )
                            )
                        } 
                    }
                }
                showAddDialog = false
                itemToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryFilterBar(
    selectedCategory: CategoryFilter,
    selectedCustomCategoryId: Long?,
    activeCategories: List<CategoryFilter>,
    customCategories: List<CustomCategoryEntity>,
    onCategorySelected: (CategoryFilter) -> Unit,
    onCustomCategorySelected: (Long) -> Unit,
    onAddCategoryRequested: () -> Unit,
    onEditCustomCategory: (CustomCategoryEntity) -> Unit,
    onDeleteCustomCategory: (CustomCategoryEntity) -> Unit
) {
    val displayCategories = listOf(CategoryFilter.ALL) + activeCategories

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(displayCategories) { category ->
            FilterChip(
                selected = selectedCategory == category && selectedCustomCategoryId == null,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName) }
            )
        }
        
        items(customCategories) { category ->
            var showMenu by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = selectedCustomCategoryId == category.id,
                    onClick = { }, 
                    label = { Text(category.name) }
                )
                
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .combinedClickable(
                            onClick = { onCustomCategorySelected(category.id) },
                            onLongClick = { showMenu = true }
                        )
                )
                
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEditCustomCategory(category)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDeleteCustomCategory(category)
                        }
                    )
                }
            }
        }

        item {
            IconButton(onClick = onAddCategoryRequested) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Categoría", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun PrendaItemCard(
    title: String,
    subtitle: String,
    imageUri: String?,
    laundryState: LaundryState,
    onDeleteRequested: () -> Unit,
    onEditRequested: () -> Unit,
    onImageClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onImageClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (laundryState == LaundryState.IN_LAUNDRY) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "Lavandería",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar Prenda") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onEditRequested()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar Prenda", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteRequested()
                            }
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    initialCategory: CustomCategoryEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, attributes: String, needsLaundry: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var attributes by remember { mutableStateOf(initialCategory?.attributeNames ?: "") }
    var needsLaundry by remember { mutableStateOf(initialCategory?.needsLaundry ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCategory == null) "Nueva Categoría" else "Editar Categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la categoría") },
                    placeholder = { Text("Ej: Sombreros") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = attributes,
                    onValueChange = { attributes = it },
                    label = { Text("Atributos (separados por comas)") },
                    placeholder = { Text("Ej: Color, Material, Estilo") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = needsLaundry,
                        onCheckedChange = { needsLaundry = it }
                    )
                    Text(text = "¿Las prendas van a lavandería?")
                }

                Text(
                    text = "Define qué datos quieres guardar y si el uso ensucia la prenda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, attributes, needsLaundry) },
                enabled = name.isNotBlank() && attributes.isNotBlank()
            ) {
                Text(if (initialCategory == null) "Crear" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var manuallySelectedOther by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    val isOtherMode = manuallySelectedOther || (value.isNotEmpty() && !options.contains(value))

    Column(modifier = modifier) {
        Box {
            OutlinedTextField(
                value = if (isOtherMode) "Otro..." else value,
                onValueChange = { },
                label = { Text(label) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Box(modifier = Modifier.matchParentSize().clickable { expanded = true })

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            manuallySelectedOther = false
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Otro...") },
                    onClick = {
                        manuallySelectedOther = true
                        if (options.contains(value) || value.isEmpty()) onValueChange("")
                        expanded = false
                    }
                )
            }
        }

        if (isOtherMode) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it) },
                label = { Text("Escribe el $label") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true
            )

            // Solicitar foco solo cuando se activa el modo "Otro" manualmente
            LaunchedEffect(manuallySelectedOther) {
                if (manuallySelectedOther) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}

@Composable
fun PrendaDialog(
    category: CategoryFilter,
    initialCustomCategoryId: Long? = null,
    customCategories: List<CustomCategoryEntity> = emptyList(),
    initialData: Map<String, Any?>? = null,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any?>) -> Unit,
    viewModel: WardrobeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wardrobeUiState by viewModel.uiState.collectAsState()
    
    var selectedCategory by remember { mutableStateOf(category) }
    var selectedCustomCategory by remember { 
        mutableStateOf(customCategories.find { it.id == (initialCustomCategoryId ?: initialData?.get("customCategoryId") as? Long) }) 
    }

    var brand by remember { mutableStateOf(initialData?.get("brand") as? String ?: "") }
    var subType by remember { mutableStateOf(initialData?.get("subType") as? String ?: (
        when(category) {
            CategoryFilter.PANTS -> "Pantalón"
            CategoryFilter.SHOES -> "Zapatos de vestir"
            else -> "Camisa"
        }
    )) }
    var color by remember { mutableStateOf(initialData?.get("color") as? String ?: "") }
    var secondaryColor by remember { mutableStateOf(initialData?.get("secondaryColor") as? String ?: "") }
    var pattern by remember { mutableStateOf(initialData?.get("pattern") as? String ?: "") }
    var sleeveLength by remember { mutableStateOf(initialData?.get("sleeveLength") as? String ?: "Manga larga") }
    var necklineStyle by remember { mutableStateOf(initialData?.get("necklineStyle") as? String ?: "Camisero") }
    var material by remember { mutableStateOf(initialData?.get("material") as? String ?: (
        when(category) {
            CategoryFilter.PANTS -> "Denim"
            CategoryFilter.SHOES -> "Cuero"
            else -> "Algodón"
        }
    )) }
    var lengthStyle by remember { mutableStateOf(initialData?.get("lengthStyle") as? String ?: "Largo") }
    var waistRise by remember { mutableStateOf(initialData?.get("waistRise") as? String ?: "Tiro medio") }
    var heelHeightStyle by remember { mutableStateOf(initialData?.get("heelHeightStyle") as? String ?: "Plano") }
    var toeStyle by remember { mutableStateOf(initialData?.get("toeStyle") as? String ?: "Puntera lisa") }
    var closureType by remember { mutableStateOf(initialData?.get("closureType") as? String ?: "Cordones") }
    var formalityLevel by remember { mutableStateOf(initialData?.get("formalityLevel") as? String ?: "Casual") }
    var fit by remember { mutableStateOf(initialData?.get("fit") as? String ?: "Regular") }
    
    var fitStyle by remember { mutableStateOf(initialData?.get("fitStyle") as? String ?: "Recto") }
    var style by remember { mutableStateOf(initialData?.get("style") as? String ?: (if (category == CategoryFilter.SHOES) "Oxford" else "")) }
    var width by remember { mutableStateOf(initialData?.get("width")?.toString() ?: "7.5") }
    var model by remember { mutableStateOf(initialData?.get("model") as? String ?: "") }
    var dialColor by remember { mutableStateOf(initialData?.get("dialColor") as? String ?: "") }
    var strapColor by remember { mutableStateOf(initialData?.get("strapColor") as? String ?: "") }
    var strapMaterial by remember { mutableStateOf(initialData?.get("strapMaterial") as? String ?: "") }
    var name by remember { mutableStateOf(initialData?.get("name") as? String ?: "") }
    var occasion by remember { mutableStateOf(initialData?.get("occasion") as? String ?: "") }
    var profile by remember { mutableStateOf(initialData?.get("profile") as? String ?: "") }
    var jacketType by remember { mutableStateOf(initialData?.get("type") as? String ?: "") }
    // closureType ya está declarado arriba para zapatos
    var bagStyle by remember { mutableStateOf(initialData?.get("style") as? String ?: "") }
    var bagSize by remember { mutableStateOf(initialData?.get("size") as? String ?: "Mediano") }
    var dressLength by remember { mutableStateOf(initialData?.get("length") as? String ?: "Midi") }
    var sleeveStyle by remember { mutableStateOf(initialData?.get("sleeveStyle") as? String ?: "Corta") }
    var skirtLength by remember { mutableStateOf(initialData?.get("length") as? String ?: "Corta") }
    var skirtStyle by remember { mutableStateOf(initialData?.get("style") as? String ?: "") }
    
    var isAnalyzing by remember { mutableStateOf(false) }
    var offlineMode by remember { mutableStateOf(false) }
    
    // Listas de opciones para Dropdowns
    val subTypesList = when(selectedCategory) {
        CategoryFilter.SHIRTS -> listOf("Camisa", "T-Shirt", "Polo", "Blusa", "Top", "Crop Top", "Bodysuit", "Guayabera")
        CategoryFilter.PANTS -> listOf("Pantalón", "Jeans", "Shorts", "Bermudas", "Leggings", "Jumpsuit", "Overol")
        CategoryFilter.SHOES -> listOf("Zapatos de vestir", "Tenis/Sneakers", "Sandalias", "Tacones", "Flats", "Botas", "Mocasines")
        else -> emptyList()
    }
    val materialsList = listOf("Algodón", "Denim", "Lino", "Seda", "Lana", "Cuero", "Gamuza", "Poliéster", "Encaje")
    val formalityLevelsList = listOf("Formal", "Smart Casual", "Casual", "Deportivo", "Gala")
    val sleeveLengthsList = listOf("Manga larga", "Manga corta", "Manga 3/4", "Sin mangas")
    val necklineStylesList = listOf("Camisero", "Cuello redondo", "Cuello en V", "Mao", "Polo", "Halter")
    val lengthsList = listOf("Largo", "Tobillero", "Midi", "Corto", "Mini")
    val waistRisesList = listOf("Tiro alto", "Tiro medio", "Tiro bajo")
    val heelHeightsList = listOf("Plano", "Tacón bajo", "Tacón medio", "Tacón alto", "Plataforma")
    val toeStylesList = listOf("Puntera lisa", "Cap Toe", "En punta", "Cuadrada", "Peep Toe")
    val closureTypesList = listOf("Cordones", "Hebilla", "Slip-on", "Cremallera", "Tiras")
    val shoeStylesList = listOf("Oxford", "Derby", "Loafer", "Chelsea", "Sneaker", "Stiletto", "Mocasín", "Sandalia")
    
    val customAttributes = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(selectedCustomCategory) {
        selectedCustomCategory?.let { cat ->
            val existingValues = (initialData?.get("customAttributes") as? String)
                ?.split("|")?.associate { 
                    val split = it.split(":")
                    split[0] to (split.getOrNull(1) ?: "")
                } ?: emptyMap()
            
            cat.attributeNames.split(",").forEach { attr ->
                customAttributes[attr.trim()] = existingValues[attr.trim()] ?: ""
            }
        }
    }

    var selectedImageUri by remember { 
        mutableStateOf(initialData?.get("imageUrl")?.toString()?.let { Uri.parse(it) }) 
    }
    var showSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isAnalyzing = true
            offlineMode = false
            analyzeImage(
                uri = uri, 
                context = context, 
                viewModel = viewModel, 
                scope = scope,
                categoryName = selectedCustomCategory?.name ?: selectedCategory.displayName,
                customAttributes = selectedCustomCategory?.attributeNames
            ) { json, offline ->
                isAnalyzing = false
                offlineMode = offline
                brand = json.optString("brand", brand)
                color = json.optString("color", color)
                pattern = json.optString("pattern", pattern)
                material = json.optString("material", material)
                
                val aiSubType = json.optString("subType", "")
                if (aiSubType.isNotBlank()) subType = aiSubType
                
                secondaryColor = json.optString("secondaryColor", secondaryColor)
                sleeveLength = json.optString("sleeveLength", sleeveLength)
                necklineStyle = json.optString("necklineStyle", necklineStyle)
                formalityLevel = json.optString("formalityLevel", formalityLevel)
                lengthStyle = json.optString("lengthStyle", lengthStyle)
                waistRise = json.optString("waistRise", waistRise)
                heelHeightStyle = json.optString("heelHeightStyle", heelHeightStyle)
                toeStyle = json.optString("toeStyle", toeStyle)
                closureType = json.optString("closureType", closureType)

                val other = json.optString("other", "")
                if (other.isNotBlank()) {
                    if (necklineStyle.isBlank()) necklineStyle = other
                    if (jacketType.isBlank()) jacketType = other
                }
                
                val aiStyle = json.optString("style", "")
                if (aiStyle.isNotBlank()) {
                    style = aiStyle
                    fit = aiStyle
                    fitStyle = aiStyle
                }

                if (selectedCustomCategory != null) {
                    selectedCustomCategory?.attributeNames?.split(",")?.forEach { attr ->
                        val key = attr.trim()
                        val value = json.optString(key, "")
                        if (value.isNotBlank()) customAttributes[key] = value
                    }
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            val uri = tempCameraUri
            selectedImageUri = uri
            uri?.let {
                isAnalyzing = true
                offlineMode = false
                analyzeImage(
                    uri = it, 
                    context = context, 
                    viewModel = viewModel, 
                    scope = scope,
                    categoryName = selectedCustomCategory?.name ?: selectedCategory.displayName,
                    customAttributes = selectedCustomCategory?.attributeNames
                ) { json, offline ->
                    isAnalyzing = false
                    offlineMode = offline
                    brand = json.optString("brand", brand)
                    color = json.optString("color", color)
                    pattern = json.optString("pattern", pattern)
                    material = json.optString("material", material)
                    
                    val aiSubType = json.optString("subType", "")
                    if (aiSubType.isNotBlank()) subType = aiSubType
                    
                    secondaryColor = json.optString("secondaryColor", secondaryColor)
                    sleeveLength = json.optString("sleeveLength", sleeveLength)
                    necklineStyle = json.optString("necklineStyle", necklineStyle)
                    formalityLevel = json.optString("formalityLevel", formalityLevel)
                    lengthStyle = json.optString("lengthStyle", lengthStyle)
                    waistRise = json.optString("waistRise", waistRise)
                    heelHeightStyle = json.optString("heelHeightStyle", heelHeightStyle)
                    toeStyle = json.optString("toeStyle", toeStyle)
                    closureType = json.optString("closureType", closureType)

                    val other = json.optString("other", "")
                    if (other.isNotBlank()) {
                        if (necklineStyle.isBlank()) necklineStyle = other
                        if (jacketType.isBlank()) jacketType = other
                    }
                    
                    val aiStyle = json.optString("style", "")
                    if (aiStyle.isNotBlank()) {
                        style = aiStyle
                        fit = aiStyle
                        fitStyle = aiStyle
                    }

                    if (selectedCustomCategory != null) {
                        selectedCustomCategory?.attributeNames?.split(",")?.forEach { attr ->
                            val key = attr.trim()
                            val value = json.optString(key, "")
                            if (value.isNotBlank()) customAttributes[key] = value
                        }
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val tempFile = File(context.cacheDir, "temp_garment_${System.currentTimeMillis()}.jpg")
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            val titleText = selectedCustomCategory?.name ?: selectedCategory.displayName
            Text(if (initialData == null) "Agregar $titleText" else "Editar $titleText") 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedImageUri != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(MaterialTheme.shapes.medium),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Prenda seleccionada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Button(
                    onClick = { showSourceDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("IA Analizando...")
                    } else {
                        Text(if (selectedImageUri != null) "Imagen Seleccionada" else "Seleccionar Foto")
                    }
                }
                
                if (isAnalyzing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Detectando prenda, marca y color...", style = MaterialTheme.typography.labelSmall)
                }

                if (offlineMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Modo offline: completa los datos manualmente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (initialData == null) {
                    Text(text = "Categoría", style = MaterialTheme.typography.labelMedium)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedCustomCategory?.name ?: selectedCategory.displayName)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            val activeCategoriesNames = wardrobeUiState.user?.activeCategories?.split(",") ?: emptyList()
                            
                            CategoryFilter.entries.filter { 
                                it != CategoryFilter.ALL && 
                                it != CategoryFilter.LAUNDRY && 
                                activeCategoriesNames.contains(it.name) 
                            }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        selectedCategory = cat
                                        selectedCustomCategory = null
                                        expanded = false
                                    }
                                )
                            }
                            customCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCustomCategory = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedCustomCategory != null) {
                    selectedCustomCategory?.attributeNames?.split(",")?.forEach { attr ->
                        val key = attr.trim()
                        OutlinedTextField(
                            value = customAttributes[key] ?: "",
                            onValueChange = { customAttributes[key] = it },
                            label = { Text(key) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    when (selectedCategory) {
                        CategoryFilter.SHIRTS -> {
                            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Tipo", value = subType, options = subTypesList, onValueChange = { subType = it })
                            OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color Principal") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = secondaryColor, onValueChange = { secondaryColor = it }, label = { Text("Color Secundario (Opcional)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Patrón") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Largo de Manga", value = sleeveLength, options = sleeveLengthsList, onValueChange = { sleeveLength = it })
                            DropdownField(label = "Cuello / Escote", value = necklineStyle, options = necklineStylesList, onValueChange = { necklineStyle = it })
                            DropdownField(label = "Material", value = material, options = materialsList, onValueChange = { material = it })
                            DropdownField(label = "Formalidad", value = formalityLevel, options = formalityLevelsList, onValueChange = { formalityLevel = it })
                            OutlinedTextField(value = fit, onValueChange = { fit = it }, label = { Text("Ajuste (Fit)") }, modifier = Modifier.fillMaxWidth())
                        }
                        CategoryFilter.PANTS -> {
                            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Tipo", value = subType, options = subTypesList, onValueChange = { subType = it })
                            OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color Principal") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = secondaryColor, onValueChange = { secondaryColor = it }, label = { Text("Color Secundario (Opcional)") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Material", value = material, options = materialsList, onValueChange = { material = it })
                            DropdownField(label = "Largo", value = lengthStyle, options = lengthsList, onValueChange = { lengthStyle = it })
                            DropdownField(label = "Tiro", value = waistRise, options = waistRisesList, onValueChange = { waistRise = it })
                            OutlinedTextField(value = fitStyle, onValueChange = { fitStyle = it }, label = { Text("Corte (Recto, Slim, Wide Leg...)") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Formalidad", value = formalityLevel, options = formalityLevelsList, onValueChange = { formalityLevel = it })
                        }
                        CategoryFilter.SHOES -> {
                            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Tipo", value = subType, options = subTypesList, onValueChange = { subType = it })
                            DropdownField(label = "Estilo / Modelo", value = style, options = shoeStylesList, onValueChange = { style = it })
                            OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color Principal") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Material", value = material, options = materialsList, onValueChange = { material = it })
                            DropdownField(label = "Altura de Tacón / Suela", value = heelHeightStyle, options = heelHeightsList, onValueChange = { heelHeightStyle = it })
                            DropdownField(label = "Estilo de Puntera", value = toeStyle, options = toeStylesList, onValueChange = { toeStyle = it })
                            DropdownField(label = "Tipo de Cierre", value = closureType, options = closureTypesList, onValueChange = { closureType = it })
                            DropdownField(label = "Formalidad", value = formalityLevel, options = formalityLevelsList, onValueChange = { formalityLevel = it })
                        }
                        CategoryFilter.TIES -> {
                            OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Gama de Colores") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Patrón") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Material", value = material, options = listOf("Seda", "Lana", "Poliéster", "Tejido"), onValueChange = { material = it })
                            OutlinedTextField(value = width, onValueChange = { width = it }, label = { Text("Ancho (cms)") }, modifier = Modifier.fillMaxWidth())
                        }
                        CategoryFilter.WATCHES -> {
                            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Modelo") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = dialColor, onValueChange = { dialColor = it }, label = { Text("Color del Dial") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = strapColor, onValueChange = { strapColor = it }, label = { Text("Color de Correa") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Material de Correa", value = strapMaterial, options = listOf("Cuero", "Acero", "Caucho", "Tela", "Titanio"), onValueChange = { strapMaterial = it })
                        }
                        CategoryFilter.FRAGRANCES -> {
                            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Ocasión", value = occasion, options = listOf("Diario", "Oficina", "Noche", "Cita", "Formal", "Deporte"), onValueChange = { occasion = it })
                            DropdownField(label = "Perfil", value = profile, options = listOf("Fresco", "Amaderado", "Cítrico", "Oriental", "Floral", "Especiado"), onValueChange = { profile = it })
                        }
                        CategoryFilter.JACKETS -> {
                            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Tipo", value = jacketType, options = listOf("Chaqueta", "Abrigo", "Blazer", "Bomber", "Parka", "Cárdigan"), onValueChange = { jacketType = it })
                            DropdownField(label = "Tipo de Cierre", value = closureType, options = listOf("Botones", "Cremallera", "Abierto", "Cruzado"), onValueChange = { closureType = it })
                        }
                        CategoryFilter.BAGS -> {
                            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Estilo", value = bagStyle, options = listOf("Tote", "Clutch", "Crossbody", "Mochila", "Maletín", "Hobo"), onValueChange = { bagStyle = it })
                            DropdownField(label = "Material", value = material, options = materialsList, onValueChange = { material = it })
                            DropdownField(label = "Tamaño", value = bagSize, options = listOf("Pequeño", "Mediano", "Grande", "Extra Grande"), onValueChange = { bagSize = it })
                        }
                        CategoryFilter.DRESSES -> {
                            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Patrón") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Largo", value = dressLength, options = lengthsList, onValueChange = { dressLength = it })
                            DropdownField(label = "Estilo de Manga", value = sleeveStyle, options = sleeveLengthsList, onValueChange = { sleeveStyle = it })
                            DropdownField(label = "Material", value = material, options = materialsList, onValueChange = { material = it })
                        }
                        CategoryFilter.SKIRTS -> {
                            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Patrón") }, modifier = Modifier.fillMaxWidth())
                            DropdownField(label = "Largo", value = skirtLength, options = lengthsList, onValueChange = { skirtLength = it })
                            DropdownField(label = "Estilo", value = skirtStyle, options = listOf("Lápiz", "A-Line", "Plisada", "Circular", "Mini", "Midi", "Maxi"), onValueChange = { skirtStyle = it })
                            DropdownField(label = "Material", value = material, options = materialsList, onValueChange = { material = it })
                        }
                        else -> {
                            Text("Selecciona una categoría válida.")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val data = mutableMapOf<String, Any?>()
                    data["imageUrl"] = selectedImageUri?.toString()
                    data["selectedCategory"] = selectedCategory
                    data["customCategoryId"] = selectedCustomCategory?.id
                    
                    if (selectedCustomCategory != null) {
                        data["isCustom"] = true
                        data["customAttributes"] = customAttributes.entries.joinToString("|") { "${it.key}:${it.value}" }
                    } else {
                        data["brand"] = brand
                        data["subType"] = subType
                        data["color"] = color
                        data["secondaryColor"] = secondaryColor
                        data["pattern"] = pattern
                        data["sleeveLength"] = sleeveLength
                        data["necklineStyle"] = necklineStyle
                        data["material"] = material
                        data["formalityLevel"] = formalityLevel
                        data["fit"] = fit
                        data["style"] = style
                        data["heelHeightStyle"] = heelHeightStyle
                        data["toeStyle"] = toeStyle
                        data["closureType"] = closureType
                        data["width"] = width
                        data["model"] = model
                        data["dialColor"] = dialColor
                        data["strapColor"] = strapColor
                        data["strapMaterial"] = strapMaterial
                        data["name"] = name
                        data["occasion"] = occasion
                        data["profile"] = profile
                        data["type"] = jacketType
                        data["styleForCustom"] = if (selectedCategory == CategoryFilter.BAGS) bagStyle else skirtStyle
                        data["size"] = bagSize
                        data["length"] = if (selectedCategory == CategoryFilter.DRESSES) dressLength else skirtLength
                        data["sleeveStyle"] = sleeveStyle
                        data["lengthStyle"] = lengthStyle
                        data["waistRise"] = waistRise
                        data["fitStyle"] = fitStyle
                    }
                    onSave(data)
                },
                enabled = if (selectedCustomCategory != null) {
                    selectedImageUri != null && customAttributes.values.all { it.isNotBlank() }
                } else {
                    when(selectedCategory) {
                        CategoryFilter.ALL, CategoryFilter.LAUNDRY -> false
                        CategoryFilter.TIES -> color.isNotEmpty()
                        CategoryFilter.FRAGRANCES -> brand.isNotEmpty() && name.isNotEmpty()
                        else -> brand.isNotEmpty()
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun saveImageToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "prenda_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
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

private fun analyzeImage(
    uri: Uri,
    context: android.content.Context,
    viewModel: WardrobeViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    categoryName: String? = null,
    customAttributes: String? = null,
    onResult: (JSONObject, Boolean) -> Unit
) {
    scope.launch {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                val inputStream = context.contentResolver.openInputStream(uri)
                android.graphics.BitmapFactory.decodeStream(inputStream)
            }.copy(Bitmap.Config.ARGB_8888, true)

            // Extracción de color LOCAL (Offline y Gratis)
            val palette = Palette.from(bitmap).generate()
            val dominantColorArgb = palette.getDominantColor(0)
            val dominantColorName = if (dominantColorArgb != 0) {
                // Un mapeo de colores muy básico para el ejemplo
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(dominantColorArgb, hsv)
                when {
                    hsv[1] < 0.1 -> "Gris / Blanco / Negro"
                    hsv[0] < 20 || hsv[0] > 340 -> "Rojo"
                    hsv[0] < 50 -> "Naranja / Café"
                    hsv[0] < 70 -> "Amarillo"
                    hsv[0] < 160 -> "Verde"
                    hsv[0] < 260 -> "Azul"
                    hsv[0] < 300 -> "Violeta"
                    else -> "Rosa"
                }
            } else ""

            // Redimensionar para optimizar IA (Max 512px para mayor velocidad)
            val scaledBitmap = if (bitmap.width > 512 || bitmap.height > 512) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val newWidth = if (ratio > 1) 512 else (512 * ratio).toInt()
                val newHeight = if (ratio > 1) (512 / ratio).toInt() else 512
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else bitmap

            val aiResult = viewModel.analyzeGarment(scaledBitmap, categoryName, customAttributes)
            
            if (aiResult != null && !aiResult.contains("Error")) {
                onResult(JSONObject(aiResult), false)
            } else {
                // Fallback Offline: Solo enviamos el color detectado localmente
                val offlineJson = JSONObject().apply {
                    put("color", dominantColorName)
                }
                onResult(offlineJson, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

package com.myperfectoutfit.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.local.enums.LaundryState
import com.myperfectoutfit.ui.components.ImageFramingDialog
import com.myperfectoutfit.ui.state.CategoryFilter
import com.myperfectoutfit.ui.state.WardrobeUiState
import com.myperfectoutfit.ui.viewmodel.WardrobeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeScreen(
    viewModel: WardrobeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Any?>(null) }
    var categoryToDelete by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var fullImageToView by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            if (uiState.selectedCategory != CategoryFilter.ALL || uiState.selectedCustomCategoryId != null) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir Prenda")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            CategoryFilters(
                uiState = uiState,
                onSelect = { viewModel.selectCategory(it) },
                onSelectCustom = { viewModel.selectCustomCategory(it) },
                onAddCustom = { showCustomCategoryDialog = true },
                onDeleteCustom = { categoryToDelete = it }
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                WardrobeGrid(
                    uiState = uiState,
                    onPreview = { fullImageToView = it },
                    onEdit = { itemToEdit = it },
                    onDelete = { showDeleteConfirm = it }
                )
            }
        }
    }

    if (fullImageToView != null) {
        FullImagePreview(
            imageUrl = fullImageToView!!,
            onDismiss = { fullImageToView = null }
        )
    }

    if (showAddDialog || itemToEdit != null) {
        PrendaDialog(
            category = itemToEdit?.get("selectedCategory") as? CategoryFilter ?: uiState.selectedCategory,
            initialCustomCategoryId = itemToEdit?.get("customCategoryId") as? Long ?: uiState.selectedCustomCategoryId,
            customCategories = uiState.customCategories,
            initialData = itemToEdit,
            onDismiss = { 
                showAddDialog = false
                itemToEdit = null
            },
            onSave = { data ->
                if (itemToEdit == null) {
                    saveNewItem(data, viewModel)
                } else {
                    updateExistingItem(data, itemToEdit!!, viewModel)
                }
                showAddDialog = false
                itemToEdit = null
            }
        )
    }

    if (showCustomCategoryDialog) {
        CustomCategoryDialog(
            onDismiss = { showCustomCategoryDialog = false },
            onSave = { name, attrs, needsLaundry ->
                viewModel.insertCustomCategory(name, attrs, needsLaundry)
                showCustomCategoryDialog = false
            }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Eliminar Categoría") },
            text = { Text("¿Estás seguro de que quieres eliminar la categoría '${categoryToDelete?.name}'? Se borrarán también todas las prendas que contenga.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomCategory(categoryToDelete!!)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDeleteConfirm != null) {
        DeleteConfirmDialog(
            item = showDeleteConfirm!!,
            onDismiss = { showDeleteConfirm = null },
            onConfirm = {
                deleteItem(showDeleteConfirm!!, viewModel)
                showDeleteConfirm = null
            }
        )
    }
}

@Composable
fun CategoryFilters(
    uiState: WardrobeUiState,
    onSelect: (CategoryFilter) -> Unit,
    onSelectCustom: (Long) -> Unit,
    onAddCustom: () -> Unit,
    onDeleteCustom: (CustomCategoryEntity) -> Unit
) {
    val activeCategoriesNames = uiState.user?.activeCategories?.split(",") ?: emptyList()

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CategoryChip(
                    label = CategoryFilter.ALL.displayName,
                    count = null,
                    selected = uiState.selectedCategory == CategoryFilter.ALL && uiState.selectedCustomCategoryId == null,
                    onClick = { onSelect(CategoryFilter.ALL) }
                )
            }

            items(CategoryFilter.entries.filter { 
                it != CategoryFilter.ALL && it != CategoryFilter.LAUNDRY && activeCategoriesNames.contains(it.name)
            }) { category ->
                val count = when(category) {
                    CategoryFilter.SHIRTS -> uiState.shirts.size
                    CategoryFilter.PANTS -> uiState.pants.size
                    CategoryFilter.SHOES -> uiState.shoes.size
                    CategoryFilter.JACKETS -> uiState.jackets.size
                    CategoryFilter.FRAGRANCES -> uiState.fragrances.size
                    CategoryFilter.WATCHES -> uiState.watches.size
                    CategoryFilter.BAGS -> uiState.bags.size
                    CategoryFilter.DRESSES -> uiState.dresses.size
                    CategoryFilter.SKIRTS -> uiState.skirts.size
                    CategoryFilter.TIES -> uiState.ties.size
                    else -> 0
                }
                CategoryChip(
                    label = category.displayName,
                    count = count,
                    selected = uiState.selectedCategory == category && uiState.selectedCustomCategoryId == null,
                    onClick = { onSelect(category) }
                )
            }
            
            items(uiState.customCategories) { cat ->
                val count = uiState.customGarments.count { it.categoryId == cat.id }
                CategoryChip(
                    label = cat.name,
                    count = count,
                    selected = uiState.selectedCustomCategoryId == cat.id,
                    onClick = { onSelectCustom(cat.id) },
                    onDelete = { onDeleteCustom(cat) }
                )
            }
        }
        
        IconButton(onClick = onAddCustom, modifier = Modifier.padding(end = 8.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Nueva Categoría", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            
            if (count != null) {
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                    shape = CircleShape
                ) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            if (onDelete != null) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Eliminar",
                    modifier = Modifier.size(14.dp).clickable { onDelete() },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WardrobeGrid(
    uiState: WardrobeUiState,
    onPreview: (String) -> Unit,
    onEdit: (Map<String, Any?>) -> Unit,
    onDelete: (Any) -> Unit
) {
    val garmentList = when {
        uiState.selectedCustomCategoryId != null -> uiState.customGarments.filter { it.categoryId == uiState.selectedCustomCategoryId }
        uiState.selectedCategory == CategoryFilter.ALL -> (uiState.shirts + uiState.pants + uiState.shoes + uiState.jackets + uiState.fragrances + uiState.watches + uiState.bags + uiState.dresses + uiState.skirts + uiState.customGarments + uiState.ties)
        uiState.selectedCategory == CategoryFilter.LAUNDRY -> (uiState.shirts + uiState.pants + uiState.jackets + uiState.dresses + uiState.skirts + uiState.customGarments + uiState.ties).filter { 
            val laundry = when(it) {
                is ShirtEntity -> it.laundryState
                is PantEntity -> it.laundryState
                is JacketEntity -> it.laundryState
                is DressEntity -> it.laundryState
                is SkirtEntity -> it.laundryState
                is CustomGarmentEntity -> it.laundryState
                is TieEntity -> it.laundryState
                else -> LaundryState.CLEAN
            }
            laundry != LaundryState.CLEAN
        }
        uiState.selectedCategory == CategoryFilter.SHIRTS -> uiState.shirts
        uiState.selectedCategory == CategoryFilter.PANTS -> uiState.pants
        uiState.selectedCategory == CategoryFilter.SHOES -> uiState.shoes
        uiState.selectedCategory == CategoryFilter.JACKETS -> uiState.jackets
        uiState.selectedCategory == CategoryFilter.FRAGRANCES -> uiState.fragrances
        uiState.selectedCategory == CategoryFilter.WATCHES -> uiState.watches
        uiState.selectedCategory == CategoryFilter.BAGS -> uiState.bags
        uiState.selectedCategory == CategoryFilter.DRESSES -> uiState.dresses
        uiState.selectedCategory == CategoryFilter.SKIRTS -> uiState.skirts
        uiState.selectedCategory == CategoryFilter.TIES -> uiState.ties
        else -> emptyList()
    }

    if (garmentList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay prendas en esta categoría.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(garmentList) { item ->
                GarmentCard(
                    item = item,
                    onClick = { 
                        val url = when(item) {
                            is ShirtEntity -> item.imageUrl
                            is PantEntity -> item.imageUrl
                            is ShoeEntity -> item.imageUrl
                            is JacketEntity -> item.imageUrl
                            is FragranceEntity -> item.imageUrl
                            is WatchEntity -> item.imageUrl
                            is BagEntity -> item.imageUrl
                            is DressEntity -> item.imageUrl
                            is SkirtEntity -> item.imageUrl
                            is TieEntity -> item.imageUrl
                            is CustomGarmentEntity -> item.imageUrl
                            else -> ""
                        }
                        onPreview(url)
                    },
                    onEdit = { onEdit(itemToMap(item)) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GarmentCard(
    item: Any,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val imageUrl = when(item) {
        is ShirtEntity -> item.imageUrl
        is PantEntity -> item.imageUrl
        is ShoeEntity -> item.imageUrl
        is JacketEntity -> item.imageUrl
        is FragranceEntity -> item.imageUrl
        is WatchEntity -> item.imageUrl
        is BagEntity -> item.imageUrl
        is DressEntity -> item.imageUrl
        is SkirtEntity -> item.imageUrl
        is TieEntity -> item.imageUrl
        is CustomGarmentEntity -> item.imageUrl
        else -> ""
    }
    
    val title = when(item) {
        is ShirtEntity -> item.brand
        is PantEntity -> item.brand ?: "Sin marca"
        is ShoeEntity -> item.brand
        is JacketEntity -> item.brand ?: "Sin marca"
        is FragranceEntity -> item.brand
        is WatchEntity -> item.brand
        is BagEntity -> item.brand ?: "Sin marca"
        is DressEntity -> item.brand ?: "Sin marca"
        is SkirtEntity -> item.brand ?: "Sin marca"
        is TieEntity -> "Corbata"
        is CustomGarmentEntity -> item.attributeValues.split("|").firstOrNull { it.contains("Marca", ignoreCase = true) }?.split(":")?.getOrNull(1) ?: "Personalizado"
        else -> "Prenda"
    }

    val subtitle = when(item) {
        is ShirtEntity -> item.pattern
        is PantEntity -> item.primaryColor
        is ShoeEntity -> item.color
        is JacketEntity -> item.color
        is FragranceEntity -> item.name
        is WatchEntity -> item.model
        is BagEntity -> item.color
        is DressEntity -> item.pattern
        is SkirtEntity -> item.pattern
        is TieEntity -> item.colorRange
        is CustomGarmentEntity -> item.attributeValues.split("|").getOrNull(1)?.split(":")?.getOrNull(1) ?: ""
        else -> ""
    }

    val isLaundry = when(item) {
        is ShirtEntity -> item.laundryState != LaundryState.CLEAN
        is PantEntity -> item.laundryState != LaundryState.CLEAN
        is JacketEntity -> item.laundryState != LaundryState.CLEAN
        is DressEntity -> item.laundryState != LaundryState.CLEAN
        is SkirtEntity -> item.laundryState != LaundryState.CLEAN
        is TieEntity -> item.laundryState != LaundryState.CLEAN
        is CustomGarmentEntity -> item.laundryState != LaundryState.CLEAN
        else -> false
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentScale = ContentScale.Crop
            )
            
            if (isLaundry) {
                Surface(
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.padding(8.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Lavandería",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            var showMenu by remember { mutableStateOf(false) }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White.copy(alpha = 0.8f))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
fun FullImagePreview(imageUrl: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var bagStyle by remember { mutableStateOf(initialData?.get("style") as? String ?: "") }
    var bagSize by remember { mutableStateOf(initialData?.get("size") as? String ?: "Mediano") }
    var dressLength by remember { mutableStateOf(initialData?.get("length") as? String ?: "Midi") }
    var sleeveStyle by remember { mutableStateOf(initialData?.get("sleeveStyle") as? String ?: "Corta") }
    var skirtLength by remember { mutableStateOf(initialData?.get("length") as? String ?: "Corta") }
    var skirtStyle by remember { mutableStateOf(initialData?.get("style") as? String ?: "") }
    
    var isAnalyzing by remember { mutableStateOf(false) }
    var offlineMode by remember { mutableStateOf(false) }
    
    val materialsList = listOf("Algodón", "Denim", "Lino", "Seda", "Lana", "Cuero", "Gamuza", "Poliéster", "Encaje")
    val subTypesList = when(selectedCategory) {
        CategoryFilter.SHIRTS -> listOf("Camisa", "T-Shirt", "Polo", "Blusa", "Top", "Crop Top", "Bodysuit", "Guayabera")
        CategoryFilter.PANTS -> listOf("Pantalón", "Jeans", "Shorts", "Bermudas", "Leggings", "Jumpsuit", "Overol")
        CategoryFilter.SHOES -> listOf("Zapatos de vestir", "Tenis/Sneakers", "Sandalias", "Tacones", "Flats", "Botas", "Mocasines")
        else -> emptyList()
    }
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
        val customCat = selectedCustomCategory
        if (customCat != null) {
            val existingValues = (initialData?.get("customAttributes") as? String)
                ?.split("|")?.associate { 
                    val split = it.split(":")
                    split[0] to (split.getOrNull(1) ?: "")
                } ?: emptyMap()
            customCat.attributeNames.split(",").forEach { attr ->
                customAttributes[attr.trim()] = existingValues[attr.trim()] ?: ""
            }
        }
    }

    var selectedImageUri by remember { mutableStateOf(initialData?.get("imageUrl")?.toString()?.toUri()) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            isAnalyzing = true
            offlineMode = false
            val currentCat = selectedCustomCategory?.name ?: selectedCategory.displayName
            val currentAttrs = selectedCustomCategory?.attributeNames
            analyzeImage(uri, context, viewModel, scope, currentCat, currentAttrs,
                onResult = { json, offline ->
                    isAnalyzing = false
                    offlineMode = offline
                    
                    // Función auxiliar para obtener strings limpios y manejar listas de la IA
                    fun getClean(key: String): String {
                        val v = json.opt(key) ?: return ""
                        if (v == JSONObject.NULL) return ""
                        
                        // Si la IA devolvió una lista ["a", "b"], la convertimos en "A, B"
                        if (v is org.json.JSONArray) {
                            val items = mutableListOf<String>()
                            for (i in 0 until v.length()) {
                                items.add(v.optString(i))
                            }
                            return items.joinToString(", ").replace("\\", "")
                        }
                        
                        val s = v.toString()
                        return if (s == "null" || s == "None") "" else s.replace("\\", "")
                    }

                    brand = getClean("brand").ifBlank { brand }
                    name = getClean("name").ifBlank { name }
                    val aiOccasion = getClean("occasionTag")
                    if (aiOccasion.isNotBlank()) occasion = aiOccasion
                    profile = getClean("profile").ifBlank { profile }
                    model = getClean("model").ifBlank { model }
                    dialColor = getClean("dialColor").ifBlank { dialColor }
                    strapColor = getClean("strapColor").ifBlank { strapColor }
                    strapMaterial = getClean("strapMaterial").ifBlank { strapMaterial }
                    
                    val detectedColor = getClean("primaryColor").ifBlank { getClean("color") }
                    if (detectedColor.isNotBlank()) color = detectedColor
                    
                    secondaryColor = getClean("secondaryColor").ifBlank { secondaryColor }
                    pattern = getClean("pattern").ifBlank { pattern }
                    material = getClean("material").ifBlank { material }
                    val aiWidth = getClean("widthCms")
                    if (aiWidth.isNotBlank()) width = aiWidth
                    val aiType = getClean("type")
                    if (aiType.isNotBlank()) jacketType = aiType
                    val aiSize = getClean("size")
                    if (aiSize.isNotBlank()) bagSize = aiSize
                    val aiLength = getClean("length")
                    if (aiLength.isNotBlank()) { dressLength = aiLength; skirtLength = aiLength }
                    val aiSleeveStyle = getClean("sleeveStyle")
                    if (aiSleeveStyle.isNotBlank()) sleeveStyle = aiSleeveStyle
                    val aiSubType = getClean("subType")
                    if (aiSubType.isNotBlank()) subType = aiSubType
                    sleeveLength = getClean("sleeveLength").ifBlank { sleeveLength }
                    necklineStyle = getClean("necklineStyle").ifBlank { necklineStyle }
                    formalityLevel = getClean("formalityLevel").ifBlank { formalityLevel }
                    lengthStyle = getClean("lengthStyle").ifBlank { lengthStyle }
                    waistRise = getClean("waistRise").ifBlank { waistRise }
                    fitStyle = getClean("fitStyle").ifBlank { fitStyle }
                    fit = getClean("fit").ifBlank { fit }
                    
                    val otherVal = getClean("other")
                    if (otherVal.isNotBlank()) {
                        if (necklineStyle.isBlank()) necklineStyle = otherVal
                        if (jacketType.isBlank()) jacketType = otherVal
                    }
                    val aiStyle = getClean("style")
                    if (aiStyle.isNotBlank()) {
                        style = aiStyle; fit = aiStyle; fitStyle = aiStyle; bagStyle = aiStyle; skirtStyle = aiStyle
                    }
                    val customCat = selectedCustomCategory
                    if (customCat != null) {
                        customCat.attributeNames.split(",").forEach { attr ->
                            val key = attr.trim()
                            val value = getClean(key)
                            if (value.isNotBlank()) customAttributes[key] = value
                        }
                    }
                }
            )
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = tempCameraUri
            selectedImageUri = uri
            uri?.let {
                isAnalyzing = true
                offlineMode = false
                val currentCat = selectedCustomCategory?.name ?: selectedCategory.displayName
                val currentAttrs = selectedCustomCategory?.attributeNames
                analyzeImage(it, context, viewModel, scope, currentCat, currentAttrs,
                    onResult = { json, offline ->
                        isAnalyzing = false
                        offlineMode = offline
                        
                        fun getClean(key: String): String {
                            val v = json.optString(key, "")
                            return if (v == "null" || v == "None") "" else v
                        }

                        brand = getClean("brand").ifBlank { brand }
                        name = getClean("name").ifBlank { name }
                        val aiOccasion = getClean("occasionTag")
                        if (aiOccasion.isNotBlank()) occasion = aiOccasion
                        profile = getClean("profile").ifBlank { profile }
                        model = getClean("model").ifBlank { model }
                        dialColor = getClean("dialColor").ifBlank { dialColor }
                        strapColor = getClean("strapColor").ifBlank { strapColor }
                        strapMaterial = getClean("strapMaterial").ifBlank { strapMaterial }
                        
                        val detectedColor = getClean("primaryColor").ifBlank { getClean("color") }
                        if (detectedColor.isNotBlank()) color = detectedColor
                        
                        secondaryColor = getClean("secondaryColor").ifBlank { secondaryColor }
                        pattern = getClean("pattern").ifBlank { pattern }
                        material = getClean("material").ifBlank { material }
                        val aiWidth = getClean("widthCms")
                        if (aiWidth.isNotBlank()) width = aiWidth
                        val aiType = getClean("type")
                        if (aiType.isNotBlank()) jacketType = aiType
                        val aiSize = getClean("size")
                        if (aiSize.isNotBlank()) bagSize = aiSize
                        val aiLength = getClean("length")
                        if (aiLength.isNotBlank()) { dressLength = aiLength; skirtLength = aiLength }
                        val aiSleeveStyle = getClean("sleeveStyle")
                        if (aiSleeveStyle.isNotBlank()) sleeveStyle = aiSleeveStyle
                        val aiSubType = getClean("subType")
                        if (aiSubType.isNotBlank()) subType = aiSubType
                        sleeveLength = getClean("sleeveLength").ifBlank { sleeveLength }
                        necklineStyle = getClean("necklineStyle").ifBlank { necklineStyle }
                        formalityLevel = getClean("formalityLevel").ifBlank { formalityLevel }
                        lengthStyle = getClean("lengthStyle").ifBlank { lengthStyle }
                        waistRise = getClean("waistRise").ifBlank { waistRise }
                        fitStyle = getClean("fitStyle").ifBlank { fitStyle }
                        fit = getClean("fit").ifBlank { fit }
                        
                        val otherVal = getClean("other")
                        if (otherVal.isNotBlank()) {
                            if (necklineStyle.isBlank()) necklineStyle = otherVal
                            if (jacketType.isBlank()) jacketType = otherVal
                        }
                        val aiStyle = getClean("style")
                        if (aiStyle.isNotBlank()) {
                            style = aiStyle; fit = aiStyle; fitStyle = aiStyle; bagStyle = aiStyle; skirtStyle = aiStyle
                        }
                        val customCat = selectedCustomCategory
                        if (customCat != null) {
                            customCat.attributeNames.split(",").forEach { attr ->
                                val key = attr.trim()
                                val value = getClean(key)
                                if (value.isNotBlank()) customAttributes[key] = value
                            }
                        }
                    }
                )
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val tempFile = File(context.cacheDir, "temp_garment_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
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

                Button(onClick = { showSourceDialog = true }, modifier = Modifier.fillMaxWidth()) {
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
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Modo offline: completa los datos manualmente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
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
                            CategoryFilter.entries.filter { it != CategoryFilter.ALL && it != CategoryFilter.LAUNDRY && activeCategoriesNames.contains(it.name) }.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat.displayName) }, onClick = { selectedCategory = cat; selectedCustomCategory = null; expanded = false })
                            }
                            customCategories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCustomCategory = cat; expanded = false })
                            }
                        }
                    }
                }

                val currentCustomCat = selectedCustomCategory
                if (currentCustomCat != null) {
                    currentCustomCat.attributeNames.split(",").forEach { attr ->
                        val key = attr.trim()
                        OutlinedTextField(value = customAttributes[key] ?: "", onValueChange = { customAttributes[key] = it }, label = { Text(key) }, modifier = Modifier.fillMaxWidth())
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
                            OutlinedTextField(value = occasion, onValueChange = { occasion = it }, label = { Text("Ocasión") }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Ej: Diario, Cita, Noche...") })
                            OutlinedTextField(value = profile, onValueChange = { profile = it }, label = { Text("Perfil Olfativo") }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Ej: Fresco, Cítrico, Amaderado...") })
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
                        else -> { Text("Selecciona una categoría válida.") }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalImageUrl = selectedImageUri?.let { uri ->
                        if (uri.scheme == "content" || uri.toString().contains("cache")) {
                            saveImageToInternalStorage(context, uri)
                        } else {
                            uri.toString()
                        }
                    } ?: ""

                    val data = mutableMapOf<String, Any?>()
                    data["imageUrl"] = finalImageUrl
                    data["selectedCategory"] = selectedCategory
                    data["customCategoryId"] = selectedCustomCategory?.id
                    if (selectedCustomCategory != null) {
                        data["isCustom"] = true
                        data["customAttributes"] = customAttributes.entries.joinToString("|") { "${it.key}:${it.value}" }
                    } else {
                        data["brand"] = brand; data["subType"] = subType; data["color"] = color; data["secondaryColor"] = secondaryColor
                        data["pattern"] = pattern; data["sleeveLength"] = sleeveLength; data["necklineStyle"] = necklineStyle
                        data["material"] = material; data["formalityLevel"] = formalityLevel; data["fit"] = fit
                        data["style"] = style; data["heelHeightStyle"] = heelHeightStyle; data["toeStyle"] = toeStyle
                        data["closureType"] = closureType; data["width"] = width; data["model"] = model
                        data["dialColor"] = dialColor; data["strapColor"] = strapColor; data["strapMaterial"] = strapMaterial
                        data["name"] = name; data["occasion"] = occasion; data["profile"] = profile
                        data["type"] = jacketType; data["styleForCustom"] = if (selectedCategory == CategoryFilter.BAGS) bagStyle else skirtStyle
                        data["size"] = bagSize; data["length"] = if (selectedCategory == CategoryFilter.DRESSES) dressLength else skirtLength
                        data["sleeveStyle"] = sleeveStyle; data["lengthStyle"] = lengthStyle; data["waistRise"] = waistRise; data["fitStyle"] = fitStyle
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
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun DropdownField(label: String, value: String, options: List<String>, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var manuallySelectedOther by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val isOtherMode = manuallySelectedOther || (value.isNotEmpty() && !options.contains(value))
    Column(modifier = modifier) {
        Box {
            OutlinedTextField(value = if (isOtherMode) "Otro..." else value, onValueChange = { }, label = { Text(label) }, readOnly = true, trailingIcon = { IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null) } }, modifier = Modifier.fillMaxWidth())
            Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { manuallySelectedOther = false; onValueChange(option); expanded = false })
                }
                DropdownMenuItem(text = { Text("Otro...") }, onClick = { manuallySelectedOther = true; if (options.contains(value) || value.isEmpty()) onValueChange(""); expanded = false })
            }
        }
        if (isOtherMode) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(value = value, onValueChange = { onValueChange(it) }, label = { Text("Escribe el $label") }, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester), singleLine = true)
            LaunchedEffect(manuallySelectedOther) { if (manuallySelectedOther) focusRequester.requestFocus() }
        }
    }
}

@Composable
fun CustomCategoryDialog(onDismiss: () -> Unit, onSave: (String, String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var attrs by remember { mutableStateOf("") }
    var needsLaundry by remember { mutableStateOf(true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nueva Categoría") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre de la categoría") }, placeholder = { Text("Ej: Relojes, Cinturones...") }, modifier = Modifier.fillMaxWidth()) ; OutlinedTextField(value = attrs, onValueChange = { attrs = it }, label = { Text("Atributos (separados por coma)") }, placeholder = { Text("Ej: Marca, Color, Material...") }, modifier = Modifier.fillMaxWidth()) ; Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = needsLaundry, onCheckedChange = { needsLaundry = it }) ; Spacer(modifier = Modifier.width(8.dp)) ; Text("¿Requiere lavandería?") } } }, confirmButton = { Button(onClick = { onSave(name, attrs, needsLaundry) }, enabled = name.isNotBlank() && attrs.isNotBlank()) { Text("Crear") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun DeleteConfirmDialog(item: Any, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Eliminar Prenda") }, text = { Text("¿Estás seguro de que quieres eliminar esta prenda del armario? Esta acción no se puede deshacer.") }, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

private fun itemToMap(item: Any): Map<String, Any?> {
    val data = mutableMapOf<String, Any?>()
    when (item) {
        is ShirtEntity -> {
            data["id"] = item.id; data["userId"] = item.userId; data["brand"] = item.brand; data["subType"] = item.subType; data["color"] = item.primaryColor; data["secondaryColor"] = item.secondaryColor; data["pattern"] = item.pattern; data["sleeveLength"] = item.sleeveLength; data["necklineStyle"] = item.necklineStyle; data["material"] = item.material; data["formalityLevel"] = item.formalityLevel; data["fit"] = item.fit; data["imageUrl"] = item.imageUrl; data["selectedCategory"] = CategoryFilter.SHIRTS
        }
        is PantEntity -> {
            data["id"] = item.id; data["userId"] = item.userId; data["brand"] = item.brand; data["subType"] = item.subType; data["color"] = item.primaryColor; data["secondaryColor"] = item.secondaryColor; data["material"] = item.material; data["lengthStyle"] = item.lengthStyle; data["waistRise"] = item.waistRise; data["fitStyle"] = item.fitStyle; data["formalityLevel"] = item.formalityLevel; data["imageUrl"] = item.imageUrl; data["selectedCategory"] = CategoryFilter.PANTS
        }
        is ShoeEntity -> {
            data["id"] = item.id; data["userId"] = item.userId; data["brand"] = item.brand; data["subType"] = item.subType; data["style"] = item.style; data["color"] = item.color; data["secondaryColor"] = item.secondaryColor; data["material"] = item.material; data["heelHeightStyle"] = item.heelHeightStyle; data["toeStyle"] = item.toeStyle; data["closureType"] = item.closureType; data["formalityLevel"] = item.formalityLevel; data["imageUrl"] = item.imageUrl; data["selectedCategory"] = CategoryFilter.SHOES
        }
        is JacketEntity -> {
            data["id"] = item.id; data["userId"] = item.userId; data["brand"] = item.brand; data["color"] = item.color; data["type"] = item.type; data["closureType"] = item.closureType; data["imageUrl"] = item.imageUrl; data["selectedCategory"] = CategoryFilter.JACKETS
        }
        is FragranceEntity -> {
            data["id"] = item.id; data["userId"] = item.userId; data["brand"] = item.brand; data["name"] = item.name; data["occasion"] = item.occasionTag; data["profile"] = item.profile; data["imageUrl"] = item.imageUrl; data["selectedCategory"] = CategoryFilter.FRAGRANCES
        }
        is WatchEntity -> {
            data["id"] = item.id; data["userId"] = item.userId; data["brand"] = item.brand; data["model"] = item.model; data["dialColor"] = item.dialColor; data["strapColor"] = item.strapColor; data["strapMaterial"] = item.strapMaterial; data["imageUrl"] = item.imageUrl; data["selectedCategory"] = CategoryFilter.WATCHES
        }
        is BagEntity -> {
            data["id"] = item.id; data["userId"] = item.userId; data["brand"] = item.brand; data["color"] = item.color; data["style"] = item.style; data["material"] = item.material; data["size"] = item.size; data["imageUrl"] = item.imageUrl; data["selectedCategory"] = CategoryFilter.BAGS
        }
        is DressEntity -> {
            data["id"] = item.id; data["userId"] = item.userId; data["brand"] = item.brand; data["color"] = item.color; data["pattern"] = item.pattern; data["length"] = item.length; data["sleeveStyle"] = item.sleeveStyle; data["material"] = item.material; data["imageUrl"] = item.imageUrl; data["selectedCategory"] = CategoryFilter.DRESSES
        }
        is SkirtEntity -> {
            data["id"] = item.id; data["userId"] = item.userId; data["brand"] = item.brand; data["color"] = item.color; data["pattern"] = item.pattern; data["length"] = item.length; data["style"] = item.style; data["material"] = item.material; data["imageUrl"] = item.imageUrl; data["selectedCategory"] = CategoryFilter.SKIRTS
        }
        is TieEntity -> {
            data["id"] = item.id; data["userId"] = item.userId; data["color"] = item.colorRange; data["pattern"] = item.pattern; data["material"] = item.material; data["width"] = item.widthCms; data["imageUrl"] = item.imageUrl; data["selectedCategory"] = CategoryFilter.TIES
        }
        is CustomGarmentEntity -> {
            data["id"] = item.id; data["userId"] = item.userId; data["categoryId"] = item.categoryId; data["imageUrl"] = item.imageUrl; data["customAttributes"] = item.attributeValues; data["selectedCategory"] = CategoryFilter.ALL; data["customCategoryId"] = item.categoryId
        }
    }
    return data
}

private fun saveNewItem(data: Map<String, Any?>, viewModel: WardrobeViewModel) {
    val userId = (data["userId"] as? Long) ?: 1L
    val imageUrl = (data["imageUrl"] as? String) ?: ""
    when (data["selectedCategory"] as? CategoryFilter) {
        CategoryFilter.SHIRTS -> viewModel.insertShirt(ShirtEntity(userId = userId, brand = data["brand"] as String, subType = data["subType"] as String, primaryColor = data["color"] as String, secondaryColor = data["secondaryColor"] as? String, pattern = data["pattern"] as String, sleeveLength = data["sleeveLength"] as String, necklineStyle = data["necklineStyle"] as String, material = data["material"] as String, formalityLevel = data["formalityLevel"] as String, fit = data["fit"] as String, imageUrl = imageUrl, code = "SH${System.currentTimeMillis()}"))
        CategoryFilter.PANTS -> viewModel.insertPant(PantEntity(userId = userId, brand = data["brand"] as? String, subType = data["subType"] as String, primaryColor = data["color"] as String, secondaryColor = data["secondaryColor"] as? String, material = data["material"] as String, lengthStyle = data["lengthStyle"] as String, waistRise = data["waistRise"] as String, fitStyle = data["fitStyle"] as String, formalityLevel = data["formalityLevel"] as String, imageUrl = imageUrl, code = "PA${System.currentTimeMillis()}"))
        CategoryFilter.SHOES -> viewModel.insertShoe(ShoeEntity(userId = userId, brand = data["brand"] as String, subType = data["subType"] as String, style = data["style"] as String, color = data["color"] as String, material = data["material"] as String, heelHeightStyle = data["heelHeightStyle"] as String, toeStyle = data["toeStyle"] as String, closureType = data["closureType"] as String, formalityLevel = data["formalityLevel"] as String, imageUrl = imageUrl, code = "SH${System.currentTimeMillis()}"))
        CategoryFilter.JACKETS -> viewModel.insertJacket(JacketEntity(userId = userId, brand = data["brand"] as? String, color = data["color"] as String, type = data["type"] as String, closureType = data["closureType"] as String, imageUrl = imageUrl, code = "JA${System.currentTimeMillis()}"))
        CategoryFilter.FRAGRANCES -> viewModel.insertFragrance(FragranceEntity(userId = userId, brand = data["brand"] as String, name = data["name"] as String, occasionTag = data["occasion"] as String, profile = data["profile"] as String, imageUrl = imageUrl, code = "FR${System.currentTimeMillis()}"))
        CategoryFilter.WATCHES -> viewModel.insertWatch(WatchEntity(userId = userId, brand = data["brand"] as String, model = data["model"] as String, dialColor = data["dialColor"] as String, strapColor = data["strapColor"] as String, strapMaterial = data["strapMaterial"] as String, imageUrl = imageUrl, code = "WA${System.currentTimeMillis()}"))
        CategoryFilter.BAGS -> viewModel.insertBag(BagEntity(userId = userId, brand = data["brand"] as? String, color = data["color"] as String, style = data["style"] as String, material = data["material"] as String, size = data["size"] as String, imageUrl = imageUrl, code = "BA${System.currentTimeMillis()}"))
        CategoryFilter.DRESSES -> viewModel.insertDress(DressEntity(userId = userId, brand = data["brand"] as? String, color = data["color"] as String, pattern = data["pattern"] as String, length = data["length"] as String, sleeveStyle = data["sleeveStyle"] as String, material = data["material"] as String, imageUrl = imageUrl, code = "DR${System.currentTimeMillis()}"))
        CategoryFilter.SKIRTS -> viewModel.insertSkirt(SkirtEntity(userId = userId, brand = data["brand"] as? String, color = data["color"] as String, pattern = data["pattern"] as String, length = data["length"] as String, style = data["style"] as String, material = data["material"] as String, imageUrl = imageUrl, code = "SK${System.currentTimeMillis()}"))
        CategoryFilter.TIES -> viewModel.insertTie(TieEntity(userId = userId, colorRange = data["color"] as String, pattern = data["pattern"] as String, material = data["material"] as String, widthCms = (data["width"]?.toString()?.toDoubleOrNull() ?: 7.5), imageUrl = imageUrl, code = "TI${System.currentTimeMillis()}"))
        else -> {
            if (data["isCustom"] == true) {
                viewModel.insertCustomGarment(CustomGarmentEntity(categoryId = data["customCategoryId"] as Long, userId = userId, imageUrl = imageUrl, attributeValues = data["customAttributes"] as String))
            }
        }
    }
}

private fun updateExistingItem(data: Map<String, Any?>, original: Map<String, Any?>, viewModel: WardrobeViewModel) {
    val id = original["id"] as Long
    val userId = original["userId"] as Long
    val code = (original["code"] as? String) ?: "UP${System.currentTimeMillis()}"
    val imageUrl = (data["imageUrl"] as? String) ?: (original["imageUrl"] as String)
    when (data["selectedCategory"] as? CategoryFilter) {
        CategoryFilter.SHIRTS -> viewModel.updateShirt(ShirtEntity(id = id, userId = userId, brand = data["brand"] as String, subType = data["subType"] as String, primaryColor = data["color"] as String, secondaryColor = data["secondaryColor"] as? String, pattern = data["pattern"] as String, sleeveLength = data["sleeveLength"] as String, necklineStyle = data["necklineStyle"] as String, material = data["material"] as String, formalityLevel = data["formalityLevel"] as String, fit = data["fit"] as String, imageUrl = imageUrl, code = code))
        CategoryFilter.PANTS -> viewModel.updatePant(PantEntity(id = id, userId = userId, brand = data["brand"] as? String, subType = data["subType"] as String, primaryColor = data["color"] as String, secondaryColor = data["secondaryColor"] as? String, material = data["material"] as String, lengthStyle = data["lengthStyle"] as String, waistRise = data["waistRise"] as String, fitStyle = data["fitStyle"] as String, formalityLevel = data["formalityLevel"] as String, imageUrl = imageUrl, code = code))
        CategoryFilter.SHOES -> viewModel.updateShoe(ShoeEntity(id = id, userId = userId, brand = data["brand"] as String, subType = data["subType"] as String, style = data["style"] as String, color = data["color"] as String, material = data["material"] as String, heelHeightStyle = data["heelHeightStyle"] as String, toeStyle = data["toeStyle"] as String, closureType = data["closureType"] as String, formalityLevel = data["formalityLevel"] as String, imageUrl = imageUrl, code = code))
        CategoryFilter.JACKETS -> viewModel.updateJacket(JacketEntity(id = id, userId = userId, brand = data["brand"] as? String, color = data["color"] as String, type = data["type"] as String, closureType = data["closureType"] as String, imageUrl = imageUrl, code = code))
        CategoryFilter.FRAGRANCES -> viewModel.updateFragrance(FragranceEntity(id = id, userId = userId, brand = data["brand"] as String, name = data["name"] as String, occasionTag = data["occasion"] as String, profile = data["profile"] as String, imageUrl = imageUrl, code = code))
        CategoryFilter.WATCHES -> viewModel.updateWatch(WatchEntity(id = id, userId = userId, brand = data["brand"] as String, model = data["model"] as String, dialColor = data["dialColor"] as String, strapColor = data["strapColor"] as String, strapMaterial = data["strapMaterial"] as String, imageUrl = imageUrl, code = code))
        CategoryFilter.BAGS -> viewModel.updateBag(BagEntity(id = id, userId = userId, brand = data["brand"] as? String, color = data["color"] as String, style = data["style"] as String, material = data["material"] as String, size = data["size"] as String, imageUrl = imageUrl, code = code))
        CategoryFilter.DRESSES -> viewModel.updateDress(DressEntity(id = id, userId = userId, brand = data["brand"] as? String, color = data["color"] as String, pattern = data["pattern"] as String, length = data["length"] as String, sleeveStyle = data["sleeveStyle"] as String, material = data["material"] as String, imageUrl = imageUrl, code = code))
        CategoryFilter.SKIRTS -> viewModel.updateSkirt(SkirtEntity(id = id, userId = userId, brand = data["brand"] as? String, color = data["color"] as String, pattern = data["pattern"] as String, length = data["length"] as String, style = data["style"] as String, material = data["material"] as String, imageUrl = imageUrl, code = code))
        CategoryFilter.TIES -> viewModel.updateTie(TieEntity(id = id, userId = userId, colorRange = data["color"] as String, pattern = data["pattern"] as String, material = data["material"] as String, widthCms = (data["width"]?.toString()?.toDoubleOrNull() ?: 7.5), imageUrl = imageUrl, code = code))
        else -> {
            if (data["isCustom"] == true) {
                viewModel.updateCustomGarment(CustomGarmentEntity(id = id, categoryId = data["customCategoryId"] as Long, userId = userId, imageUrl = imageUrl, attributeValues = data["customAttributes"] as String))
            }
        }
    }
}

private fun deleteItem(item: Any, viewModel: WardrobeViewModel) {
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
    onResult: (JSONObject, Boolean) -> Unit,
    onRawResult: (String?) -> Unit = {}
) {
    scope.launch {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                val inputStream = context.contentResolver.openInputStream(uri)
                android.graphics.BitmapFactory.decodeStream(inputStream)
            }.copy(Bitmap.Config.ARGB_8888, true)

            // Extracción de color LOCAL
            val palette = Palette.from(bitmap).generate()
            val dominantColorArgb = palette.getDominantColor(0)
            val dominantColorName = if (dominantColorArgb != 0) {
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

            val maxDim = 800
            val (newW, newH) = if (bitmap.width > bitmap.height) {
                maxDim to (maxDim * bitmap.height / bitmap.width)
            } else {
                (maxDim * bitmap.width / bitmap.height) to maxDim
            }
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true)

            android.util.Log.d("WardrobeScreen", "Starting AI analysis...")
            val aiResult = viewModel.analyzeGarment(scaledBitmap, categoryName, customAttributes)
            onRawResult(aiResult)
            
            if (!aiResult.isNullOrBlank() && !aiResult.contains("Error")) {
                try {
                    onResult(JSONObject(aiResult), false)
                } catch (e: Exception) {
                    android.util.Log.e("WardrobeScreen", "Error parsing AI JSON response: $aiResult", e)
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Respuesta IA no válida: $aiResult", Toast.LENGTH_LONG).show()
                    }
                    val offlineJson = JSONObject().apply { put("color", dominantColorName) }
                    onResult(offlineJson, true)
                }
            } else {
                scope.launch(Dispatchers.Main) {
                    val errorMsg = aiResult ?: "Sin respuesta del servidor"
                    Toast.makeText(context, "IA Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
                val offlineJson = JSONObject().apply { put("color", dominantColorName) }
                onResult(offlineJson, true)
            }
        } catch (e: Exception) {
            android.util.Log.e("WardrobeScreen", "Error in analyzeImage: ${e.message}", e)
            val offlineJson = JSONObject().apply { put("color", "") }
            onResult(offlineJson, true)
        }
    }
}

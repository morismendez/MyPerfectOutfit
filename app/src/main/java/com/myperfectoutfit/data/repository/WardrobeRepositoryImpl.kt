package com.myperfectoutfit.data.repository

import com.myperfectoutfit.data.local.AppDatabase
import com.myperfectoutfit.data.local.dao.OutfitWithDetails
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.local.enums.LaundryState
import com.myperfectoutfit.data.local.backup.BackupManager
import com.myperfectoutfit.data.local.security.SecurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WardrobeRepositoryImpl(
    private val db: AppDatabase,
    private val securePrefs: SecurePreferences,
    private val backupManager: BackupManager
) : WardrobeRepository {

    private val userDao = db.userDao()
    private val shirtDao = db.shirtDao()
    private val pantDao = db.pantDao()
    private val tieDao = db.tieDao()
    private val shoeDao = db.shoeDao()
    private val watchDao = db.watchDao()
    private val fragranceDao = db.fragranceDao()
    private val jacketDao = db.jacketDao()
    private val outfitDao = db.outfitDao()
    private val historyDao = db.historyDao()
    private val bagDao = db.bagDao()
    private val dressDao = db.dressDao()
    private val skirtDao = db.skirtDao()
    private val customDao = db.customDao()
    private val ruleDao = db.ruleDao()

    // --- Usuario ---
    override fun getPrimaryUser(): Flow<UserEntity?> = userDao.getPrimaryUser()

    override suspend fun saveUser(user: UserEntity): Long = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    // --- Seguridad (API Key) ---
    override fun getGeminiApiKey(): String? = securePrefs.getGeminiApiKey()
    override fun saveGeminiApiKey(apiKey: String) = securePrefs.saveGeminiApiKey(apiKey)
    override fun clearGeminiApiKey() = securePrefs.clearGeminiApiKey()

    override suspend fun createBackup(): java.io.File? = backupManager.createBackupZip()
    override suspend fun restoreBackup(uri: android.net.Uri): Boolean = backupManager.restoreFromZip(uri)

    // --- Consultas de Disponibilidad (Flows) ---
    override fun getCleanShirts(userId: Long): Flow<List<ShirtEntity>> = shirtDao.getCleanShirts(userId)
    override fun getCleanPants(userId: Long): Flow<List<PantEntity>> = pantDao.getCleanPants(userId)
    override fun getCleanTies(userId: Long): Flow<List<TieEntity>> = tieDao.getCleanTies(userId)
    override fun getAvailableShoes(userId: Long): Flow<List<ShoeEntity>> = shoeDao.getAvailableShoes(userId)
    override fun getAvailableWatches(userId: Long): Flow<List<WatchEntity>> = watchDao.getAvailableWatches(userId)
    override fun getAvailableFragrances(userId: Long): Flow<List<FragranceEntity>> = fragranceDao.getAvailableFragrances(userId)
    override fun getCleanJackets(userId: Long): Flow<List<JacketEntity>> = jacketDao.getCleanJackets(userId)
    override fun getAvailableBags(userId: Long): Flow<List<BagEntity>> = bagDao.getAvailableBags(userId)
    override fun getCleanDresses(userId: Long): Flow<List<DressEntity>> = dressDao.getCleanDresses(userId)
    override fun getCleanSkirts(userId: Long): Flow<List<SkirtEntity>> = skirtDao.getCleanSkirts(userId)

    // --- Inventario Completo ---
    override fun getAllShirts(userId: Long): Flow<List<ShirtEntity>> = shirtDao.getAllShirts(userId)
    override fun getAllPants(userId: Long): Flow<List<PantEntity>> = pantDao.getAllPants(userId)
    override fun getAllTies(userId: Long): Flow<List<TieEntity>> = tieDao.getAllTies(userId)
    override fun getAllWatches(userId: Long): Flow<List<WatchEntity>> = watchDao.getAllWatches(userId)
    override fun getAllJackets(userId: Long): Flow<List<JacketEntity>> = jacketDao.getAllJackets(userId)
    override fun getAllShoes(userId: Long): Flow<List<ShoeEntity>> = shoeDao.getAllShoes(userId)
    override fun getAllFragrances(userId: Long): Flow<List<FragranceEntity>> = fragranceDao.getAllFragrances(userId)
    override fun getAllBags(userId: Long): Flow<List<BagEntity>> = bagDao.getAllBags(userId)
    override fun getAllDresses(userId: Long): Flow<List<DressEntity>> = dressDao.getAllDresses(userId)
    override fun getAllSkirts(userId: Long): Flow<List<SkirtEntity>> = skirtDao.getAllSkirts(userId)

    override fun getCustomCategories(userId: Long): Flow<List<CustomCategoryEntity>> = customDao.getCategories(userId)
    override suspend fun insertCustomCategory(category: CustomCategoryEntity): Long = withContext(Dispatchers.IO) {
        customDao.insertCategory(category)
    }
    override suspend fun updateCustomCategory(category: CustomCategoryEntity) = withContext(Dispatchers.IO) {
        customDao.updateCategory(category)
    }
    override suspend fun deleteCustomCategory(category: CustomCategoryEntity) = withContext(Dispatchers.IO) {
        customDao.deleteCategory(category)
    }

    override fun getAllCustomGarments(userId: Long): Flow<List<CustomGarmentEntity>> = customDao.getAllCustomGarments(userId)
    override suspend fun insertCustomGarment(garment: CustomGarmentEntity): Long = withContext(Dispatchers.IO) {
        customDao.insertGarment(garment)
    }
    override suspend fun updateCustomGarment(garment: CustomGarmentEntity) = withContext(Dispatchers.IO) {
        customDao.updateGarment(garment)
    }
    override suspend fun deleteCustomGarment(garment: CustomGarmentEntity) = withContext(Dispatchers.IO) {
        customDao.deleteGarment(garment)
    }

    override fun getStyleRules(userId: Long): Flow<List<StyleRuleEntity>> = ruleDao.getRules(userId)
    override fun getActiveStyleRules(userId: Long): Flow<List<StyleRuleEntity>> = ruleDao.getActiveRules(userId)
    override suspend fun insertStyleRule(rule: StyleRuleEntity): Long = withContext(Dispatchers.IO) {
        ruleDao.insertRule(rule)
    }
    override suspend fun updateStyleRule(rule: StyleRuleEntity) = withContext(Dispatchers.IO) {
        ruleDao.updateRule(rule)
    }
    override suspend fun deleteStyleRule(rule: StyleRuleEntity) = withContext(Dispatchers.IO) {
        ruleDao.deleteRule(rule)
    }
    override suspend fun toggleStyleRule(ruleId: Long, isActive: Boolean) = withContext(Dispatchers.IO) {
        ruleDao.toggleRule(ruleId, isActive)
    }

    // --- Actualización de Lavandería / Disponibilidad ---
    override suspend fun updateShirtState(shirtId: Long, state: LaundryState) = withContext(Dispatchers.IO) {
        shirtDao.updateLaundryState(shirtId, state)
    }

    override suspend fun updatePantState(pantId: Long, state: LaundryState) = withContext(Dispatchers.IO) {
        pantDao.updateLaundryState(pantId, state)
    }

    override suspend fun updateTieState(tieId: Long, state: LaundryState) = withContext(Dispatchers.IO) {
        tieDao.updateLaundryState(tieId, state)
    }

    override suspend fun updateJacketState(jacketId: Long, state: LaundryState) = withContext(Dispatchers.IO) {
        jacketDao.updateLaundryState(jacketId, state)
    }

    override suspend fun updateDressState(dressId: Long, state: LaundryState) = withContext(Dispatchers.IO) {
        dressDao.updateLaundryState(dressId, state)
    }

    override suspend fun updateSkirtState(skirtId: Long, state: LaundryState) = withContext(Dispatchers.IO) {
        skirtDao.updateLaundryState(skirtId, state)
    }

    override suspend fun sendShirtsToLaundry(shirtIds: List<Long>) = withContext(Dispatchers.IO) {
        shirtDao.sendToLaundry(shirtIds)
    }

    override suspend fun updateShoeAvailability(shoeId: Long, isAvailable: Boolean) = withContext(Dispatchers.IO) {
        shoeDao.updateAvailability(shoeId, isAvailable)
    }

    override suspend fun updateWatchAvailability(watchId: Long, isAvailable: Boolean) = withContext(Dispatchers.IO) {
        watchDao.updateAvailability(watchId, isAvailable)
    }

    override suspend fun updateBagAvailability(bagId: Long, isAvailable: Boolean) = withContext(Dispatchers.IO) {
        bagDao.updateAvailability(bagId, isAvailable)
    }

    // --- Inserciones Masivas ---
    override suspend fun insertShirts(shirts: List<ShirtEntity>) = withContext(Dispatchers.IO) {
        shirtDao.insertShirts(shirts)
    }

    override suspend fun insertPants(pants: List<PantEntity>) = withContext(Dispatchers.IO) {
        pantDao.insertPants(pants)
    }

    override suspend fun insertTies(ties: List<TieEntity>) = withContext(Dispatchers.IO) {
        tieDao.insertTies(ties)
    }

    override suspend fun insertShoes(shoes: List<ShoeEntity>) = withContext(Dispatchers.IO) {
        shoeDao.insertShoes(shoes)
    }

    override suspend fun insertWatches(watches: List<WatchEntity>) = withContext(Dispatchers.IO) {
        watchDao.insertWatches(watches)
    }

    override suspend fun insertFragrances(fragrances: List<FragranceEntity>) = withContext(Dispatchers.IO) {
        fragranceDao.insertFragrances(fragrances)
    }

    override suspend fun insertJackets(jackets: List<JacketEntity>) = withContext(Dispatchers.IO) {
        jacketDao.insertJackets(jackets)
    }

    override suspend fun insertBags(bags: List<BagEntity>) = withContext(Dispatchers.IO) {
        bagDao.insertBags(bags)
    }

    override suspend fun insertDresses(dresses: List<DressEntity>) = withContext(Dispatchers.IO) {
        dressDao.insertDresses(dresses)
    }

    override suspend fun insertSkirts(skirts: List<SkirtEntity>) = withContext(Dispatchers.IO) {
        skirtDao.insertSkirts(skirts)
    }

    // --- Actualizaciones (Updates) ---
    override suspend fun updateShirt(shirt: ShirtEntity) = withContext(Dispatchers.IO) {
        shirtDao.updateShirt(shirt)
    }

    override suspend fun updatePant(pant: PantEntity) = withContext(Dispatchers.IO) {
        pantDao.updatePant(pant)
    }

    override suspend fun updateShoe(shoe: ShoeEntity) = withContext(Dispatchers.IO) {
        shoeDao.updateShoe(shoe)
    }

    override suspend fun updateTie(tie: TieEntity) = withContext(Dispatchers.IO) {
        tieDao.updateTie(tie)
    }

    override suspend fun updateWatch(watch: WatchEntity) = withContext(Dispatchers.IO) {
        watchDao.updateWatch(watch)
    }

    override suspend fun updateFragrance(fragrance: FragranceEntity) = withContext(Dispatchers.IO) {
        fragranceDao.updateFragrance(fragrance)
    }

    override suspend fun updateJacket(jacket: JacketEntity) = withContext(Dispatchers.IO) {
        jacketDao.updateJacket(jacket)
    }

    override suspend fun updateBag(bag: BagEntity) = withContext(Dispatchers.IO) {
        bagDao.updateBag(bag)
    }

    override suspend fun updateDress(dress: DressEntity) = withContext(Dispatchers.IO) {
        dressDao.updateDress(dress)
    }

    override suspend fun updateSkirt(skirt: SkirtEntity) = withContext(Dispatchers.IO) {
        skirtDao.updateSkirt(skirt)
    }

    // --- Outfits Diarios ---
    override suspend fun saveOutfit(outfit: DailyOutfitEntity) = withContext(Dispatchers.IO) {
        outfitDao.insertOutfit(outfit)
    }

    override suspend fun getOutfitByDate(date: String, userId: Long): OutfitWithDetails? = withContext(Dispatchers.IO) {
        outfitDao.getOutfitByDate(date, userId)
    }

    override fun getOutfitHistory(userId: Long): Flow<List<OutfitWithDetails>> = outfitDao.getOutfitHistory(userId)

    override suspend fun deleteOutfitByDate(date: String, userId: Long) = withContext(Dispatchers.IO) {
        outfitDao.deleteOutfitByDate(date, userId)
    }

    override suspend fun deleteShirt(shirt: ShirtEntity) = withContext(Dispatchers.IO) {
        shirtDao.deleteShirt(shirt)
    }

    override suspend fun deletePant(pant: PantEntity) = withContext(Dispatchers.IO) {
        pantDao.deletePant(pant)
    }

    override suspend fun deleteShoe(shoe: ShoeEntity) = withContext(Dispatchers.IO) {
        shoeDao.deleteShoe(shoe)
    }

    override suspend fun deleteTie(tie: TieEntity) = withContext(Dispatchers.IO) {
        tieDao.deleteTie(tie)
    }

    override suspend fun deleteWatch(watch: WatchEntity) = withContext(Dispatchers.IO) {
        watchDao.deleteWatch(watch)
    }

    override suspend fun deleteFragrance(fragrance: FragranceEntity) = withContext(Dispatchers.IO) {
        fragranceDao.deleteFragrance(fragrance)
    }

    override suspend fun deleteJacket(jacket: JacketEntity) = withContext(Dispatchers.IO) {
        jacketDao.deleteJacket(jacket)
    }

    override suspend fun deleteBag(bag: BagEntity) = withContext(Dispatchers.IO) {
        bagDao.deleteBag(bag)
    }

    override suspend fun deleteDress(dress: DressEntity) = withContext(Dispatchers.IO) {
        dressDao.deleteDress(dress)
    }

    override suspend fun deleteSkirt(skirt: SkirtEntity) = withContext(Dispatchers.IO) {
        skirtDao.deleteSkirt(skirt)
    }

    override suspend fun confirmAndWearOutfit(
        userId: Long,
        shirt: ShirtEntity?,
        pant: PantEntity?,
        shoe: ShoeEntity?,
        tie: TieEntity?,
        watch: WatchEntity?,
        fragrance: FragranceEntity?,
        jacket: JacketEntity?,
        bag: BagEntity?,
        dress: DressEntity?,
        skirt: SkirtEntity?,
        customGarments: List<CustomGarmentEntity>,
        summary: String
    ) = withContext(Dispatchers.IO) {
        // 1. Marcar prendas usadas como EN LAVANDERÍA
        shirt?.let { shirtDao.updateLaundryState(it.id, LaundryState.IN_LAUNDRY) }
        pant?.let { pantDao.updateLaundryState(it.id, LaundryState.IN_LAUNDRY) }
        tie?.let { tieDao.updateLaundryState(it.id, LaundryState.IN_LAUNDRY) }
        jacket?.let { jacketDao.updateLaundryState(it.id, LaundryState.IN_LAUNDRY) }
        dress?.let { dressDao.updateLaundryState(it.id, LaundryState.IN_LAUNDRY) }
        skirt?.let { skirtDao.updateLaundryState(it.id, LaundryState.IN_LAUNDRY) }
        bag?.let { bagDao.updateAvailability(it.id, false) }
        
        customGarments.forEach { garment ->
            val category = customDao.getCategoryById(garment.categoryId)
            if (category?.needsLaundry == true) {
                customDao.updateLaundryState(garment.id, LaundryState.IN_LAUNDRY.name)
            } else {
                customDao.updateAvailability(garment.id, false)
            }
        }

        // 2. Registrar en el historial
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        historyDao.insertHistory(
            OutfitHistoryEntity(
                userId = userId,
                dateString = today,
                shirtId = shirt?.id,
                pantId = pant?.id,
                shoeId = shoe?.id,
                tieId = tie?.id,
                watchId = watch?.id,
                fragranceId = fragrance?.id,
                summaryText = summary
            )
        )
    }
}

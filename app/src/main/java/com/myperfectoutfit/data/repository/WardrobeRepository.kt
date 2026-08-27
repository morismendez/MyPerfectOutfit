package com.myperfectoutfit.data.repository

import com.myperfectoutfit.data.local.dao.OutfitWithDetails
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.local.enums.LaundryState
import kotlinx.coroutines.flow.Flow

interface WardrobeRepository {

    // --- Usuario ---
    fun getPrimaryUser(): Flow<UserEntity?>
    suspend fun saveUser(user: UserEntity): Long
    
    // --- Seguridad (API Key) ---
    fun getGeminiApiKey(): String?
    fun saveGeminiApiKey(apiKey: String)
    fun clearGeminiApiKey()

    // --- Respaldo y Restauración ---
    suspend fun createBackup(): java.io.File?
    suspend fun restoreBackup(uri: android.net.Uri): Boolean

    // --- Consultas de Disponibilidad en Tiempo Real (Flows) ---
    fun getCleanShirts(userId: Long): Flow<List<ShirtEntity>>
    fun getCleanPants(userId: Long): Flow<List<PantEntity>>
    fun getCleanTies(userId: Long): Flow<List<TieEntity>>
    fun getAvailableShoes(userId: Long): Flow<List<ShoeEntity>>
    fun getAvailableWatches(userId: Long): Flow<List<WatchEntity>>
    fun getAvailableFragrances(userId: Long): Flow<List<FragranceEntity>>
    fun getCleanJackets(userId: Long): Flow<List<JacketEntity>>
    fun getAvailableBags(userId: Long): Flow<List<BagEntity>>
    fun getCleanDresses(userId: Long): Flow<List<DressEntity>>
    fun getCleanSkirts(userId: Long): Flow<List<SkirtEntity>>

    // --- Inventario Completo ---
    fun getAllShirts(userId: Long): Flow<List<ShirtEntity>>
    fun getAllShoes(userId: Long): Flow<List<ShoeEntity>>
    fun getAllWatches(userId: Long): Flow<List<WatchEntity>>
    fun getAllJackets(userId: Long): Flow<List<JacketEntity>>
    fun getAllPants(userId: Long): Flow<List<PantEntity>>
    fun getAllTies(userId: Long): Flow<List<TieEntity>>
    fun getAllFragrances(userId: Long): Flow<List<FragranceEntity>>
    fun getAllBags(userId: Long): Flow<List<BagEntity>>
    fun getAllDresses(userId: Long): Flow<List<DressEntity>>
    fun getAllSkirts(userId: Long): Flow<List<SkirtEntity>>
    
    // --- Categorías Personalizadas ---
    fun getCustomCategories(userId: Long): Flow<List<CustomCategoryEntity>>
    suspend fun insertCustomCategory(category: CustomCategoryEntity): Long
    suspend fun updateCustomCategory(category: CustomCategoryEntity)
    suspend fun deleteCustomCategory(category: CustomCategoryEntity)
    
    fun getAllCustomGarments(userId: Long): Flow<List<CustomGarmentEntity>>
    suspend fun insertCustomGarment(garment: CustomGarmentEntity): Long
    suspend fun updateCustomGarment(garment: CustomGarmentEntity)
    suspend fun deleteCustomGarment(garment: CustomGarmentEntity)

    // --- Reglas de Estilo ---
    fun getStyleRules(userId: Long): Flow<List<StyleRuleEntity>>
    fun getActiveStyleRules(userId: Long): Flow<List<StyleRuleEntity>>
    suspend fun insertStyleRule(rule: StyleRuleEntity): Long
    suspend fun updateStyleRule(rule: StyleRuleEntity)
    suspend fun deleteStyleRule(rule: StyleRuleEntity)
    suspend fun toggleStyleRule(ruleId: Long, isActive: Boolean)

    // --- Actualización de Lavandería y Mantenimiento ---
    suspend fun updateShirtState(shirtId: Long, state: LaundryState)
    suspend fun updatePantState(pantId: Long, state: LaundryState)
    suspend fun updateTieState(tieId: Long, state: LaundryState)
    suspend fun updateJacketState(jacketId: Long, state: LaundryState)
    suspend fun updateDressState(dressId: Long, state: LaundryState)
    suspend fun updateSkirtState(skirtId: Long, state: LaundryState)
    suspend fun sendShirtsToLaundry(shirtIds: List<Long>)
    suspend fun updateShoeAvailability(shoeId: Long, isAvailable: Boolean)
    suspend fun updateWatchAvailability(watchId: Long, isAvailable: Boolean)
    suspend fun updateBagAvailability(bagId: Long, isAvailable: Boolean)

    // --- Inserciones e Inventariado ---
    suspend fun insertShirts(shirts: List<ShirtEntity>)
    suspend fun insertPants(pants: List<PantEntity>)
    suspend fun insertTies(ties: List<TieEntity>)
    suspend fun insertShoes(shoes: List<ShoeEntity>)
    suspend fun insertWatches(watches: List<WatchEntity>)
    suspend fun insertFragrances(fragrances: List<FragranceEntity>)
    suspend fun insertJackets(jackets: List<JacketEntity>)
    suspend fun insertBags(bags: List<BagEntity>)
    suspend fun insertDresses(dresses: List<DressEntity>)
    suspend fun insertSkirts(skirts: List<SkirtEntity>)

    // --- Actualizaciones (Updates) ---
    suspend fun updateShirt(shirt: ShirtEntity)
    suspend fun updatePant(pant: PantEntity)
    suspend fun updateShoe(shoe: ShoeEntity)
    suspend fun updateTie(tie: TieEntity)
    suspend fun updateWatch(watch: WatchEntity)
    suspend fun updateFragrance(fragrance: FragranceEntity)
    suspend fun updateJacket(jacket: JacketEntity)
    suspend fun updateBag(bag: BagEntity)
    suspend fun updateDress(dress: DressEntity)
    suspend fun updateSkirt(skirt: SkirtEntity)

    // --- Outfits Diarios ---
    suspend fun saveOutfit(outfit: DailyOutfitEntity)
    suspend fun getOutfitByDate(date: String, userId: Long): OutfitWithDetails?
    fun getOutfitHistory(userId: Long): Flow<List<OutfitWithDetails>>
    suspend fun deleteOutfitByDate(date: String, userId: Long)

    //DELETES
    suspend fun deleteShirt(shirt: ShirtEntity)
     suspend fun deletePant(pant: PantEntity)
     suspend fun deleteShoe(shoe: ShoeEntity)
     suspend fun deleteTie(tie: TieEntity)
     suspend fun deleteWatch(watch: WatchEntity)
     suspend fun deleteFragrance(fragrance: FragranceEntity)
     suspend fun deleteJacket(jacket: JacketEntity)
     suspend fun deleteBag(bag: BagEntity)
     suspend fun deleteDress(dress: DressEntity)
     suspend fun deleteSkirt(skirt: SkirtEntity)

    suspend fun confirmAndWearOutfit(
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
        customGarments: List<CustomGarmentEntity> = emptyList(),
        summary: String
    )
}

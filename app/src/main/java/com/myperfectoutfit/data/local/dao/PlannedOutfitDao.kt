package com.myperfectoutfit.data.local.dao

import androidx.room.*
import com.myperfectoutfit.data.local.entities.*
import kotlinx.coroutines.flow.Flow

data class PlannedOutfitWithDetails(
    @Embedded val plannedOutfit: PlannedOutfitEntity,

    @Relation(parentColumn = "shirtId", entityColumn = "id")
    val shirt: ShirtEntity?,

    @Relation(parentColumn = "pantId", entityColumn = "id")
    val pant: PantEntity?,

    @Relation(parentColumn = "tieId", entityColumn = "id")
    val tie: TieEntity?,

    @Relation(parentColumn = "shoeId", entityColumn = "id")
    val shoe: ShoeEntity?,

    @Relation(parentColumn = "jacketId", entityColumn = "id")
    val jacket: JacketEntity?,

    @Relation(parentColumn = "watchId", entityColumn = "id")
    val watch: WatchEntity?,

    @Relation(parentColumn = "fragranceId", entityColumn = "id")
    val fragrance: FragranceEntity?,

    @Relation(parentColumn = "bagId", entityColumn = "id")
    val bag: BagEntity?,

    @Relation(parentColumn = "dressId", entityColumn = "id")
    val dress: DressEntity?,

    @Relation(parentColumn = "skirtId", entityColumn = "id")
    val skirt: SkirtEntity?
)

@Dao
interface PlannedOutfitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedOutfit(plannedOutfit: PlannedOutfitEntity): Long

    @Update
    suspend fun updatePlannedOutfit(plannedOutfit: PlannedOutfitEntity)

    @Delete
    suspend fun deletePlannedOutfit(plannedOutfit: PlannedOutfitEntity)

    @Transaction
    @Query("SELECT * FROM planned_outfits WHERE user_id = :userId AND dateString >= :startDate AND dateString <= :endDate ORDER BY dateString ASC")
    fun getPlannedOutfitsInRange(userId: Long, startDate: String, endDate: String): Flow<List<PlannedOutfitWithDetails>>

    @Transaction
    @Query("SELECT * FROM planned_outfits WHERE user_id = :userId AND dateString = :date LIMIT 1")
    fun getPlannedOutfitByDate(userId: Long, date: String): Flow<PlannedOutfitWithDetails?>
    
    @Query("DELETE FROM planned_outfits WHERE user_id = :userId AND dateString = :date")
    suspend fun deletePlannedOutfitByDate(userId: Long, date: String)
}

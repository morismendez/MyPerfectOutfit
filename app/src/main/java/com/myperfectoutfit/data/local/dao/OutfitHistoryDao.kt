package com.myperfectoutfit.data.local.dao

import androidx.room.*
import com.myperfectoutfit.data.local.entities.*
import kotlinx.coroutines.flow.Flow

data class HistoryWithDetails(
    @Embedded val history: OutfitHistoryEntity,

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
interface OutfitHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: OutfitHistoryEntity)

    @Transaction
    @Query("SELECT * FROM outfit_history WHERE userId = :userId ORDER BY dateString DESC, id DESC")
    fun getFullHistory(userId: Long): Flow<List<HistoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM outfit_history WHERE userId = :userId AND dateString = :date")
    fun getHistoryByDate(userId: Long, date: String): Flow<List<HistoryWithDetails>>

    @Query("SELECT * FROM outfit_history WHERE userId = :userId ORDER BY id DESC LIMIT 10")
    fun getRecentHistory(userId: Long): Flow<List<OutfitHistoryEntity>>
}

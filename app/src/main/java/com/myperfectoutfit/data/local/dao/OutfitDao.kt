package com.myperfectoutfit.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.myperfectoutfit.data.local.entities.*
import kotlinx.coroutines.flow.Flow

// Objeto de unión que Room procesa de forma transparente
data class OutfitWithDetails(
    @Embedded val outfit: DailyOutfitEntity,

    @Relation(parentColumn = "shirt_id", entityColumn = "id")
    val shirt: ShirtEntity?,

    @Relation(parentColumn = "pant_id", entityColumn = "id")
    val pant: PantEntity?,

    @Relation(parentColumn = "tie_id", entityColumn = "id")
    val tie: TieEntity?,

    @Relation(parentColumn = "shoe_id", entityColumn = "id")
    val shoe: ShoeEntity?,

    @Relation(parentColumn = "jacket_id", entityColumn = "id")
    val jacket: JacketEntity?,

    @Relation(parentColumn = "watch_id", entityColumn = "id")
    val watch: WatchEntity?,

    @Relation(parentColumn = "fragrance_id", entityColumn = "id")
    val fragrance: FragranceEntity?
)

@Dao
interface OutfitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfit(outfit: DailyOutfitEntity)

    // Recupera el outfit de una fecha específica con los detalles de cada prenda
    @Transaction
    @Query("SELECT * FROM daily_outfits WHERE date = :date AND user_id = :userId LIMIT 1")
    suspend fun getOutfitByDate(date: String, userId: Long): OutfitWithDetails?

    // Obtiene el historial completo de combinaciones ordenado del más reciente al más antiguo
    @Transaction
    @Query("SELECT * FROM daily_outfits WHERE user_id = :userId ORDER BY date DESC")
    fun getOutfitHistory(userId: Long): Flow<List<OutfitWithDetails>>

    @Query("DELETE FROM daily_outfits WHERE date = :date AND user_id = :userId")
    suspend fun deleteOutfitByDate(date: String, userId: Long)
}
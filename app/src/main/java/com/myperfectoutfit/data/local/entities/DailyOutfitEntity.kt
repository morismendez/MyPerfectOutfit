package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_outfits",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["user_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ShirtEntity::class, parentColumns = ["id"], childColumns = ["shirt_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = PantEntity::class, parentColumns = ["id"], childColumns = ["pant_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = TieEntity::class, parentColumns = ["id"], childColumns = ["tie_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ShoeEntity::class, parentColumns = ["id"], childColumns = ["shoe_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = JacketEntity::class, parentColumns = ["id"], childColumns = ["jacket_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = WatchEntity::class, parentColumns = ["id"], childColumns = ["watch_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = FragranceEntity::class, parentColumns = ["id"], childColumns = ["fragrance_id"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class DailyOutfitEntity(
    @PrimaryKey val date: String,  // Formato "YYYY-MM-DD"
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "shirt_id") val shirtId: Long?,
    @ColumnInfo(name = "pant_id") val pantId: Long?,
    @ColumnInfo(name = "tie_id") val tieId: Long?,
    @ColumnInfo(name = "shoe_id") val shoeId: Long?,
    @ColumnInfo(name = "jacket_id") val jacketId: Long?,
    @ColumnInfo(name = "watch_id") val watchId: Long?,
    @ColumnInfo(name = "fragrance_id") val fragranceId: Long?,
    val notes: String? = null
)
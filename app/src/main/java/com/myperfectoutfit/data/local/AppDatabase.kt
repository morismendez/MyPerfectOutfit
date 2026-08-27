package com.myperfectoutfit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.myperfectoutfit.data.local.converters.Converters
import com.myperfectoutfit.data.local.dao.*
import com.myperfectoutfit.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        ShirtEntity::class,
        PantEntity::class,
        TieEntity::class,
        ShoeEntity::class,
        WatchEntity::class,
        FragranceEntity::class,
        JacketEntity::class,
        OutfitHistoryEntity::class,
        BagEntity::class,
        DressEntity::class,
        SkirtEntity::class,
        CustomCategoryEntity::class,
        CustomGarmentEntity::class,
        StyleRuleEntity::class
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // DAOs
    abstract fun userDao(): UserDao
    abstract fun shirtDao(): ShirtDao
    abstract fun pantDao(): PantDao
    abstract fun tieDao(): TieDao
    abstract fun shoeDao(): ShoeDao
    abstract fun watchDao(): WatchDao
    abstract fun fragranceDao(): FragranceDao
    abstract fun jacketDao(): JacketDao
    abstract fun historyDao(): OutfitHistoryDao
    abstract fun bagDao(): BagDao
    abstract fun dressDao(): DressDao
    abstract fun skirtDao(): SkirtDao
    abstract fun customDao(): CustomCategoryDao
    abstract fun ruleDao(): StyleRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // --- MIGRACIONES ROBUSTAS ---
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `outfit_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `dateString` TEXT NOT NULL, `shirtId` INTEGER, `pantId` INTEGER, `shoeId` INTEGER, `tieId` INTEGER, `watchId` INTEGER, `fragranceId` INTEGER, `summaryText` TEXT NOT NULL)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `bags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `code` TEXT NOT NULL, `brand` TEXT, `color` TEXT NOT NULL, `style` TEXT NOT NULL, `material` TEXT NOT NULL, `size` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, `isAvailable` INTEGER NOT NULL, FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE TABLE IF NOT EXISTS `dresses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `code` TEXT NOT NULL, `brand` TEXT, `color` TEXT NOT NULL, `pattern` TEXT NOT NULL, `length` TEXT NOT NULL, `sleeveStyle` TEXT NOT NULL, `material` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, `laundryState` TEXT NOT NULL, FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE TABLE IF NOT EXISTS `skirts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `code` TEXT NOT NULL, `brand` TEXT, `color` TEXT NOT NULL, `pattern` TEXT NOT NULL, `length` TEXT NOT NULL, `style` TEXT NOT NULL, `material` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, `laundryState` TEXT NOT NULL, FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `custom_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `attributeNames` TEXT NOT NULL, `needsLaundry` INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE TABLE IF NOT EXISTS `custom_garments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category_id` INTEGER NOT NULL, `user_id` INTEGER NOT NULL, `imageUrl` TEXT NOT NULL, `laundryState` TEXT NOT NULL, `isAvailable` INTEGER NOT NULL, `attributeValues` TEXT NOT NULL, FOREIGN KEY(`category_id`) REFERENCES `custom_categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `style_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `isActive` INTEGER NOT NULL, FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }

        val MIGRATION_5_11 = object : Migration(5, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `shirts` ")
                db.execSQL("CREATE TABLE IF NOT EXISTS `shirts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `code` TEXT NOT NULL, `brand` TEXT NOT NULL, `subType` TEXT NOT NULL, `primaryColor` TEXT NOT NULL, `secondaryColor` TEXT, `pattern` TEXT NOT NULL, `sleeveLength` TEXT NOT NULL, `necklineStyle` TEXT NOT NULL, `material` TEXT NOT NULL, `formalityLevel` TEXT NOT NULL, `fit` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, `laundryState` TEXT NOT NULL, FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                
                db.execSQL("DROP TABLE IF EXISTS `pants` ")
                db.execSQL("CREATE TABLE IF NOT EXISTS `pants` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `code` TEXT NOT NULL, `brand` TEXT, `subType` TEXT NOT NULL, `primaryColor` TEXT NOT NULL, `secondaryColor` TEXT, `material` TEXT NOT NULL, `lengthStyle` TEXT NOT NULL, `waistRise` TEXT NOT NULL, `fitStyle` TEXT NOT NULL, `formalityLevel` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, `laundryState` TEXT NOT NULL, FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                
                db.execSQL("DROP TABLE IF EXISTS `shoes` ")
                db.execSQL("CREATE TABLE IF NOT EXISTS `shoes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `code` TEXT NOT NULL, `brand` TEXT NOT NULL, `subType` TEXT NOT NULL, `style` TEXT NOT NULL, `color` TEXT NOT NULL, `secondaryColor` TEXT, `material` TEXT NOT NULL, `heelHeightStyle` TEXT NOT NULL, `toeStyle` TEXT NOT NULL, `closureType` TEXT NOT NULL, `formalityLevel` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, `isAvailable` INTEGER NOT NULL, FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `outfit_history` ADD COLUMN `jacketId` INTEGER")
                db.execSQL("ALTER TABLE `outfit_history` ADD COLUMN `bagId` INTEGER")
                db.execSQL("ALTER TABLE `outfit_history` ADD COLUMN `dressId` INTEGER")
                db.execSQL("ALTER TABLE `outfit_history` ADD COLUMN `skirtId` INTEGER")
                db.execSQL("ALTER TABLE `outfit_history` ADD COLUMN `customGarmentIds` TEXT")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `daily_outfits` ")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wardrobe_database"
                )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, 
                    MIGRATION_4_5, MIGRATION_5_11, MIGRATION_11_12, MIGRATION_12_13,
                    object : Migration(9, 11) { override fun migrate(db: SupportSQLiteDatabase) {} },
                    object : Migration(10, 11) { override fun migrate(db: SupportSQLiteDatabase) {} }
                )
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}

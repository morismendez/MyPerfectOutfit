package com.myperfectoutfit.domain.model

import com.myperfectoutfit.data.local.entities.*

sealed class GarmentItem {
    abstract val id: Long
    abstract val imageUrl: String?
    abstract val categoryTag: String
    abstract val formattedDescription: String
    abstract val rawEntity: Any

    data class Shirt(val entity: ShirtEntity) : GarmentItem() {
        override val id get() = entity.id
        override val imageUrl get() = entity.imageUrl
        override val categoryTag get() = "SHIRT"
        override val formattedDescription get() = "Camisa/Top: ${entity.brand ?: ""} ${entity.primaryColor} (${entity.subType})".trim()
        override val rawEntity get() = entity
    }

    data class Pant(val entity: PantEntity) : GarmentItem() {
        override val id get() = entity.id
        override val imageUrl get() = entity.imageUrl
        override val categoryTag get() = "PANT"
        override val formattedDescription get() = "Pantalón: ${entity.brand ?: ""} ${entity.primaryColor} (${entity.subType})".trim()
        override val rawEntity get() = entity
    }

    data class Shoe(val entity: ShoeEntity) : GarmentItem() {
        override val id get() = entity.id
        override val imageUrl get() = entity.imageUrl
        override val categoryTag get() = "SHOE"
        override val formattedDescription get() = "Calzado: ${entity.brand ?: ""} ${entity.color} (${entity.style})".trim()
        override val rawEntity get() = entity
    }

    data class Jacket(val entity: JacketEntity) : GarmentItem() {
        override val id get() = entity.id
        override val imageUrl get() = entity.imageUrl
        override val categoryTag get() = "JACKET"
        override val formattedDescription get() = "Chaqueta: ${entity.brand ?: ""} ${entity.color}".trim()
        override val rawEntity get() = entity
    }

    data class Dress(val entity: DressEntity) : GarmentItem() {
        override val id get() = entity.id
        override val imageUrl get() = entity.imageUrl
        override val categoryTag get() = "DRESS"
        override val formattedDescription get() = "Vestido: ${entity.brand ?: ""} ${entity.color}".trim()
        override val rawEntity get() = entity
    }

    data class Skirt(val entity: SkirtEntity) : GarmentItem() {
        override val id get() = entity.id
        override val imageUrl get() = entity.imageUrl
        override val categoryTag get() = "SKIRT"
        override val formattedDescription get() = "Falda: ${entity.brand ?: ""} ${entity.color}".trim()
        override val rawEntity get() = entity
    }

    data class Tie(val entity: TieEntity) : GarmentItem() {
        override val id get() = entity.id
        override val imageUrl get() = entity.imageUrl
        override val categoryTag get() = "TIE"
        override val formattedDescription get() = "Corbata: ${entity.colorRange} ${entity.pattern}".trim()
        override val rawEntity get() = entity
    }

    data class Watch(val entity: WatchEntity) : GarmentItem() {
        override val id get() = entity.id
        override val imageUrl get() = entity.imageUrl
        override val categoryTag get() = "WATCH"
        override val formattedDescription get() = "Reloj: ${entity.brand} ${entity.model}".trim()
        override val rawEntity get() = entity
    }

    data class Fragrance(val entity: FragranceEntity) : GarmentItem() {
        override val id get() = entity.id
        override val imageUrl get() = entity.imageUrl
        override val categoryTag get() = "FRAGRANCE"
        override val formattedDescription get() = "Fragancia: ${entity.brand} ${entity.name}".trim()
        override val rawEntity get() = entity
    }

    data class Bag(val entity: BagEntity) : GarmentItem() {
        override val id get() = entity.id
        override val imageUrl get() = entity.imageUrl
        override val categoryTag get() = "BAG"
        override val formattedDescription get() = "Bolso: ${entity.brand ?: ""} ${entity.color}".trim()
        override val rawEntity get() = entity
    }

    data class Custom(val entity: CustomGarmentEntity) : GarmentItem() {
        override val id get() = entity.id
        override val imageUrl get() = entity.imageUrl
        override val categoryTag get() = "CUSTOM"
        override val formattedDescription get() = "Prenda Personalizada: ${entity.attributeValues.replace("|", " ")}".trim()
        override val rawEntity get() = entity
    }

    companion object {
        fun fromEntity(entity: Any): GarmentItem? = when (entity) {
            is ShirtEntity -> Shirt(entity)
            is PantEntity -> Pant(entity)
            is ShoeEntity -> Shoe(entity)
            is JacketEntity -> Jacket(entity)
            is DressEntity -> Dress(entity)
            is SkirtEntity -> Skirt(entity)
            is TieEntity -> Tie(entity)
            is WatchEntity -> Watch(entity)
            is FragranceEntity -> Fragrance(entity)
            is BagEntity -> Bag(entity)
            is CustomGarmentEntity -> Custom(entity)
            is GarmentItem -> entity
            else -> null
        }
    }
}

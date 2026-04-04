package com.linknest.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.linknest.core.database.entity.CategoryEntity
import com.linknest.core.database.entity.WebsiteEntryEntity

data class CategoryWithWebsitesEntity(
    @Embedded
    val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "category_id",
    )
    val websites: List<WebsiteEntryEntity>,
)

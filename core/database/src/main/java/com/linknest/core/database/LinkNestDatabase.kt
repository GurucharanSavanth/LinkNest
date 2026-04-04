package com.linknest.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.linknest.core.database.dao.CategoryDao
import com.linknest.core.database.dao.DomainCategoryMappingDao
import com.linknest.core.database.dao.IconCacheDao
import com.linknest.core.database.dao.IntegrityEventDao
import com.linknest.core.database.dao.RecentQueryDao
import com.linknest.core.database.dao.SavedFilterDao
import com.linknest.core.database.dao.TagDao
import com.linknest.core.database.dao.WebsiteDao
import com.linknest.core.database.entity.CategoryEntity
import com.linknest.core.database.entity.DomainCategoryMappingEntity
import com.linknest.core.database.entity.IconCacheEntity
import com.linknest.core.database.entity.IntegrityEventEntity
import com.linknest.core.database.entity.RecentQueryEntity
import com.linknest.core.database.entity.SavedFilterEntity
import com.linknest.core.database.entity.TagEntity
import com.linknest.core.database.entity.WebsiteEntryEntity
import com.linknest.core.database.entity.WebsiteTagCrossRefEntity
import com.linknest.core.database.util.DatabaseConverters

@Database(
    entities = [
        CategoryEntity::class,
        WebsiteEntryEntity::class,
        TagEntity::class,
        WebsiteTagCrossRefEntity::class,
        DomainCategoryMappingEntity::class,
        IconCacheEntity::class,
        SavedFilterEntity::class,
        IntegrityEventEntity::class,
        RecentQueryEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class LinkNestDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun websiteDao(): WebsiteDao
    abstract fun tagDao(): TagDao
    abstract fun domainCategoryMappingDao(): DomainCategoryMappingDao
    abstract fun iconCacheDao(): IconCacheDao
    abstract fun savedFilterDao(): SavedFilterDao
    abstract fun integrityEventDao(): IntegrityEventDao
    abstract fun recentQueryDao(): RecentQueryDao
}

package com.linknest.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.linknest.core.database.DatabaseMigrations
import com.linknest.core.database.LinkNestDatabase
import com.linknest.core.database.dao.CategoryDao
import com.linknest.core.database.dao.DomainCategoryMappingDao
import com.linknest.core.database.dao.IconCacheDao
import com.linknest.core.database.dao.IntegrityEventDao
import com.linknest.core.database.dao.RecentQueryDao
import com.linknest.core.database.dao.SavedFilterDao
import com.linknest.core.database.dao.TagDao
import com.linknest.core.database.dao.WebsiteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): LinkNestDatabase = Room
        .databaseBuilder(context, LinkNestDatabase::class.java, "linknest.db")
        .addMigrations(*DatabaseMigrations.ALL)
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .build()

    @Provides
    fun provideCategoryDao(database: LinkNestDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideWebsiteDao(database: LinkNestDatabase): WebsiteDao = database.websiteDao()

    @Provides
    fun provideTagDao(database: LinkNestDatabase): TagDao = database.tagDao()

    @Provides
    fun provideIconCacheDao(database: LinkNestDatabase): IconCacheDao = database.iconCacheDao()

    @Provides
    fun provideDomainCategoryMappingDao(
        database: LinkNestDatabase,
    ): DomainCategoryMappingDao = database.domainCategoryMappingDao()

    @Provides
    fun provideSavedFilterDao(database: LinkNestDatabase): SavedFilterDao = database.savedFilterDao()

    @Provides
    fun provideIntegrityEventDao(database: LinkNestDatabase): IntegrityEventDao = database.integrityEventDao()

    @Provides
    fun provideRecentQueryDao(database: LinkNestDatabase): RecentQueryDao = database.recentQueryDao()
}

package com.linknest.core.data.di

import com.linknest.core.common.coroutine.DefaultDispatcher
import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.common.time.SystemTimeProvider
import com.linknest.core.common.time.TimeProvider
import com.linknest.core.data.repository.CategoryRepository
import com.linknest.core.data.repository.DomainCategoryMappingRepository
import com.linknest.core.data.repository.BackupRepository
import com.linknest.core.data.repository.AppSearchBackedSearchIndexRepository
import com.linknest.core.data.repository.OfflineFirstCategoryRepository
import com.linknest.core.data.repository.OfflineFirstDomainCategoryMappingRepository
import com.linknest.core.data.repository.OfflineFirstBackupRepository
import com.linknest.core.data.repository.OfflineFirstIntegrityEventRepository
import com.linknest.core.data.repository.OfflineFirstRecentQueryRepository
import com.linknest.core.data.repository.OfflineFirstSavedFilterRepository
import com.linknest.core.data.repository.OfflineFirstTagRepository
import com.linknest.core.data.repository.OfflineFirstUserPreferencesRepository
import com.linknest.core.data.repository.OfflineFirstWebsiteRepository
import com.linknest.core.data.repository.IntegrityEventRepository
import com.linknest.core.data.repository.RecentQueryRepository
import com.linknest.core.data.repository.SavedFilterRepository
import com.linknest.core.data.repository.SearchIndexRepository
import com.linknest.core.data.repository.TagRepository
import com.linknest.core.data.repository.UserPreferencesRepository
import com.linknest.core.data.repository.WebsiteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        repository: OfflineFirstCategoryRepository,
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindWebsiteRepository(
        repository: OfflineFirstWebsiteRepository,
    ): WebsiteRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(
        repository: OfflineFirstTagRepository,
    ): TagRepository

    @Binds
    @Singleton
    abstract fun bindDomainCategoryMappingRepository(
        repository: OfflineFirstDomainCategoryMappingRepository,
    ): DomainCategoryMappingRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        repository: OfflineFirstBackupRepository,
    ): BackupRepository

    @Binds
    @Singleton
    abstract fun bindSavedFilterRepository(
        repository: OfflineFirstSavedFilterRepository,
    ): SavedFilterRepository

    @Binds
    @Singleton
    abstract fun bindRecentQueryRepository(
        repository: OfflineFirstRecentQueryRepository,
    ): RecentQueryRepository

    @Binds
    @Singleton
    abstract fun bindSearchIndexRepository(
        repository: AppSearchBackedSearchIndexRepository,
    ): SearchIndexRepository

    @Binds
    @Singleton
    abstract fun bindIntegrityEventRepository(
        repository: OfflineFirstIntegrityEventRepository,
    ): IntegrityEventRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        repository: OfflineFirstUserPreferencesRepository,
    ): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindTimeProvider(
        timeProvider: SystemTimeProvider,
    ): TimeProvider
}

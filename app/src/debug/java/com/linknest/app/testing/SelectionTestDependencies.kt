package com.linknest.app.testing

import com.linknest.core.action.pipeline.SearchPipeline
import com.linknest.core.action.action.TrackWebsiteOpenAction
import com.linknest.core.data.usecase.*
import com.linknest.core.database.dao.RecentQueryDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug-only constructor dependencies let selection tests isolate recent-query writes. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SelectionTestDependencies {
    fun queries(): RecentQueryDao
    fun search(): SearchPipeline
    fun trackOpen(): TrackWebsiteOpenAction
    fun filters(): LoadSavedFiltersUseCase
    fun saveFilter(): SaveFilterUseCase
    fun deleteFilter(): DeleteSavedFilterUseCase
    fun suggestions(): GetSearchSuggestionsUseCase
    fun selectableCategories(): ObserveSelectableCategoriesUseCase
    fun tags(): ObserveTagsUseCase
    fun websiteItems(): GetAllWebsiteItemsUseCase
    fun applyFilter(): ApplyFilterUseCase
    fun warmIndex(): WarmSearchIndexUseCase
    fun exportData(): ExportDataUseCase
}

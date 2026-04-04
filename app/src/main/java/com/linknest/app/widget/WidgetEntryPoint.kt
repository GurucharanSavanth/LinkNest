package com.linknest.app.widget

import com.linknest.core.data.usecase.GetCategoryWidgetSnapshotUseCase
import com.linknest.core.data.usecase.GetRecentWidgetLinksUseCase
import com.linknest.core.data.usecase.GetWidgetLinksUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getWidgetLinksUseCase(): GetWidgetLinksUseCase
    fun getRecentWidgetLinksUseCase(): GetRecentWidgetLinksUseCase
    fun getCategoryWidgetSnapshotUseCase(): GetCategoryWidgetSnapshotUseCase
}

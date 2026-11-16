package com.linknest.app.testing

import com.linknest.core.data.repository.CategoryRepository
import com.linknest.core.data.repository.WebsiteRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ProfilingEntryPoint {
    fun categories(): CategoryRepository
    fun websites(): WebsiteRepository
}

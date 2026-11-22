package com.linknest.app

import android.os.Bundle
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.core.data.repository.RecentQueryRepository
import com.linknest.core.data.usecase.*
import com.linknest.app.testing.SelectionTestDependencies
import com.linknest.core.model.RecentQuery
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import com.linknest.feature.search.SEARCH_FILTER_ID_ARG
import com.linknest.feature.search.SearchViewModel
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SearchSelectionStateTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val dependencies get() = EntryPointAccessors.fromApplication(
        compose.activity.application, SelectionTestDependencies::class.java,
    )

    @Test fun routeSelection_appliesOnce_andRestoredSelection_reconcilesDeletedFilter() {
        val queries = dependencies.queries()
        // Explicit run-owned UUIDs clean artifacts from earlier test versions without touching other history.
        InstrumentationRegistry.getArguments().getString("cleanupQuerySuffixes")?.split(',')?.forEach { value ->
            val suffix = UUID.fromString(value).toString()
            runBlocking {
                for (prefix in listOf("route criteria", "edited criteria")) {
                    val name = "$prefix $suffix"
                    queries.getByQuery(name)?.let { check(it.query == name); queries.deleteById(it.id) }
                }
            }
        }
        val originalQueries = runBlocking { queries.getAllRecentQueries() }
        val suffix = UUID.randomUUID().toString()
        val ownedNames = setOf("QA route $suffix", "QA unrelated $suffix")
        lateinit var writer: SearchViewModel
        compose.runOnIdle { writer = createViewModel("writer-$suffix", Bundle()) }
        try {
            compose.runOnIdle {
                writer.onQueryChanged("route criteria $suffix")
                writer.onSaveCurrentFilter(ownedNames.first())
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                writer.uiState.value.savedFilters.any { it.name == ownedNames.first() }
            }
            val fixture = writer.uiState.value.savedFilters.single { it.name == ownedNames.first() }
            lateinit var route: SearchViewModel
            compose.runOnIdle {
                route = createViewModel("route-$suffix", Bundle().apply { putLong(SEARCH_FILTER_ID_ARG, fixture.id) })
            }
            compose.waitUntil(timeoutMillis = 5_000) { route.uiState.value.activeSavedFilterId == fixture.id }
            compose.runOnIdle {
                route.onQueryChanged("edited criteria $suffix")
                writer.onSaveCurrentFilter(ownedNames.last())
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                route.uiState.value.savedFilters.any { it.name == ownedNames.last() }
            }
            compose.runOnIdle {
                assertEquals("edited criteria $suffix", route.uiState.value.query)
                assertNull(route.uiState.value.activeSavedFilterId)
            }

            lateinit var restored: SearchViewModel
            compose.runOnIdle {
                restored = createViewModel("restored-$suffix", Bundle().apply {
                    putBundle("searchDraft", Bundle().apply {
                        putString("query", fixture.spec.query)
                        putLong("activeFilter", fixture.id)
                    })
                })
            }
            compose.waitUntil(timeoutMillis = 5_000) { restored.uiState.value.savedFilters.any { it.id == fixture.id } }
            compose.runOnIdle {
                assertEquals(fixture.id, restored.uiState.value.activeSavedFilterId)
                assertEquals(fixture.spec.query, restored.uiState.value.query)
                check(writer.uiState.value.savedFilters.single { it.id == fixture.id }.name == ownedNames.first())
                writer.onDeleteSavedFilter(fixture.id)
            }
            compose.waitUntil(timeoutMillis = 5_000) { restored.uiState.value.activeSavedFilterId == null }
            compose.runOnIdle { assertEquals(fixture.spec.query, restored.uiState.value.query) }
        } finally {
            val owned = writer.uiState.value.savedFilters.filter { it.name in ownedNames }
            compose.runOnIdle {
                owned.forEach { fixture ->
                    check(writer.uiState.value.savedFilters.single { it.id == fixture.id }.name == fixture.name)
                    writer.onDeleteSavedFilter(fixture.id)
                }
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                writer.uiState.value.savedFilters.none { it.name in ownedNames }
            }
            assertEquals(originalQueries, runBlocking { queries.getAllRecentQueries() })
        }
    }

    private fun createViewModel(key: String, args: Bundle): SearchViewModel {
        val activity = compose.activity
        val recentQueries = object : RecentQueryRepository {
            override fun observeRecentQueries(limit: Int) = flowOf(emptyList<RecentQuery>())
            override suspend fun getRecentQueries(limit: Int) = emptyList<RecentQuery>()
            override suspend fun saveQuery(query: String) = Unit
            override suspend fun clearRecentQueries() = Unit
        }
        val state = SavedStateHandle(buildMap<String, Any?> {
            if (args.containsKey(SEARCH_FILTER_ID_ARG)) put(SEARCH_FILTER_ID_ARG, args.getLong(SEARCH_FILTER_ID_ARG))
            args.getBundle("searchDraft")?.let { put("searchDraft", it) }
        })
        val entry = dependencies
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = modelClass.cast(SearchViewModel(
                state, entry.search(), entry.trackOpen(), entry.filters(), entry.saveFilter(), entry.deleteFilter(),
                ObserveRecentQueriesUseCase(recentQueries), ClearRecentQueriesUseCase(recentQueries),
                SaveRecentQueryUseCase(recentQueries), entry.suggestions(), entry.selectableCategories(), entry.tags(),
                entry.websiteItems(), entry.applyFilter(), entry.warmIndex(), activity.applicationContext,
            ))!!
        }
        return ViewModelProvider(activity.viewModelStore, factory)
            .get(key, SearchViewModel::class.java)
    }
}

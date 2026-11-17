package com.linknest.app

import android.os.Bundle
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.linknest.feature.search.SEARCH_FILTER_ID_ARG
import com.linknest.feature.search.SearchViewModel
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SearchSelectionStateTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun routeSelection_appliesOnce_andRestoredSelection_reconcilesDeletedFilter() {
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
        }
    }

    private fun createViewModel(key: String, args: Bundle): SearchViewModel {
        val activity = compose.activity
        val extras = MutableCreationExtras(activity.defaultViewModelCreationExtras).apply { set(DEFAULT_ARGS_KEY, args) }
        return ViewModelProvider(activity.viewModelStore, activity.defaultViewModelProviderFactory, extras)
            .get(key, SearchViewModel::class.java)
    }
}

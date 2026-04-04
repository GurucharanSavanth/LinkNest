package com.linknest.core.data.usecase

import com.linknest.core.data.repository.CategoryRepository
import com.linknest.core.data.repository.UserPreferencesRepository
import com.linknest.core.model.DashboardSmartSection
import com.linknest.core.model.DashboardModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveDashboardUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<DashboardModel> = combine(
        categoryRepository.observeDashboardCategories(),
        userPreferencesRepository.observeUserPreferences(),
    ) { categories, preferences ->
        val allWebsites = categories.flatMap { it.websites }
        val smartSections = buildList {
            val pinned = allWebsites
                .filter { it.isPinned }
                .sortedByDescending { it.openCount }
                .take(6)
            if (pinned.isNotEmpty()) {
                add(DashboardSmartSection(id = "pinned", title = "Pinned", websites = pinned))
            }

            val recent = allWebsites
                .asSequence()
                .filter { it.lastOpenedAt != null }
                .sortedByDescending { it.lastOpenedAt }
                .take(6)
                .toList()
            if (recent.isNotEmpty()) {
                add(DashboardSmartSection(id = "recent", title = "Recent", websites = recent))
            }

            val mostUsed = allWebsites
                .asSequence()
                .filter { it.openCount > 0 }
                .sortedWith(
                    compareByDescending<com.linknest.core.model.WebsiteListItem> { it.openCount }
                        .thenByDescending { it.lastOpenedAt ?: 0L },
                )
                .take(6)
                .toList()
            if (mostUsed.isNotEmpty()) {
                add(DashboardSmartSection(id = "most_used", title = "Frequent", websites = mostUsed))
            }

            val needsAttention = allWebsites
                .asSequence()
                .filter { website ->
                    website.healthStatus in ATTENTION_HEALTH_STATUSES ||
                        website.followUpStatus != com.linknest.core.model.FollowUpStatus.NONE
                }
                .sortedByDescending { it.revisitAt ?: it.lastCheckedAt ?: it.lastOpenedAt ?: 0L }
                .take(6)
                .toList()
            if (needsAttention.isNotEmpty()) {
                add(
                    DashboardSmartSection(
                        id = "needs_attention",
                        title = "Needs Attention",
                        websites = needsAttention,
                    ),
                )
            }

            val duplicates = allWebsites
                .filter { it.duplicateCount > 0 }
                .sortedByDescending { it.duplicateCount }
                .take(6)
            if (duplicates.isNotEmpty()) {
                add(
                    DashboardSmartSection(
                        id = "duplicates",
                        title = "Duplicates",
                        websites = duplicates,
                    ),
                )
            }

            val unsorted = allWebsites
                .filter { website ->
                    website.tagNames.isEmpty() &&
                        website.note.isNullOrBlank() &&
                        website.reasonSaved.isNullOrBlank() &&
                        !website.isPinned
                }
                .take(6)
            if (unsorted.isNotEmpty()) {
                add(
                    DashboardSmartSection(
                        id = "unsorted",
                        title = "Unsorted",
                        websites = unsorted,
                    ),
                )
            }

            val lastImported = allWebsites
                .sortedByDescending { it.id }
                .take(6)
            if (lastImported.isNotEmpty()) {
                add(
                    DashboardSmartSection(
                        id = "last_imported",
                        title = "Last Imported",
                        websites = lastImported,
                    ),
                )
            }
        }
        DashboardModel(
            categories = categories,
            smartSections = smartSections,
            layoutMode = preferences.layoutMode,
            tileSizeDp = preferences.tileSizeDp,
            tileDensityMode = preferences.tileDensityMode,
        )
    }

    private companion object {
        val ATTENTION_HEALTH_STATUSES = setOf(
            com.linknest.core.model.HealthStatus.BLOCKED,
            com.linknest.core.model.HealthStatus.LOGIN_REQUIRED,
            com.linknest.core.model.HealthStatus.REDIRECTED,
            com.linknest.core.model.HealthStatus.DNS_FAILED,
            com.linknest.core.model.HealthStatus.SSL_ISSUE,
            com.linknest.core.model.HealthStatus.DEAD,
            com.linknest.core.model.HealthStatus.TIMEOUT,
        )
    }
}

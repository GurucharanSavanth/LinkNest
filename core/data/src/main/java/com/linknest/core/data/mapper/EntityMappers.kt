package com.linknest.core.data.mapper

import com.linknest.core.database.entity.CategoryEntity
import com.linknest.core.database.entity.IconCacheEntity
import com.linknest.core.database.entity.IntegrityEventEntity
import com.linknest.core.database.entity.SavedFilterEntity
import com.linknest.core.database.entity.WebsiteEntryEntity
import com.linknest.core.database.model.DomainCategorySuggestionRow
import com.linknest.core.database.model.CategoryWithWebsitesEntity
import com.linknest.core.database.model.SearchWebsiteRow
import com.linknest.core.database.model.TagUsageRow
import com.linknest.core.data.model.DomainCategoryMapping
import com.linknest.core.model.DashboardCategory
import com.linknest.core.model.CategorySuggestion
import com.linknest.core.model.IntegrityEvent
import com.linknest.core.model.MetadataPreview
import com.linknest.core.model.SavedFilter
import com.linknest.core.model.SavedFilterSpec
import com.linknest.core.model.SearchResultItem
import com.linknest.core.model.SelectableCategory
import com.linknest.core.model.TagModel
import com.linknest.core.model.WebsiteListItem
import com.linknest.core.network.model.MetadataResult

fun CategoryEntity.asSelectableCategory(): SelectableCategory = SelectableCategory(
    id = id,
    name = name,
    colorHex = colorHex,
    iconType = iconType,
    iconValue = iconValue,
)

fun CategoryWithWebsitesEntity.asDashboardCategory(
    iconCacheByWebsiteId: Map<Long, IconCacheEntity>,
    tagNamesByWebsiteId: Map<Long, List<String>>,
): DashboardCategory = DashboardCategory(
    id = category.id,
    name = category.name,
    colorHex = category.colorHex,
    iconType = category.iconType,
    iconValue = category.iconValue,
    isCollapsed = category.isCollapsed,
    isArchived = category.isArchived,
    websites = websites
        .sortedWith(
            compareBy<WebsiteEntryEntity> { it.sortOrder }
                .thenBy { it.title.lowercase() },
        )
        .map { website ->
            website.asWebsiteListItem(
                cachedIconUri = iconCacheByWebsiteId[website.id]?.localUri,
                tagNames = tagNamesByWebsiteId[website.id].orEmpty(),
            )
        },
)

fun WebsiteEntryEntity.asWebsiteListItem(
    cachedIconUri: String? = null,
    tagNames: List<String> = emptyList(),
): WebsiteListItem = WebsiteListItem(
    id = id,
    categoryId = categoryId,
    title = title,
    domain = domain,
    normalizedUrl = normalizedUrl,
    finalUrl = finalUrl,
    canonicalUrl = canonicalUrl,
    ogImageUrl = ogImageUrl,
    faviconUrl = faviconUrl,
    chosenIconSource = chosenIconSource,
    customIconUri = customIconUri,
    emojiIcon = emojiIcon,
    tileSizeDp = tileSizeDp,
    cachedIconUri = cachedIconUri,
    isPinned = isPinned,
    openCount = openCount,
    lastOpenedAt = lastOpenedAt,
    lastCheckedAt = lastCheckedAt,
    healthStatus = healthStatus,
    note = note,
    reasonSaved = reasonSaved,
    priority = priority,
    followUpStatus = followUpStatus,
    revisitAt = revisitAt,
    sourceLabel = sourceLabel,
    customLabel = customLabel,
    sortOrder = sortOrder,
    tagNames = tagNames,
)

fun MetadataResult.asPreview(normalizedUrl: String): MetadataPreview = MetadataPreview(
    title = title,
    normalizedUrl = normalizedUrl,
    canonicalUrl = canonicalUrl,
    finalUrl = finalUrl,
    domain = domain,
    ogImageUrl = ogImageUrl,
    faviconUrl = faviconUrl,
    chosenIconSource = chosenIconSource,
)

fun SearchWebsiteRow.asSearchResultItem(): SearchResultItem = SearchResultItem(
    websiteId = websiteId,
    categoryId = categoryId,
    categoryName = categoryName,
    title = title,
    domain = domain,
    normalizedUrl = normalizedUrl,
    finalUrl = finalUrl,
    faviconUrl = faviconUrl,
    ogImageUrl = ogImageUrl,
    chosenIconSource = chosenIconSource,
    customIconUri = customIconUri,
    emojiIcon = emojiIcon,
    cachedIconUri = cachedIconUri,
    healthStatus = healthStatus,
    priority = priority,
    followUpStatus = followUpStatus,
    tagNames = tagNames.split(',').map(String::trim).filter(String::isNotEmpty),
    matchedFields = matchedFields.split('|').map(String::trim).filter(String::isNotEmpty),
)

fun DomainCategorySuggestionRow.asCategorySuggestion(): CategorySuggestion = CategorySuggestion(
    categoryId = categoryId,
    categoryName = categoryName,
    reason = "Suggested from $domain history",
    score = usageCount * 10,
)

fun TagUsageRow.asTagModel(): TagModel = TagModel(
    id = id,
    name = name,
    usageCount = usageCount,
)

fun DomainCategoryMapping.asExportString(): String =
    "$domain|$categoryId|$usageCount|$lastUsedAt"

fun SavedFilterEntity.asSavedFilter(spec: SavedFilterSpec): SavedFilter = SavedFilter(
    id = id,
    name = name,
    spec = spec,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun IntegrityEventEntity.asIntegrityEvent(): IntegrityEvent = IntegrityEvent(
    id = id,
    type = type,
    title = title,
    summary = summary,
    successful = successful,
    createdAt = createdAt,
)

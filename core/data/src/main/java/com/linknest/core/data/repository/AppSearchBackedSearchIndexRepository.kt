package com.linknest.core.data.repository

import android.content.Context
import android.util.Log
import androidx.appsearch.app.AppSearchBatchResult
import androidx.appsearch.app.AppSearchSchema
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.GenericDocument
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.RemoveByDocumentIdRequest
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.app.SetSchemaRequest
import androidx.appsearch.localstorage.LocalStorage
import com.google.common.util.concurrent.ListenableFuture
import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.database.dao.CategoryDao
import com.linknest.core.database.dao.IconCacheDao
import com.linknest.core.database.dao.SavedFilterDao
import com.linknest.core.database.dao.TagDao
import com.linknest.core.database.dao.WebsiteDao
import com.linknest.core.database.entity.CategoryEntity
import com.linknest.core.database.entity.SavedFilterEntity
import com.linknest.core.database.entity.TagEntity
import com.linknest.core.database.entity.WebsiteEntryEntity
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.model.SearchResultItem
import com.linknest.core.model.SearchSuggestion
import com.linknest.core.model.SearchSuggestionType
import com.linknest.core.model.WebsitePriority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AppSearchBackedSearchIndexRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val websiteDao: WebsiteDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val iconCacheDao: IconCacheDao,
    private val savedFilterDao: SavedFilterDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SearchIndexRepository {
    @Volatile
    private var searchSession: AppSearchSession? = null

    @Volatile
    private var hasWarmedIndex = false

    override suspend fun warmIndex() = withContext(ioDispatcher) {
        if (hasWarmedIndex) return@withContext

        runCatching {
            val hadExistingStorage = appSearchStorageHasFiles()
            var session = requireSession()
            if (session.getNamespacesAsync().await().isEmpty()) {
                if (hadExistingStorage) {
                    Log.w(TAG, "AppSearch opened with existing on-disk files but no namespaces. Resetting local index.")
                    resetSearchStorage()
                    session = requireSession()
                }
                rebuildAll(session)
            }
            hasWarmedIndex = true
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
        }
    }

    override suspend fun rebuildIndex() = withContext(ioDispatcher) {
        runCatching {
            val session = requireSession()
            clearIndexedDocuments(session)
            rebuildAll(session)
            hasWarmedIndex = true
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
        }
    }

    override suspend fun search(
        query: String,
        limit: Int,
    ): List<SearchResultItem> = withContext(ioDispatcher) {
        val sanitized = query.trim()
        if (sanitized.isBlank()) return@withContext emptyList()

        runCatching {
            warmIndex()
            val searchResults = requireSession().search(
                sanitized,
                SearchSpec.Builder()
                    .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                    .addFilterSchemas(WEBSITE_SCHEMA)
                    .setResultCountPerPage(limit.coerceAtLeast(1))
                    .build(),
            )

            try {
                buildList {
                    while (size < limit) {
                        val page = searchResults.getNextPageAsync().await()
                        if (page.isEmpty()) break
                        page.forEach { result ->
                            add(result.getGenericDocument().toSearchResultItem(sanitized))
                        }
                    }
                }.take(limit)
            } finally {
                searchResults.close()
            }
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            emptyList()
        }
    }

    override suspend fun suggestions(
        query: String,
        limit: Int,
    ): List<SearchSuggestion> = withContext(ioDispatcher) {
        val sanitized = query.trim()
        if (sanitized.isBlank()) return@withContext emptyList()

        runCatching {
            warmIndex()
            val searchResults = requireSession().search(
                sanitized,
                SearchSpec.Builder()
                    .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                    .setResultCountPerPage((limit * 3).coerceAtLeast(limit))
                    .build(),
            )

            try {
                val suggestions = linkedMapOf<String, SearchSuggestion>()
                while (suggestions.size < limit) {
                    val page = searchResults.getNextPageAsync().await()
                    if (page.isEmpty()) break
                    page.forEach { result ->
                        result.getGenericDocument().collectSuggestions(
                            query = sanitized,
                            sink = suggestions,
                        )
                    }
                }
                suggestions.values.take(limit)
            } finally {
                searchResults.close()
            }
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            emptyList()
        }
    }

    override suspend fun indexWebsite(websiteId: Long) = withContext(ioDispatcher) {
        runCatching {
            warmIndex()
            val session = requireSession()
            val document = buildWebsiteDocument(websiteId)
            if (document == null) {
                removeDocument(session, WEBSITE_NAMESPACE, websiteDocumentId(websiteId))
            } else {
                putDocuments(session, listOf(document))
            }
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
        }
    }

    override suspend fun removeWebsite(websiteId: Long) = withContext(ioDispatcher) {
        runCatching {
            removeDocument(requireSession(), WEBSITE_NAMESPACE, websiteDocumentId(websiteId))
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
        }
    }

    override suspend fun indexCategory(categoryId: Long) = withContext(ioDispatcher) {
        runCatching {
            warmIndex()
            val session = requireSession()
            val category = categoryDao.getCategoryById(categoryId)
            if (category == null || category.isArchived) {
                removeDocument(session, AUX_NAMESPACE, categoryDocumentId(categoryId))
            } else {
                putDocuments(session, listOf(category.toDocument()))
            }
            reindexCategoryWebsites(categoryId)
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
        }
    }

    override suspend fun removeCategory(categoryId: Long) = withContext(ioDispatcher) {
        runCatching {
            val session = requireSession()
            removeDocument(session, AUX_NAMESPACE, categoryDocumentId(categoryId))
            reindexCategoryWebsites(categoryId)
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
        }
    }

    override suspend fun indexTag(tagId: Long) = withContext(ioDispatcher) {
        runCatching {
            warmIndex()
            val session = requireSession()
            val tag = tagDao.getAllTags().firstOrNull { candidate -> candidate.id == tagId }
            if (tag == null) {
                removeDocument(session, AUX_NAMESPACE, tagDocumentId(tagId))
            } else {
                putDocuments(session, listOf(tag.toDocument()))
            }
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
        }
    }

    override suspend fun removeTag(tagId: Long) = withContext(ioDispatcher) {
        runCatching {
            removeDocument(requireSession(), AUX_NAMESPACE, tagDocumentId(tagId))
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
        }
    }

    override suspend fun indexSavedFilter(filterId: Long) = withContext(ioDispatcher) {
        runCatching {
            warmIndex()
            val session = requireSession()
            val filter = savedFilterDao.getSavedFilterById(filterId)
            if (filter == null) {
                removeDocument(session, AUX_NAMESPACE, savedFilterDocumentId(filterId))
            } else {
                putDocuments(session, listOf(filter.toDocument()))
            }
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
        }
    }

    override suspend fun removeSavedFilter(filterId: Long) = withContext(ioDispatcher) {
        runCatching {
            removeDocument(requireSession(), AUX_NAMESPACE, savedFilterDocumentId(filterId))
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
        }
    }

    override suspend fun reindexCategoryWebsites(categoryId: Long) = withContext(ioDispatcher) {
        runCatching {
            warmIndex()
            val session = requireSession()
            websiteDao.getWebsitesByCategory(categoryId).forEach { entity ->
                val document = buildWebsiteDocument(entity.id)
                if (document == null) {
                    removeDocument(session, WEBSITE_NAMESPACE, websiteDocumentId(entity.id))
                } else {
                    putDocuments(session, listOf(document))
                }
            }
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
        }
    }

    private suspend fun rebuildAll(session: AppSearchSession) {
        val categories = categoryDao.getActiveCategories()
        val categoriesById = categories.associateBy(CategoryEntity::id)
        val tags = tagDao.getAllTags()
        val tagsById = tags.associateBy(TagEntity::id)
        val tagNamesByWebsiteId = tagDao.getAllCrossRefs()
            .groupBy(
                keySelector = { crossRef -> crossRef.websiteId },
                valueTransform = { crossRef -> tagsById[crossRef.tagId]?.name.orEmpty() },
            )

        val websiteDocuments = websiteDao.getAllWebsites().mapNotNull { website ->
            val category = categoriesById[website.categoryId] ?: return@mapNotNull null
            website.toDocument(
                categoryName = category.name,
                tagNames = tagNamesByWebsiteId[website.id].orEmpty().filter(String::isNotBlank),
                cachedIconUri = iconCacheDao.getByWebsiteId(website.id)?.localUri,
            )
        }

        val allDocuments = buildList {
            addAll(categories.map { category -> category.toDocument() })
            addAll(tags.map { tag -> tag.toDocument() })
            addAll(savedFilterDao.getSavedFilters().map { filter -> filter.toDocument() })
            addAll(websiteDocuments)
        }

        if (allDocuments.isNotEmpty()) {
            putDocuments(session, allDocuments)
        }
    }

    private suspend fun buildWebsiteDocument(websiteId: Long): GenericDocument? {
        val entity = websiteDao.getWebsiteById(websiteId) ?: return null
        val category = categoryDao.getCategoryById(entity.categoryId) ?: return null
        if (category.isArchived) return null
        return entity.toDocument(
            categoryName = category.name,
            tagNames = tagDao.getTagNamesForWebsite(entity.id),
            cachedIconUri = iconCacheDao.getByWebsiteId(entity.id)?.localUri,
        )
    }

    private suspend fun requireSession(): AppSearchSession {
        searchSession?.let { return it }

        val created = runCatching {
            createSession()
        }.recoverCatching { throwable ->
            if (throwable is CancellationException) throw throwable
            Log.w(TAG, "AppSearch session creation failed. Resetting local index and retrying.", throwable)
            resetSearchStorage()
            createSession()
        }.getOrThrow()

        searchSession = created
        return created
    }

    private suspend fun createSession(): AppSearchSession {
        val created = LocalStorage.createSearchSessionAsync(
            LocalStorage.SearchContext.Builder(context, APP_SEARCH_DATABASE).build(),
        ).await()

        created.setSchemaAsync(
            SetSchemaRequest.Builder()
                .addSchemas(
                    websiteSchema(),
                    categorySchema(),
                    tagSchema(),
                    savedFilterSchema(),
                )
                .setForceOverride(false)
                .build(),
        ).await()
        return created
    }

    private fun appSearchStorageHasFiles(): Boolean {
        val storageDir = context.filesDir.resolve(APP_SEARCH_STORAGE_DIRECTORY)
        return storageDir.exists() && storageDir.walkTopDown().any { file -> file.isFile }
    }

    private fun resetSearchStorage() {
        runCatching { searchSession?.close() }
        searchSession = null
        hasWarmedIndex = false
        context.filesDir.resolve(APP_SEARCH_STORAGE_DIRECTORY).deleteRecursively()
    }

    private suspend fun putDocuments(
        session: AppSearchSession,
        documents: List<GenericDocument>,
    ) {
        if (documents.isEmpty()) return
        session.putAsync(
            PutDocumentsRequest.Builder()
                .addGenericDocuments(documents)
                .build(),
        ).await().throwIfFailed()
    }

    private suspend fun removeDocument(
        session: AppSearchSession,
        namespace: String,
        documentId: String,
    ) {
        session.removeAsync(
            RemoveByDocumentIdRequest.Builder(namespace)
                .addIds(documentId)
                .build(),
        ).await().throwIfFailed()
    }

    private suspend fun clearIndexedDocuments(session: AppSearchSession) {
        session.removeAsync(
            "",
            SearchSpec.Builder()
                .addFilterNamespaces(WEBSITE_NAMESPACE, AUX_NAMESPACE)
                .build(),
        ).await()
    }

    private fun websiteSchema(): AppSearchSchema = AppSearchSchema.Builder(WEBSITE_SCHEMA)
        .addProperty(indexedStringProperty("title"))
        .addProperty(indexedStringProperty("domain"))
        .addProperty(indexedStringProperty("normalizedUrl"))
        .addProperty(indexedStringProperty("finalUrl"))
        .addProperty(indexedStringProperty("categoryName"))
        .addProperty(indexedStringProperty("note"))
        .addProperty(indexedStringProperty("reasonSaved"))
        .addProperty(indexedStringProperty("sourceLabel"))
        .addProperty(indexedStringProperty("customLabel"))
        .addProperty(indexedRepeatedStringProperty("tagNames"))
        .addProperty(indexedRepeatedStringProperty("keywords"))
        .addProperty(plainStringProperty("healthStatus"))
        .addProperty(plainStringProperty("priority"))
        .addProperty(plainStringProperty("followUpStatus"))
        .addProperty(plainStringProperty("chosenIconSource"))
        .addProperty(plainStringProperty("faviconUrl"))
        .addProperty(plainStringProperty("ogImageUrl"))
        .addProperty(plainStringProperty("customIconUri"))
        .addProperty(plainStringProperty("emojiIcon"))
        .addProperty(plainStringProperty("cachedIconUri"))
        .addProperty(longProperty("websiteId"))
        .addProperty(longProperty("categoryId"))
        .build()

    private fun categorySchema(): AppSearchSchema = AppSearchSchema.Builder(CATEGORY_SCHEMA)
        .addProperty(indexedStringProperty("name"))
        .addProperty(indexedRepeatedStringProperty("keywords"))
        .addProperty(longProperty("categoryId"))
        .build()

    private fun tagSchema(): AppSearchSchema = AppSearchSchema.Builder(TAG_SCHEMA)
        .addProperty(indexedStringProperty("name"))
        .addProperty(indexedRepeatedStringProperty("keywords"))
        .addProperty(longProperty("tagId"))
        .build()

    private fun savedFilterSchema(): AppSearchSchema = AppSearchSchema.Builder(SAVED_FILTER_SCHEMA)
        .addProperty(indexedStringProperty("name"))
        .addProperty(indexedStringProperty("query"))
        .addProperty(indexedRepeatedStringProperty("keywords"))
        .addProperty(longProperty("savedFilterId"))
        .build()

    private fun indexedStringProperty(name: String): AppSearchSchema.StringPropertyConfig =
        AppSearchSchema.StringPropertyConfig.Builder(name)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .build()

    private fun indexedRepeatedStringProperty(name: String): AppSearchSchema.StringPropertyConfig =
        AppSearchSchema.StringPropertyConfig.Builder(name)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .build()

    private fun plainStringProperty(name: String): AppSearchSchema.StringPropertyConfig =
        AppSearchSchema.StringPropertyConfig.Builder(name)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .build()

    private fun longProperty(name: String): AppSearchSchema.LongPropertyConfig =
        AppSearchSchema.LongPropertyConfig.Builder(name)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE)
            .build()

    private fun CategoryEntity.toDocument(): GenericDocument {
        val keywords = listOf(name, iconValue.orEmpty()).filter(String::isNotBlank)
        val builder = documentBuilder(AUX_NAMESPACE, categoryDocumentId(id), CATEGORY_SCHEMA)
            .setPropertyLong("categoryId", id)
            .setPropertyString("name", name)
        if (keywords.isNotEmpty()) {
            builder.setPropertyString("keywords", *keywords.toTypedArray())
        }
        return builder.build()
    }

    private fun TagEntity.toDocument(): GenericDocument {
        val builder = documentBuilder(AUX_NAMESPACE, tagDocumentId(id), TAG_SCHEMA)
            .setPropertyLong("tagId", id)
            .setPropertyString("name", name)
        builder.setPropertyString("keywords", name)
        return builder.build()
    }

    private fun SavedFilterEntity.toDocument(): GenericDocument {
        val query = runCatching { JSONObject(specJson).optString("query") }.getOrDefault("")
        val keywords = listOf(name, query).filter(String::isNotBlank)
        val builder = documentBuilder(AUX_NAMESPACE, savedFilterDocumentId(id), SAVED_FILTER_SCHEMA)
            .setPropertyLong("savedFilterId", id)
            .setPropertyString("name", name)
        if (query.isNotBlank()) {
            builder.setPropertyString("query", query)
        }
        if (keywords.isNotEmpty()) {
            builder.setPropertyString("keywords", *keywords.toTypedArray())
        }
        return builder.build()
    }

    private fun WebsiteEntryEntity.toDocument(
        categoryName: String,
        tagNames: List<String>,
        cachedIconUri: String?,
    ): GenericDocument {
        val keywords = buildList {
            add(categoryName)
            addAll(tagNames)
            add(domain)
            add(title)
            add(healthStatus.name.lowercase())
            add(priority.name.lowercase())
            add(followUpStatus.name.lowercase())
            if (isPinned) add("pinned")
            note?.takeIf(String::isNotBlank)?.let(::add)
            reasonSaved?.takeIf(String::isNotBlank)?.let(::add)
        }.distinct()

        val builder = documentBuilder(WEBSITE_NAMESPACE, websiteDocumentId(id), WEBSITE_SCHEMA)
            .setPropertyLong("websiteId", id)
            .setPropertyLong("categoryId", categoryId)
            .setPropertyString("title", title)
            .setPropertyString("domain", domain)
            .setPropertyString("normalizedUrl", normalizedUrl)
            .setPropertyString("categoryName", categoryName)
            .setPropertyString("healthStatus", healthStatus.name)
            .setPropertyString("priority", priority.name)
            .setPropertyString("followUpStatus", followUpStatus.name)
            .setPropertyString("chosenIconSource", chosenIconSource.name)

        finalUrl?.takeIf(String::isNotBlank)?.let { builder.setPropertyString("finalUrl", it) }
        note?.takeIf(String::isNotBlank)?.let { builder.setPropertyString("note", it) }
        reasonSaved?.takeIf(String::isNotBlank)?.let { builder.setPropertyString("reasonSaved", it) }
        sourceLabel?.takeIf(String::isNotBlank)?.let { builder.setPropertyString("sourceLabel", it) }
        customLabel?.takeIf(String::isNotBlank)?.let { builder.setPropertyString("customLabel", it) }
        faviconUrl?.takeIf(String::isNotBlank)?.let { builder.setPropertyString("faviconUrl", it) }
        ogImageUrl?.takeIf(String::isNotBlank)?.let { builder.setPropertyString("ogImageUrl", it) }
        customIconUri?.takeIf(String::isNotBlank)?.let { builder.setPropertyString("customIconUri", it) }
        emojiIcon?.takeIf(String::isNotBlank)?.let { builder.setPropertyString("emojiIcon", it) }
        cachedIconUri?.takeIf(String::isNotBlank)?.let { builder.setPropertyString("cachedIconUri", it) }

        if (tagNames.isNotEmpty()) {
            builder.setPropertyString("tagNames", *tagNames.toTypedArray())
        }
        if (keywords.isNotEmpty()) {
            builder.setPropertyString("keywords", *keywords.toTypedArray())
        }
        return builder.build()
    }

    private fun GenericDocument.collectSuggestions(
        query: String,
        sink: MutableMap<String, SearchSuggestion>,
    ) {
        when (schemaType) {
            SAVED_FILTER_SCHEMA -> {
                val savedFilterId = longValue("savedFilterId") ?: return
                val name = stringValue("name") ?: return
                val filterQuery = stringValue("query").orEmpty()
                sink.putSuggestion(
                    SearchSuggestion(
                        id = "saved-filter-$savedFilterId",
                        type = SearchSuggestionType.SAVED_SEARCH,
                        title = name,
                        supportingText = filterQuery.ifBlank { "Saved search" },
                        query = filterQuery.ifBlank { query },
                        savedFilterId = savedFilterId,
                    ),
                )
            }
            CATEGORY_SCHEMA -> {
                val name = stringValue("name") ?: return
                if (name.contains(query, ignoreCase = true)) {
                    sink.putSuggestion(
                        SearchSuggestion(
                            id = "category-$name",
                            type = SearchSuggestionType.CATEGORY,
                            title = name,
                            supportingText = "Category",
                            query = name,
                        ),
                    )
                }
            }
            TAG_SCHEMA -> {
                val name = stringValue("name") ?: return
                if (name.contains(query, ignoreCase = true)) {
                    sink.putSuggestion(
                        SearchSuggestion(
                            id = "tag-$name",
                            type = SearchSuggestionType.TAG,
                            title = name,
                            supportingText = "Tag",
                            query = name,
                        ),
                    )
                }
            }
            WEBSITE_SCHEMA -> {
                addWebsiteSuggestion("title", SearchSuggestionType.WEBSITE_TITLE, "Website", query, sink)
                addWebsiteSuggestion("domain", SearchSuggestionType.DOMAIN, "Domain", query, sink)
                addWebsiteSuggestion("categoryName", SearchSuggestionType.CATEGORY, "Category", query, sink)
                stringsValue("tagNames").forEach { tag ->
                    if (tag.contains(query, ignoreCase = true)) {
                        sink.putSuggestion(
                            SearchSuggestion(
                                id = "website-tag-$tag",
                                type = SearchSuggestionType.TAG,
                                title = tag,
                                supportingText = "Tag",
                                query = tag,
                            ),
                        )
                    }
                }
                stringValue("note")
                    ?.takeIf { noteValue -> noteValue.contains(query, ignoreCase = true) }
                    ?.let { noteValue ->
                        sink.putSuggestion(
                            SearchSuggestion(
                                id = "note-${longValue("websiteId") ?: noteValue.hashCode().toLong()}",
                                type = SearchSuggestionType.NOTE,
                                title = noteValue.take(48),
                                supportingText = stringValue("title"),
                                query = query,
                            ),
                        )
                    }
            }
        }
    }

    private fun GenericDocument.addWebsiteSuggestion(
        property: String,
        type: SearchSuggestionType,
        supportingText: String,
        query: String,
        sink: MutableMap<String, SearchSuggestion>,
    ) {
        val value = stringValue(property) ?: return
        if (!value.contains(query, ignoreCase = true)) return
        sink.putSuggestion(
            SearchSuggestion(
                id = "$property-$value",
                type = type,
                title = value,
                supportingText = supportingText,
                query = value,
            ),
        )
    }

    private fun GenericDocument.toSearchResultItem(query: String): SearchResultItem {
        val matchedFields = buildList {
            if (stringValue("title").orEmpty().contains(query, ignoreCase = true)) add("title")
            if (stringValue("domain").orEmpty().contains(query, ignoreCase = true)) add("domain")
            if (stringValue("normalizedUrl").orEmpty().contains(query, ignoreCase = true)) add("url")
            if (stringValue("finalUrl").orEmpty().contains(query, ignoreCase = true)) add("final")
            if (stringValue("categoryName").orEmpty().contains(query, ignoreCase = true)) add("category")
            if (stringsValue("tagNames").any { tag -> tag.contains(query, ignoreCase = true) }) add("tag")
            if (stringValue("note").orEmpty().contains(query, ignoreCase = true)) add("note")
        }

        return SearchResultItem(
            websiteId = longValue("websiteId") ?: 0L,
            categoryId = longValue("categoryId") ?: 0L,
            categoryName = stringValue("categoryName").orEmpty(),
            title = stringValue("title").orEmpty(),
            domain = stringValue("domain").orEmpty(),
            normalizedUrl = stringValue("normalizedUrl").orEmpty(),
            finalUrl = stringValue("finalUrl"),
            faviconUrl = stringValue("faviconUrl"),
            ogImageUrl = stringValue("ogImageUrl"),
            chosenIconSource = runCatching {
                IconSource.valueOf(stringValue("chosenIconSource") ?: "")
            }.getOrDefault(IconSource.GENERATED),
            customIconUri = stringValue("customIconUri"),
            emojiIcon = stringValue("emojiIcon"),
            cachedIconUri = stringValue("cachedIconUri"),
            healthStatus = runCatching {
                HealthStatus.valueOf(stringValue("healthStatus") ?: HealthStatus.UNKNOWN.name)
            }.getOrDefault(HealthStatus.UNKNOWN),
            priority = runCatching {
                WebsitePriority.valueOf(stringValue("priority") ?: WebsitePriority.NORMAL.name)
            }.getOrDefault(WebsitePriority.NORMAL),
            followUpStatus = runCatching {
                FollowUpStatus.valueOf(stringValue("followUpStatus") ?: FollowUpStatus.NONE.name)
            }.getOrDefault(FollowUpStatus.NONE),
            tagNames = stringsValue("tagNames"),
            matchedFields = matchedFields,
        )
    }

    private fun GenericDocument.stringValue(property: String): String? =
        getPropertyString(property)?.takeIf(String::isNotBlank)

    private fun GenericDocument.stringsValue(property: String): List<String> =
        getPropertyStringArray(property)?.filter(String::isNotBlank).orEmpty()

    private fun GenericDocument.longValue(property: String): Long? =
        getPropertyLongArray(property)?.firstOrNull()

    private fun MutableMap<String, SearchSuggestion>.putSuggestion(suggestion: SearchSuggestion) {
        putIfAbsent("${suggestion.type}:${suggestion.title.lowercase()}", suggestion)
    }

    private fun documentBuilder(
        namespace: String,
        documentId: String,
        schemaType: String,
    ): GenericDocument.Builder<GenericDocument.Builder<*>> =
        GenericDocument.Builder(namespace, documentId, schemaType)

    private suspend fun <T> ListenableFuture<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addListener(
                {
                    runCatching { get() }
                        .onSuccess(continuation::resume)
                        .onFailure(continuation::resumeWithException)
                },
                Runnable::run,
            )
            continuation.invokeOnCancellation { cancel(true) }
        }

    private fun <K, V> AppSearchBatchResult<K, V>.throwIfFailed() {
        if (isSuccess) return
        val failure = failures.values.firstOrNull()
        val message = failure?.errorMessage?.takeIf(String::isNotBlank)
            ?: "AppSearch batch operation failed."
        throw IllegalStateException(message)
    }

    private companion object {
        const val TAG = "LinkNestAppSearch"
        const val APP_SEARCH_DATABASE = "linknest-search"
        const val APP_SEARCH_STORAGE_DIRECTORY = "appsearch"
        const val WEBSITE_NAMESPACE = "websites"
        const val AUX_NAMESPACE = "aux"
        const val WEBSITE_SCHEMA = "website_document"
        const val CATEGORY_SCHEMA = "category_document"
        const val TAG_SCHEMA = "tag_document"
        const val SAVED_FILTER_SCHEMA = "saved_filter_document"

        fun websiteDocumentId(websiteId: Long): String = "website:$websiteId"
        fun categoryDocumentId(categoryId: Long): String = "category:$categoryId"
        fun tagDocumentId(tagId: Long): String = "tag:$tagId"
        fun savedFilterDocumentId(filterId: Long): String = "saved-filter:$filterId"
    }
}

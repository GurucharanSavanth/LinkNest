package com.linknest.core.data.model

import com.linknest.core.model.*
import org.junit.Assert.*
import org.junit.Test

class MetadataMergeTest {
    private val existing = WebsiteListItem(
        id = 7, categoryId = 2, title = "My title", domain = "example.com",
        normalizedUrl = "https://example.com/saved", finalUrl = "https://example.com/final",
        canonicalUrl = "https://example.com/canonical", ogImageUrl = "https://example.com/image",
        faviconUrl = "https://example.com/icon", chosenIconSource = IconSource.CUSTOM,
        customIconUri = "content://my/icon", emojiIcon = null, tileSizeDp = 112,
        cachedIconUri = "file:///cache/icon", isPinned = true, openCount = 9,
        lastOpenedAt = 3, lastCheckedAt = 4, healthStatus = HealthStatus.OK,
        note = "Personal note", reasonSaved = "Research", priority = WebsitePriority.HIGH,
        followUpStatus = FollowUpStatus.REVIEW, revisitAt = 20, sourceLabel = "Work",
        customLabel = "Read", sortOrder = 5, tagNames = listOf("Saved"),
    )
    private val incoming = PersistWebsiteRequest(
        categoryId = 9, title = "Generated title", canonicalUrl = null, finalUrl = null,
        normalizedUrl = "https://example.com/new", domain = "example.com",
        ogImageUrl = null, faviconUrl = null, chosenIconSource = IconSource.GENERATED,
        customIconUri = null, emojiIcon = null, tileSizeDp = null, sortOrder = 0,
        isPinned = false, healthStatus = HealthStatus.UNKNOWN, createdAt = 30, updatedAt = 30,
        lastCheckedAt = 30, tagIds = listOf(1, 2),
    )

    @Test fun failedCapture_preservesSavedIdentityPersonalFieldsAndImages() {
        val result = incoming.mergeMetadataInto(existing)
        assertEquals(7L, result.existingWebsiteId); assertEquals(2L, result.categoryId)
        assertEquals(existing.title, result.title); assertEquals(existing.normalizedUrl, result.normalizedUrl)
        assertEquals(existing.finalUrl, result.finalUrl); assertEquals(existing.canonicalUrl, result.canonicalUrl)
        assertEquals(existing.ogImageUrl, result.ogImageUrl); assertEquals(existing.faviconUrl, result.faviconUrl)
        assertEquals(existing.customIconUri, result.customIconUri); assertEquals(IconSource.CUSTOM, result.chosenIconSource)
        assertEquals(existing.note, result.note); assertEquals(existing.reasonSaved, result.reasonSaved)
        assertEquals(existing.priority, result.priority); assertEquals(existing.followUpStatus, result.followUpStatus)
        assertEquals(existing.revisitAt, result.revisitAt); assertEquals(existing.sourceLabel, result.sourceLabel)
        assertEquals(existing.customLabel, result.customLabel); assertTrue(result.isPinned)
        assertEquals(5, result.sortOrder); assertEquals(HealthStatus.OK, result.healthStatus)
        assertEquals(existing.lastCheckedAt, result.lastCheckedAt); assertEquals(incoming.tagIds, result.tagIds)
        assertNull(result.iconCache)
    }

    @Test fun merge_fillsMissingValuesAndAcceptsFirstCachedImage() {
        val cache = PersistedIconCache("https://example.com/icon", "file:///new/icon", "hash", "image/png", 30, 30)
        val result = incoming.copy(note = "Incoming note", canonicalUrl = "https://example.com/canonical",
            faviconUrl = "https://example.com/icon", chosenIconSource = IconSource.REL_ICON, iconCache = cache)
            .mergeMetadataInto(existing.copy(note = null, canonicalUrl = null, faviconUrl = null,
                cachedIconUri = null, customIconUri = null, chosenIconSource = IconSource.GENERATED))
        assertEquals("Incoming note", result.note); assertEquals(incoming.copy(canonicalUrl = "https://example.com/canonical").canonicalUrl, result.canonicalUrl)
        assertEquals(cache, result.iconCache); assertEquals(IconSource.REL_ICON, result.chosenIconSource)
    }

    @Test fun emojiAndUnpinnedChoice_surviveIncomingIconAndPin() {
        val result = incoming.copy(isPinned = true).mergeMetadataInto(existing.copy(
            chosenIconSource = IconSource.EMOJI, customIconUri = null, emojiIcon = "*", isPinned = false,
        ))
        assertEquals(IconSource.EMOJI, result.chosenIconSource); assertEquals("*", result.emojiIcon)
        assertFalse(result.isPinned); assertNull(result.iconCache)
    }

    @Test fun titleDomainDuplicate_doesNotReplaceAnUnresolvedSavedDestination() {
        val result = incoming.copy(finalUrl = "https://example.com/new-destination")
            .mergeMetadataInto(existing.copy(finalUrl = null))
        assertEquals(existing.normalizedUrl, result.normalizedUrl)
        assertNull(result.finalUrl)
    }
}

package com.linknest.core.data.backup

import com.linknest.core.data.model.BackupSnapshot

/** Only persistent library content; export timestamps, local paths and preferences are separate stores. */
internal fun recoveryFingerprint(snapshot: BackupSnapshot): String = BackupSerializer().serialize(snapshot.copy(
    schemaVersion = 6, exportedAt = 0, appVersion = "", preferences = null, iconCache = emptyList(), omittedCustomImages = 0,
    categories = snapshot.categories.sortedBy { it.id },
    websites = snapshot.websites.sortedBy { it.id }.map { it.copy(customIconUri = null) },
    tags = snapshot.tags.sortedBy { it.id },
    websiteTags = snapshot.websiteTags.sortedWith(compareBy({ it.websiteId }, { it.tagId })),
    mappings = snapshot.mappings.sortedWith(compareBy({ it.domain }, { it.categoryId })),
    savedFilters = snapshot.savedFilters.sortedBy { it.id }, events = snapshot.events.sortedBy { it.id },
    recentQueries = snapshot.recentQueries.sortedBy { it.id }, assets = snapshot.assets.sortedBy { it.id },
)).checksum

package com.linknest.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.model.IconType
import com.linknest.core.model.IntegrityEventType
import com.linknest.core.model.WebsitePriority

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["sort_order"]),
        Index(value = ["is_archived"]),
    ],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "color_hex")
    val colorHex: String,
    @ColumnInfo(name = "icon_type")
    val iconType: IconType,
    @ColumnInfo(name = "icon_value")
    val iconValue: String?,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "is_collapsed")
    val isCollapsed: Boolean,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "website_entries",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["category_id", "sort_order"]),
        Index(value = ["normalized_url"]),
        Index(value = ["final_url"]),
        Index(value = ["domain"]),
        Index(value = ["domain", "title"]),
        Index(value = ["is_pinned"]),
        Index(value = ["health_status"]),
        Index(value = ["health_status", "last_checked_at"]),
        Index(value = ["priority"]),
        Index(value = ["follow_up_status"]),
        Index(value = ["revisit_at"]),
        Index(value = ["last_opened_at"]),
        Index(value = ["created_at"]),
        Index(value = ["updated_at"]),
        Index(value = ["sort_order"]),
    ],
)
data class WebsiteEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    val title: String,
    @ColumnInfo(name = "canonical_url")
    val canonicalUrl: String?,
    @ColumnInfo(name = "final_url")
    val finalUrl: String?,
    @ColumnInfo(name = "normalized_url")
    val normalizedUrl: String,
    val domain: String,
    @ColumnInfo(name = "og_image_url")
    val ogImageUrl: String?,
    @ColumnInfo(name = "favicon_url")
    val faviconUrl: String?,
    @ColumnInfo(name = "chosen_icon_source")
    val chosenIconSource: IconSource,
    @ColumnInfo(name = "custom_icon_uri")
    val customIconUri: String?,
    @ColumnInfo(name = "emoji_icon")
    val emojiIcon: String?,
    @ColumnInfo(name = "tile_size_dp")
    val tileSizeDp: Int?,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean,
    @ColumnInfo(name = "open_count")
    val openCount: Int,
    @ColumnInfo(name = "last_opened_at")
    val lastOpenedAt: Long?,
    @ColumnInfo(name = "last_checked_at")
    val lastCheckedAt: Long?,
    @ColumnInfo(name = "health_status")
    val healthStatus: HealthStatus,
    val note: String?,
    @ColumnInfo(name = "reason_saved")
    val reasonSaved: String?,
    val priority: WebsitePriority,
    @ColumnInfo(name = "follow_up_status")
    val followUpStatus: FollowUpStatus,
    @ColumnInfo(name = "revisit_at")
    val revisitAt: Long?,
    @ColumnInfo(name = "source_label")
    val sourceLabel: String?,
    @ColumnInfo(name = "custom_label")
    val customLabel: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "website_tag_cross_ref",
    primaryKeys = ["website_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = WebsiteEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["website_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["website_id"]),
        Index(value = ["tag_id"]),
    ],
)
data class WebsiteTagCrossRefEntity(
    @ColumnInfo(name = "website_id")
    val websiteId: Long,
    @ColumnInfo(name = "tag_id")
    val tagId: Long,
)

@Entity(
    tableName = "domain_category_mapping",
    primaryKeys = ["domain", "category_id"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["domain"]),
        Index(value = ["category_id"]),
        Index(value = ["usage_count"]),
        Index(value = ["last_used_at"]),
    ],
)
data class DomainCategoryMappingEntity(
    val domain: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "usage_count")
    val usageCount: Int,
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long,
)

@Entity(
    tableName = "icon_cache",
    indices = [
        Index(value = ["website_id"], unique = true),
        Index(value = ["source_url"]),
        Index(value = ["content_hash"]),
    ],
)
data class IconCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "website_id")
    val websiteId: Long,
    @ColumnInfo(name = "source_url")
    val sourceUrl: String?,
    @ColumnInfo(name = "local_uri")
    val localUri: String?,
    @ColumnInfo(name = "content_hash")
    val contentHash: String?,
    @ColumnInfo(name = "mime_type")
    val mimeType: String?,
    val etag: String?,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "saved_filters",
    indices = [Index(value = ["name"], unique = true)],
)
data class SavedFilterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "spec_json")
    val specJson: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "integrity_events",
    indices = [
        Index(value = ["type"]),
        Index(value = ["created_at"]),
    ],
)
data class IntegrityEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: IntegrityEventType,
    val title: String,
    val summary: String,
    val successful: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

@Entity(
    tableName = "recent_queries",
    indices = [
        Index(value = ["query"]),
        Index(value = ["last_used_at"]),
    ],
)
data class RecentQueryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    @ColumnInfo(name = "use_count")
    val useCount: Int,
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long,
)

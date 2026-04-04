package com.linknest.core.database.util

import androidx.room.TypeConverter
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.model.IconType
import com.linknest.core.model.WebsitePriority

class DatabaseConverters {
    @TypeConverter
    fun fromIconType(value: IconType): String = value.name

    @TypeConverter
    fun toIconType(value: String): IconType =
        runCatching { IconType.valueOf(value) }.getOrDefault(IconType.AUTO)

    @TypeConverter
    fun fromIconSource(value: IconSource): String = value.name

    @TypeConverter
    fun toIconSource(value: String): IconSource =
        runCatching { IconSource.valueOf(value) }.getOrDefault(IconSource.GENERATED)

    @TypeConverter
    fun fromHealthStatus(value: HealthStatus): String = value.name

    @TypeConverter
    fun toHealthStatus(value: String): HealthStatus =
        runCatching { HealthStatus.valueOf(value) }.getOrDefault(HealthStatus.UNKNOWN)

    @TypeConverter
    fun fromWebsitePriority(value: WebsitePriority): String = value.name

    @TypeConverter
    fun toWebsitePriority(value: String): WebsitePriority =
        runCatching { WebsitePriority.valueOf(value) }.getOrDefault(WebsitePriority.NORMAL)

    @TypeConverter
    fun fromFollowUpStatus(value: FollowUpStatus): String = value.name

    @TypeConverter
    fun toFollowUpStatus(value: String): FollowUpStatus =
        runCatching { FollowUpStatus.valueOf(value) }.getOrDefault(FollowUpStatus.NONE)
}

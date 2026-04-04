package com.linknest.core.data.repository

import com.linknest.core.database.dao.IntegrityEventDao
import com.linknest.core.database.entity.IntegrityEventEntity
import com.linknest.core.data.model.IntegrityEventRecord
import com.linknest.core.model.IntegrityEvent
import com.linknest.core.model.IntegrityEventType
import javax.inject.Inject

class OfflineFirstIntegrityEventRepository @Inject constructor(
    private val integrityEventDao: IntegrityEventDao,
) : IntegrityEventRepository {
    override suspend fun recordEvent(record: IntegrityEventRecord): Long =
        integrityEventDao.insertEvent(
            IntegrityEventEntity(
                type = record.type,
                title = record.title,
                summary = record.summary,
                successful = record.successful,
                createdAt = record.createdAt,
            ),
        )

    override suspend fun getRecentEvents(limit: Int): List<IntegrityEvent> =
        integrityEventDao.getRecentEvents(limit).map { entity -> entity.asModel() }

    override suspend fun getLatestEventTimestampByType(type: IntegrityEventType): Long? =
        integrityEventDao.getLatestEventByType(type)?.createdAt

    private fun IntegrityEventEntity.asModel(): IntegrityEvent = IntegrityEvent(
        id = id,
        type = type,
        title = title,
        summary = summary,
        successful = successful,
        createdAt = createdAt,
    )
}

package com.linknest.core.action.pipeline

import com.linknest.core.action.ActionIssue
import com.linknest.core.action.ActionResult
import com.linknest.core.action.action.CheckWebsiteHealthAction
import com.linknest.core.action.model.HealthCheckPipelineOutput
import com.linknest.core.action.model.HealthCheckProgress
import com.linknest.core.data.model.WebsiteHealthUpdate
import com.linknest.core.data.usecase.GetHealthCheckCandidatesUseCase
import com.linknest.core.data.usecase.PersistHealthStatusesUseCase
import com.linknest.core.model.HealthReportItem
import com.linknest.core.model.HealthStatus
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class HealthCheckPipeline @Inject constructor(
    private val getHealthCheckCandidatesUseCase: GetHealthCheckCandidatesUseCase,
    private val checkWebsiteHealthAction: CheckWebsiteHealthAction,
    private val persistHealthStatusesUseCase: PersistHealthStatusesUseCase,
) {
    suspend operator fun invoke(
        staleBefore: Long,
        limit: Int = 20,
        onProgress: (HealthCheckProgress) -> Unit = {},
    ): ActionResult<HealthCheckPipelineOutput> {
        val issues = mutableListOf<ActionIssue>()
        val candidates = getHealthCheckCandidatesUseCase(
            staleBefore = staleBefore,
            limit = limit,
        )
        if (candidates.isEmpty()) {
            return ActionResult.Success(
                HealthCheckPipelineOutput(
                    checkedCount = 0,
                    okCount = 0,
                    blockedCount = 0,
                    redirectedCount = 0,
                    deadCount = 0,
                    timeoutCount = 0,
                    items = emptyList(),
                ),
            )
        }

        val updates = mutableListOf<WebsiteHealthUpdate>()
        val reportItems = mutableListOf<HealthReportItem>()

        candidates.forEachIndexed { index, candidate ->
            currentCoroutineContext().ensureActive()
            val reportItem = when (val result = checkWebsiteHealthAction(candidate)) {
                is ActionResult.Success -> {
                    updates += result.value
                    HealthReportItem(
                        websiteId = candidate.websiteId,
                        title = candidate.title,
                        normalizedUrl = candidate.normalizedUrl,
                        status = result.value.status,
                        detailMessage = result.value.detailMessage,
                    )
                }
                is ActionResult.PartialSuccess -> {
                    issues += result.issues
                    updates += result.value
                    HealthReportItem(
                        websiteId = candidate.websiteId,
                        title = candidate.title,
                        normalizedUrl = candidate.normalizedUrl,
                        status = result.value.status,
                        detailMessage = result.value.detailMessage,
                    )
                }
                is ActionResult.Failure -> {
                    issues += result.issue
                    updates += WebsiteHealthUpdate(
                        websiteId = candidate.websiteId,
                        status = HealthStatus.UNKNOWN,
                        checkedAt = System.currentTimeMillis(),
                        detailMessage = result.issue.message,
                    )
                    HealthReportItem(
                        websiteId = candidate.websiteId,
                        title = candidate.title,
                        normalizedUrl = candidate.normalizedUrl,
                        status = HealthStatus.UNKNOWN,
                        detailMessage = result.issue.message,
                    )
                }
            }
            reportItems += reportItem
            if (updates.size >= HEALTH_PERSIST_BATCH_SIZE) {
                persistHealthStatusesUseCase(updates.toList())
                updates.clear()
            }
            onProgress(
                HealthCheckProgress(
                    totalCount = candidates.size,
                    completedCount = index + 1,
                    latestItem = reportItem,
                ),
            )
        }

        if (updates.isNotEmpty()) {
            persistHealthStatusesUseCase(updates)
        }

        val output = HealthCheckPipelineOutput(
            checkedCount = reportItems.size,
            okCount = reportItems.count { it.status == HealthStatus.OK },
            blockedCount = reportItems.count { it.status == HealthStatus.BLOCKED },
            redirectedCount = reportItems.count { it.status == HealthStatus.REDIRECTED },
            deadCount = reportItems.count { it.status == HealthStatus.DEAD },
            timeoutCount = reportItems.count { it.status == HealthStatus.TIMEOUT },
            loginRequiredCount = reportItems.count { it.status == HealthStatus.LOGIN_REQUIRED },
            dnsFailedCount = reportItems.count { it.status == HealthStatus.DNS_FAILED },
            sslIssueCount = reportItems.count { it.status == HealthStatus.SSL_ISSUE },
            items = reportItems,
        )
        return if (issues.isEmpty()) {
            ActionResult.Success(output)
        } else {
            ActionResult.PartialSuccess(output, issues)
        }
    }

    private companion object {
        const val HEALTH_PERSIST_BATCH_SIZE = 12
    }
}

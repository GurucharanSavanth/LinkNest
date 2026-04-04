package com.linknest.core.action.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.linknest.core.action.ActionResult
import com.linknest.core.action.pipeline.HealthCheckPipeline
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CheckWebsiteHealthWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthCheckPipeline: HealthCheckPipeline,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (runAttemptCount >= MAX_RETRIES) return Result.failure()
        val staleBefore = System.currentTimeMillis() - STALE_AFTER_MILLIS
        return when (healthCheckPipeline(staleBefore = staleBefore, limit = BACKGROUND_BATCH_LIMIT)) {
            is ActionResult.Failure -> Result.retry()
            else -> Result.success()
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
        const val STALE_AFTER_MILLIS = 12L * 60L * 60L * 1000L
        const val BACKGROUND_BATCH_LIMIT = 24
    }
}

package com.linknest.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.linknest.core.data.usecase.RefreshWebsiteMetadataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class MetadataRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val refreshWebsiteMetadataUseCase: RefreshWebsiteMetadataUseCase,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (runAttemptCount >= MAX_RETRIES) return Result.failure()
        return try {
            refreshWebsiteMetadataUseCase()
            Result.success()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}

package com.linknest.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.linknest.core.action.worker.CheckWebsiteHealthWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class HealthWorkScheduler @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    fun sync(enabled: Boolean) {
        if (enabled) {
            schedule()
        } else {
            cancel()
        }
    }

    fun schedule() {
        val workRequest = PeriodicWorkRequestBuilder<CheckWebsiteHealthWorker>(
            repeatInterval = 12,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build(),
        ).setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            30,
            TimeUnit.MINUTES,
        ).build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    fun cancel() {
        WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "linknest-health-check"
    }
}

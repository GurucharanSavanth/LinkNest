package com.linknest.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.disk.DiskCache
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.linknest.app.startup.DeferredStartupCoordinator
import com.linknest.core.data.storage.LinkNestStorage
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class LinkNestApplication : Application(), SingletonImageLoader.Factory, Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var deferredStartupCoordinator: DeferredStartupCoordinator

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this, 0.12)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(LinkNestStorage.imageLoaderCacheDirectory(this).toOkioPath())
                    .maxSizeBytes(32L * 1024L * 1024L)
                    .build()
            }
            .build()

    override fun onCreate() {
        super.onCreate()
        deferredStartupCoordinator.start(applicationScope)
    }

    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    }
}

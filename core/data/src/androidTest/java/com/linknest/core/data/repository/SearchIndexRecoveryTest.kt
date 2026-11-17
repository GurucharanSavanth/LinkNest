package com.linknest.core.data.repository

import android.util.Log
import androidx.appsearch.app.AppSearchBatchResult
import androidx.appsearch.app.AppSearchResult
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.localstorage.LocalStorage
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.linknest.core.database.LinkNestDatabase
import com.linknest.core.common.time.TimeProvider
import com.linknest.core.data.usecase.SearchUseCase
import com.linknest.core.network.UrlMetadataFetcher
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.*
import org.junit.Test

/** Uses an in-memory library and a uniquely named index in the library test package. */
class SearchIndexRecoveryTest {
    @Test fun partialBatchAfterClear_isRetriedBeforeReturningResults() = runBlocking {
        fixture { f ->
            f.seed(3)
            f.repository.warmIndex()
            f.failNextPut.set(true)
            try {
                f.repository.rebuildIndex()
                fail("A failed document must fail the explicit rebuild")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains("injected"))
            }
            assertTrue(f.repository.search("Link", 20).isEmpty())
            assertTrue(f.repository.suggestions("Link", 20).isEmpty())
            assertEquals("Partial namespaces must not be queried", 0, f.indexReads.get())
            f.nextRetry().second.complete(Unit)
            f.awaitRepairs()
            assertEquals(setOf(1L, 2L, 3L), f.repository.search("Link", 20).map { it.websiteId }.toSet())
        }
    }

    @Test fun rebuild_serializesQueuedWritesAndOverlappingRebuilds() = runBlocking {
        fixture { f ->
            f.seed(3)
            f.repository.warmIndex()
            coroutineScope {
                val gate = f.holdNext("putAsync")
                val rebuild = async(Dispatchers.IO) { f.repository.rebuildIndex() }
                gate.awaitEntered()
                try {
                    f.seedWebsite(4)
                    val write = async(start = CoroutineStart.UNDISPATCHED) { f.repository.indexWebsite(4) }
                    val secondRebuild = async(start = CoroutineStart.UNDISPATCHED) { f.repository.rebuildIndex() }
                    assertNull(withTimeoutOrNull(150) { write.await(); true })
                    assertNull(withTimeoutOrNull(150) { secondRebuild.await(); true })
                    assertEquals("Only the first rebuild may clear while its batch is unfinished", 2, f.clears.get())
                    gate.release()
                    withTimeout(10_000) { rebuild.await(); write.await(); secondRebuild.await() }
                } finally { gate.release() }
            }
            assertEquals(setOf(1L, 2L, 3L, 4L), f.repository.search("Link", 20).map { it.websiteId }.toSet())
        }
    }

    @Test fun deletionAndArchiveDuringRebuild_doNotResurrectResults() = runBlocking {
        fixture { f ->
            f.seed(3)
            f.repository.warmIndex()
            coroutineScope {
                val gate = f.holdNext("putAsync")
                val rebuild = async(Dispatchers.IO) { f.repository.rebuildIndex() }
                gate.awaitEntered()
                try {
                    withContext(Dispatchers.IO) {
                        f.database.openHelper.writableDatabase.execSQL("DELETE FROM website_entries WHERE id = 1")
                        f.database.openHelper.writableDatabase.execSQL("UPDATE categories SET is_archived = 1 WHERE id = 1")
                    }
                    val remove = async { f.repository.removeWebsite(1); f.repository.removeCategory(1) }
                    gate.release()
                    withTimeout(10_000) { rebuild.await(); remove.await() }
                } finally { gate.release() }
            }
            assertTrue(f.repository.search("Link", 20).isEmpty())
            assertEquals("Index recovery must not delete authoritative records", 2, f.database.websiteDao().getAllWebsites().size)
        }
    }

    @Test fun cancellationAfterClear_waitsForMutationAndReconcilesNextUse() = runBlocking {
        fixture { f ->
            f.seed(3)
            f.repository.warmIndex()
            coroutineScope {
                val gate = f.holdNext("removeAsync")
                val rebuild = async(Dispatchers.IO) { f.repository.rebuildIndex() }
                gate.awaitEntered()
                try {
                    rebuild.cancel()
                    assertFalse("Owner must remain held while backend mutation is unfinished", rebuild.isCompleted)
                } finally { gate.release() }
                withTimeout(10_000) { rebuild.join() }
                assertTrue(rebuild.isCancelled)
            }
            assertTrue(f.repository.search("Link", 20).isEmpty())
            f.nextRetry().second.complete(Unit)
            f.awaitRepairs()
            assertEquals(3, f.repository.search("Link", 20).size)
        }
    }

    @Test fun newOwner_reconcilesNonemptyButStaleNamespaces() = runBlocking {
        fixture { f ->
            f.seed(3)
            f.repository.warmIndex()
            f.seedWebsite(4) // Simulates Room committing before process death prevented its index update.
            val restarted = f.newRepository()
            restarted.warmIndex()
            assertEquals(setOf(1L, 2L, 3L, 4L), restarted.search("Link", 20).map { it.websiteId }.toSet())
        }
    }

    @Test fun rebuild_usesOneBulkIconReadAtEveryLibrarySize() = runBlocking {
        fixture { f ->
            for (size in listOf(100, 1_000, 5_000)) {
                f.seed(size)
                f.iconReads.set(0)
                val started = System.nanoTime()
                f.repository.rebuildIndex()
                val elapsedMs = (System.nanoTime() - started) / 1_000_000
                assertEquals("Icon query count must not grow with website count", 1, f.iconReads.get())
                val results = f.repository.search("Link", size + 1)
                assertEquals(size, results.size)
                assertEquals("file:///index-qa-icon.png", results.first { it.websiteId == 1L }.cachedIconUri)
                Log.i("LinkNestPerf", "indexRebuild size=$size iconReads=${f.iconReads.get()} elapsedMs=$elapsedMs")
            }
        }
    }

    @Test fun canceledRead_preservesReadyIndexWithoutSchedulingRepair() = runBlocking {
        fixture { f ->
            f.seed(3)
            f.repository.warmIndex()
            val clears = f.clears.get()
            f.cancelNextSearch.set(true)
            try { f.repository.search("Link", 20); fail("Read cancellation must propagate") }
            catch (expected: CancellationException) { assertEquals("injected read cancellation", expected.message) }
            assertEquals(3, f.repository.search("Link", 20).size)
            assertEquals("A canceled read must not trigger another rebuild", clears, f.clears.get())
            assertEquals(0, f.sessionCloses.get())
            assertTrue(f.retryDelays.tryReceive().isFailure)
            assertFalse(f.repairScope.coroutineContext[Job]!!.children.any())
        }
    }

    @Test fun failedStartup_coalescesAutomaticRepairWithoutAnotherQuery() = runBlocking {
        fixture { f ->
            f.seed(3)
            f.failPuts.set(3)
            f.repository.warmIndex()
            val retry = f.nextRetry()
            assertEquals(1_000L, retry.first)
            // More failed callers must share the already scheduled repair, rather than spawn jobs.
            f.repository.indexWebsite(1)
            f.repository.indexWebsite(2)
            assertTrue(f.retryDelays.tryReceive().isFailure)
            val gate = f.holdNext("putAsync")
            try {
                retry.second.complete(Unit)
                gate.awaitEntered()
            } finally { gate.release() }
            f.awaitRepairs()
            val clears = f.clears.get()
            assertEquals(4, clears)
            assertEquals(3, f.repository.search("Link", 20).size)
            assertEquals("The query must use the index repaired autonomously", clears, f.clears.get())
            assertTrue(f.retryDelays.tryReceive().isFailure)
        }
    }

    @Test fun persistentFailure_stopsAfterThreeAutomaticAttempts() = runBlocking {
        fixture { f ->
            f.seed(3)
            f.failPuts.set(4)
            f.repository.warmIndex()
            for (delay in listOf(1_000L, 2_000L, 4_000L)) {
                val retry = f.nextRetry()
                assertEquals(delay, retry.first)
                retry.second.complete(Unit)
            }
            f.awaitRepairs()
            assertEquals("One caller attempt plus three bounded background attempts", 4, f.putAttempts.get())
            assertEquals(4, f.sessionCreations.get())
            assertTrue(f.retryDelays.tryReceive().isFailure)
            assertFalse(f.repairScope.coroutineContext[Job]!!.children.any())
            assertEquals("Derived failures must preserve Room", 3, f.database.websiteDao().getAllWebsites().size)
            // A later read schedules a new bounded repair while returning Room fallback immediately.
            assertTrue(f.repository.search("Link", 20).isEmpty())
            f.nextRetry().second.complete(Unit)
            f.awaitRepairs()
            assertEquals(3, f.repository.search("Link", 20).size)
        }
    }

    @Test fun sessionReopen_serializesQueuedWrite() = runBlocking {
        fixture { f ->
            f.seed(3)
            f.repository.warmIndex()
            f.failNextPut.set(true)
            try { f.repository.rebuildIndex(); fail("Expected failed batch") }
            catch (expected: IllegalStateException) { assertTrue(expected.message.orEmpty().contains("injected")) }
            assertEquals(1, f.sessionCloses.get())
            // The queued automatic repair is paused; a foreground reopen owns the same mutex.
            f.nextRetry()
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            f.beforeSessionOpen = { entered.complete(Unit); release.await() }
            coroutineScope {
                val reopen = async(Dispatchers.IO) { f.repository.warmIndex() }
                withTimeout(10_000) { entered.await() }
                try {
                    val write = async(start = CoroutineStart.UNDISPATCHED) { f.repository.indexWebsite(1) }
                    assertNull(withTimeoutOrNull(150) { write.await(); true })
                    assertEquals("Only one replacement session may be opened", 1, f.sessionCreations.get())
                    release.complete(Unit)
                    withTimeout(10_000) { reopen.await(); write.await() }
                } finally { release.complete(Unit) }
            }
            assertEquals(2, f.sessionCreations.get())
            assertEquals(3, f.repository.search("Link", 20).size)
        }
    }

    @Test fun coldReads_returnFallbackWithoutCreatingSession_andCoalesceRepair() = runBlocking {
        fixture { f ->
            f.seed(3)
            val cold = f.newRepository(Dispatchers.Unconfined)
            coroutineScope {
                val search = async(start = CoroutineStart.UNDISPATCHED) { cold.search("Link", 20) }
                val suggestions = async(start = CoroutineStart.UNDISPATCHED) { cold.suggestions("Link", 20) }
                assertTrue("A cold read must return without awaiting a rebuild", search.isCompleted)
                assertTrue("Cold suggestions must return without awaiting a rebuild", suggestions.isCompleted)
                assertTrue(search.await().isEmpty())
                assertTrue(suggestions.await().isEmpty())
            }
            assertEquals(0, f.sessionCreations.get())
            assertEquals(0, f.indexReads.get())
            val retry = f.nextRetry()
            assertTrue("Both reads must share one repair job", f.retryDelays.tryReceive().isFailure)
            retry.second.complete(Unit)
            f.awaitRepairs()
            assertEquals(3, cold.search("Link", 20).size)
            assertEquals(1, f.sessionCreations.get())
        }
    }

    @Test fun readsDuringHeldRebuild_returnRoomResultsBeforeOwnerIsReleased() = runBlocking {
        fixture { f ->
            f.seed(3)
            val index = f.newRepository(Dispatchers.Unconfined)
            index.warmIndex()
            coroutineScope {
                val gate = f.holdNext("putAsync")
                val rebuild = async(Dispatchers.IO) { index.rebuildIndex() }
                gate.awaitEntered()
                try {
                    val read = async(start = CoroutineStart.UNDISPATCHED) { index.search("Link", 20) }
                    val suggestions = async(start = CoroutineStart.UNDISPATCHED) { index.suggestions("Link", 20) }
                    assertTrue("A busy owner must not suspend the read", read.isCompleted)
                    assertTrue("A busy owner must not suspend suggestions", suggestions.isCompleted)
                    assertTrue(read.await().isEmpty())
                    assertTrue(suggestions.await().isEmpty())
                    assertEquals("Never query an unfinished rebuild", 0, f.indexReads.get())
                    val results = withTimeout(10_000) { f.searchUseCase(index)("Link", 20) }
                    assertEquals(setOf(1L, 2L, 3L), results.map { it.websiteId }.toSet())
                    assertFalse("Room fallback must finish before the rebuild", rebuild.isCompleted)
                } finally { gate.release() }
                withTimeout(10_000) { rebuild.await() }
            }
            assertEquals(3, index.search("Link", 20).size)
            assertEquals(1, f.indexReads.get())
        }
    }

    private suspend fun fixture(block: suspend (Fixture) -> Unit) {
        val f = Fixture()
        try { block(f) } finally { f.close() }
    }

    private class Fixture {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        val iconReads = AtomicInteger()
        val database = Room.inMemoryDatabaseBuilder(context, LinkNestDatabase::class.java)
            .setQueryCallback(object : RoomDatabase.QueryCallback {
                override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                    if (sqlQuery.startsWith("SELECT", true) && sqlQuery.contains("FROM icon_cache", true)) iconReads.incrementAndGet()
                }
            }, Executor { it.run() }).build()
        private val name = "index-qa-${UUID.randomUUID()}"
        private val backends = mutableListOf<AppSearchSession>()
        val repairScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val retryDelays = Channel<Pair<Long, CompletableDeferred<Unit>>>(Channel.UNLIMITED)
        val failNextPut = AtomicBoolean()
        val cancelNextSearch = AtomicBoolean()
        val failPuts = AtomicInteger()
        val putAttempts = AtomicInteger()
        val indexReads = AtomicInteger()
        val clears = AtomicInteger()
        val sessionCreations = AtomicInteger()
        val sessionCloses = AtomicInteger()
        @Volatile var beforeSessionOpen: (suspend () -> Unit)? = null
        @Volatile private var held: Pair<String, Gate>? = null
        val repository = newRepository()

        fun newRepository(dispatcher: CoroutineDispatcher = Dispatchers.IO) = AppSearchBackedSearchIndexRepository(context, database, dispatcher,
            reconciliationScope = repairScope, retryDelay = { delay ->
                val permit = CompletableDeferred<Unit>()
                retryDelays.send(delay to permit)
                permit.await()
            }) {
            beforeSessionOpen?.invoke()
            sessionCreations.incrementAndGet()
            val actual = LocalStorage.createSearchSessionAsync(
                LocalStorage.SearchContext.Builder(context, name).build(),
            ).get(10, TimeUnit.SECONDS).also { backends += it }
            Proxy.newProxyInstance(AppSearchSession::class.java.classLoader, arrayOf(AppSearchSession::class.java)) { _, method, args ->
                if (method.name == "search") indexReads.incrementAndGet()
                if (method.name == "search" && cancelNextSearch.compareAndSet(true, false)) throw CancellationException("injected read cancellation")
                if (method.name == "close") sessionCloses.incrementAndGet()
                if (method.name == "putAsync") putAttempts.incrementAndGet()
                if (method.name == "putAsync" && (failNextPut.compareAndSet(true, false) || failPuts.getAndUpdate { (it - 1).coerceAtLeast(0) } > 0)) {
                    val request = args!![0] as PutDocumentsRequest
                    actual.putAsync(PutDocumentsRequest.Builder()
                        .addGenericDocuments(request.genericDocuments.take(1)).build()).get(10, TimeUnit.SECONDS)
                    return@newProxyInstance PendingFuture<AppSearchBatchResult<String, Void>>().apply {
                        complete(AppSearchBatchResult.Builder<String, Void>()
                            .setFailure("injected", AppSearchResult.RESULT_INTERNAL_ERROR, "injected batch failure").build())
                    }
                }
                if (method.name == "removeAsync" && args?.firstOrNull() is String) clears.incrementAndGet()
                val result = try { method.invoke(actual, *(args ?: emptyArray())) }
                catch (failure: InvocationTargetException) { throw failure.targetException }
                val gate = held
                if (gate != null && method.name == gate.first && result is ListenableFuture<*>) {
                    held = null
                    gate.second.attach(result)
                    gate.second.future
                } else result
            } as AppSearchSession
        }

        fun searchUseCase(index: SearchIndexRepository): SearchUseCase {
            val clock = object : TimeProvider { override fun now() = 1L }
            val websites = OfflineFirstWebsiteRepository(database, database.websiteDao(), database.iconCacheDao(),
                database.tagDao(), UrlMetadataFetcher(Dispatchers.IO), clock, index, Dispatchers.Default)
            val categories = OfflineFirstCategoryRepository(database, database.categoryDao(), database.websiteDao(),
                database.iconCacheDao(), database.tagDao(), clock, index, Dispatchers.Default)
            return SearchUseCase(websites, index, categories)
        }

        fun holdNext(method: String) = Gate().also { held = method to it }

        suspend fun nextRetry(): Pair<Long, CompletableDeferred<Unit>> = withTimeout(10_000) { retryDelays.receive() }

        suspend fun awaitRepairs() = withTimeout(10_000) {
            repairScope.coroutineContext[Job]!!.children.toList().forEach { it.join() }
        }

        suspend fun seed(size: Int) = withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase.execSQL(
                "INSERT OR IGNORE INTO categories VALUES (1, 'Inbox', '#112233', 'AUTO', NULL, 0, 0, 0, 1, 1)",
            )
            for (id in 1..size) seedWebsite(id)
            database.openHelper.writableDatabase.execSQL(
                "INSERT OR IGNORE INTO icon_cache VALUES (1, 1, NULL, 'file:///index-qa-icon.png', NULL, NULL, NULL, 1, 1)",
            )
        }

        suspend fun seedWebsite(id: Int) = withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase.execSQL("""INSERT OR IGNORE INTO website_entries
                (id, category_id, title, normalized_url, domain, chosen_icon_source, sort_order,
                 is_pinned, open_count, health_status, priority, follow_up_status, created_at, updated_at)
                VALUES (?, 1, ?, ?, 'example.com', 'GENERATED', ?, 0, 0, 'UNKNOWN', 'NORMAL', 'NONE', 1, 1)""",
                arrayOf<Any>(id, "Link $id", "https://example.com/$id", id))
        }

        suspend fun close() {
            held?.second?.release()
            repairScope.coroutineContext[Job]!!.cancelAndJoin()
            withContext(Dispatchers.IO) {
                // Every database name is owned by this fixture; no production index or records are touched.
                val session = LocalStorage.createSearchSessionAsync(
                    LocalStorage.SearchContext.Builder(context, name).build(),
                ).get(10, TimeUnit.SECONDS)
                try {
                    session.removeAsync("", androidx.appsearch.app.SearchSpec.Builder().build()).get(10, TimeUnit.SECONDS)
                } finally {
                    session.close()
                    backends.forEach { runCatching { it.close() } }
                    database.close()
                }
            }
        }
    }

    private class Gate {
        private val entered = CountDownLatch(1)
        val future = PendingFuture<Any?>()
        @Volatile private var value: Any? = null
        fun attach(backend: ListenableFuture<*>) {
            value = backend.get(10, TimeUnit.SECONDS)
            entered.countDown()
        }
        suspend fun awaitEntered() = withContext(Dispatchers.IO) {
            assertTrue("Index operation did not reach the barrier", entered.await(10, TimeUnit.SECONDS))
        }
        fun release() { future.complete(value) }
    }

    private class PendingFuture<T> : ListenableFuture<T> {
        private val delegate = CompletableFuture<T>()
        fun complete(value: T) { delegate.complete(value) }
        override fun addListener(listener: Runnable, executor: Executor) {
            delegate.whenComplete { _, _ -> executor.execute(listener) }
        }
        override fun cancel(mayInterruptIfRunning: Boolean) = delegate.cancel(mayInterruptIfRunning)
        override fun isCancelled() = delegate.isCancelled
        override fun isDone() = delegate.isDone
        override fun get(): T = delegate.get()
        override fun get(timeout: Long, unit: TimeUnit): T = delegate.get(timeout, unit)
    }
}

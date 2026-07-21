package com.linknest.core.data.usecase

import com.linknest.core.network.UrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandleIncomingShareUseCaseTest {
    private val useCase = HandleIncomingShareUseCase(UrlNormalizer())

    @Test
    fun extractsUrlFromNormalShare() {
        val result = useCase.invoke("Check this out: https://example.com/article")
        assertTrue(result.isSuccess)
        assertEquals("https://example.com/article", result.getOrThrow().normalizedUrl)
    }

    @Test
    fun boundsHugeSharedTextWithoutHanging() {
        // URL near the front, then ~2 MiB of filler. The scan is length-bounded, so this returns
        // promptly and still finds the URL instead of scanning megabytes on the caller's thread.
        val huge = "See https://example.com/x " + "lorem ipsum ".repeat(180_000)
        val result = useCase.invoke(huge)
        assertTrue(result.isSuccess)
        assertEquals("https://example.com/x", result.getOrThrow().normalizedUrl)
    }

    @Test
    fun blankShareFails() {
        assertTrue(useCase.invoke("   ").isFailure)
    }
}

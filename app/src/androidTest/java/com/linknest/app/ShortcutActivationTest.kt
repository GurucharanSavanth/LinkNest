package com.linknest.app

import android.content.Intent
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.app.shortcut.ShortcutPublisher
import org.junit.Assert.*
import org.junit.Test

class ShortcutActivationTest {
    @Test fun activation_isConsumedOnceAndRejectsMismatchedDestination() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val publisher = ShortcutPublisher(context)
        val intent = Intent(Intent.ACTION_VIEW, "linknest://search".toUri())
            .putExtra(ShortcutPublisher.EXTRA_SHORTCUT_ID, "shortcut_search")
        assertTrue(publisher.reportActivation(intent))
        assertFalse(publisher.reportActivation(intent))
        val invalid = Intent(Intent.ACTION_VIEW, "linknest://add".toUri())
            .putExtra(ShortcutPublisher.EXTRA_SHORTCUT_ID, "shortcut_health")
        assertFalse(publisher.reportActivation(invalid))
        assertNotNull(invalid.getStringExtra(ShortcutPublisher.EXTRA_SHORTCUT_ID))
    }
}

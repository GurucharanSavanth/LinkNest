package com.linknest.app

import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.app.widget.WidgetRemoteViewsFactory
import com.linknest.core.model.WidgetLink
import java.util.Locale
import kotlin.math.roundToInt
import org.junit.Assert.*
import org.junit.Test

/** Measures native RemoteViews content; launcher padding is outside the supplied content bounds. */
class WidgetLayoutBoundsTest {
    @Test fun collectionPreview_fitsHostHeight_withAccessibleTitleAndRows() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for ((fontScale, densityDpi) in listOf(1f to 480, 2f to 480, 1f to 220, 2f to 220)) {
                val context = configuredContext(fontScale, densityDpi)
                for (height in listOf(120, 170, 300)) {
                    for (count in listOf(0, 4)) {
                        val links = List(count) { index -> WidgetLink(index + 1L,
                            "A long website title that must remain one line $index", "https://example.com/$index") }
                        val views = WidgetRemoteViewsFactory.buildCollectionWidget(context,
                            "A category with a long name", "Latest links from your saved library",
                            "No links yet; open your complete library", links, "linknest://dashboard", height)
                        val root = measure(context, views, 220, height)
                        val title = root.findViewById<TextView>(R.id.widget_title)
                        val minimumTouch = (48 * context.resources.displayMetrics.density).roundToInt()
                        assertTrue("Title must open the complete library", title.isClickable)
                        assertTrue(title.height >= minimumTouch)
                        assertEquals(1, title.maxLines)
                        assertScaledTitle(context, title)
                        val rows = listOf(R.id.widget_item_1, R.id.widget_item_2, R.id.widget_item_3, R.id.widget_item_4)
                            .map { root.findViewById<TextView>(it) }.filter { it.visibility == View.VISIBLE }
                        assertTrue("At least one link or empty-state action must remain reachable", rows.isNotEmpty())
                        rows.forEach { row ->
                            assertTrue("Link rows need 48dp touch height", row.height >= minimumTouch)
                            assertTrue("Every visible row must remain actionable", row.isClickable)
                            assertEquals(1, row.maxLines)
                        }
                        assertChildrenWithinContent(root, "collection height=$height font=$fontScale density=$densityDpi count=$count")
                        if (height == 120) assertEquals(View.GONE, root.findViewById<View>(R.id.widget_subtitle).visibility)
                    }
                }
            }
        }
    }

    @Test fun quickAdd_fitsNarrowMinimum_andHidesHelperAtLargeText() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for ((fontScale, densityDpi) in listOf(1f to 480, 2f to 480, 1f to 220, 2f to 220)) {
                val context = configuredContext(fontScale, densityDpi)
                for (height in listOf(72, 120, 170, 300)) {
                    val views = WidgetRemoteViewsFactory.buildQuickAddWidget(context,
                        "Quick Add", "Save a website without browsing the full library", height)
                    val root = measure(context, views, 160, height)
                    assertTrue("The complete Quick Add widget remains actionable", root.isClickable)
                    assertTrue(root.height >= (48 * context.resources.displayMetrics.density).roundToInt())
                    val title = root.findViewById<TextView>(R.id.widget_title)
                    assertEquals(1, title.maxLines)
                    assertScaledTitle(context, title)
                    assertEquals(1, root.findViewById<TextView>(R.id.widget_body).maxLines)
                    assertChildrenWithinContent(root, "quick add height=$height font=$fontScale density=$densityDpi")
                    if (fontScale == 2f && height == 72) {
                        assertEquals(View.GONE, root.findViewById<View>(R.id.widget_body).visibility)
                    }
                }
            }
        }
    }

    private fun configuredContext(fontScale: Float, densityDpi: Int): Context {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            this.fontScale = fontScale
            this.densityDpi = densityDpi
            setLocale(Locale.ENGLISH)
        })
    }

    private fun measure(context: Context, views: RemoteViews, widthDp: Int, heightDp: Int): ViewGroup {
        val parent = FrameLayout(context)
        val root = views.apply(context, parent) as ViewGroup
        parent.addView(root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val density = context.resources.displayMetrics.density
        val width = (widthDp * density).roundToInt()
        val height = (heightDp * density).roundToInt()
        parent.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        parent.layout(0, 0, width, height)
        assertNotNull("Launcher-hosted content must keep its readable background", root.background)
        return root
    }

    private fun assertScaledTitle(context: Context, title: TextView) {
        assertEquals("Native title must use the requested font scale",
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15f, context.resources.displayMetrics), title.textSize, 0.5f)
    }

    private fun assertChildrenWithinContent(root: ViewGroup, description: String) {
        for (index in 0 until root.childCount) {
            val child = root.getChildAt(index)
            if (child.visibility != View.VISIBLE) continue
            assertTrue("$description child=${child.id} clips horizontally",
                child.left >= root.paddingLeft && child.right <= root.width - root.paddingRight)
            assertTrue("$description child=${child.id} clips vertically",
                child.top >= root.paddingTop && child.bottom <= root.height - root.paddingBottom)
            assertTrue("$description child=${child.id} must have visible bounds", child.width > 0 && child.height > 0)
        }
    }
}

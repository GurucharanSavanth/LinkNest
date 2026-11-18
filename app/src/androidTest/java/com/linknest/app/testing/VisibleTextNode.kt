package com.linknest.app.testing

import android.view.accessibility.AccessibilityNodeInfo

/** Virtual-view providers need not implement native find-by-text; inspect their exposed children. */
fun AccessibilityNodeInfo.visibleTextNode(value: String): AccessibilityNodeInfo? {
    if (isVisibleToUser && (text?.contains(value) == true || contentDescription?.contains(value) == true)) return this
    for (index in 0 until childCount) {
        getChild(index)?.visibleTextNode(value)?.let { return it }
    }
    return null
}

package com.linknest.core.common.time

import javax.inject.Inject

interface TimeProvider {
    fun now(): Long
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}

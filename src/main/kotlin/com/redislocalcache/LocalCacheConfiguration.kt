package com.redislocalcache

import java.time.Duration

data class LocalCacheConfiguration(
    val maximumSize: Long,
    val initialCapacity: Int,
    val expireAfterWrite: Duration?,
)

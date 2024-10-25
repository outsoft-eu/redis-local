package com.outsoft.redislocal

import java.time.Duration

data class LocalCacheConfiguration(
    val maximumSize: Long,
    val initialCapacity: Int,
    val expireAfterWrite: Duration?,
)

package com.redislocalcache.configuration.properties

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(value = "redis-local-cache")
data class RedisLocalCacheProperties(
    val configs: Map<String, CacheConfig>
) {

    data class CacheConfig(
        val name: String,
        val maximumLocalSize: Long,
        val initialLocalCapacity: Int,
        val expireAfterWrite: Duration?,
    )
}

package com.outsoft.redislocal

import com.github.benmanes.caffeine.cache.Cache
import com.google.common.util.concurrent.Striped
import io.lettuce.core.support.caching.CacheAccessor
import java.nio.ByteBuffer
import java.util.concurrent.locks.Lock
import kotlin.concurrent.withLock
import mu.KotlinLogging
import org.springframework.util.Assert

class CaffeineCacheAccessor(
    private val caffeineCache: Cache<Any, Any?>,
    private val cacheName: String,
    private val localCacheModificationLock: Striped<Lock>,
    private val keyDeserializer: (ByteBuffer) -> Any
) : Cache<Any, Any?> by caffeineCache, CacheAccessor<Any, Any?> {

    override fun get(key: Any?): Any? {
        return caffeineCache.getIfPresent(key)
    }

    override fun evict(key: Any) {
        Assert.isInstanceOf(ByteArray::class.java, key)
        val keyBytes = ByteBuffer.wrap(key as ByteArray)
        val deserializedKey = keyDeserializer.invoke(keyBytes)
        localCacheModificationLock.get(deserializedKey).withLock {
            log.trace { "Evict local cache [$cacheName] by key $deserializedKey" }
            caffeineCache.invalidate(keyBytes)
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

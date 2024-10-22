package com.redislocalcache

import com.github.benmanes.caffeine.cache.Cache
import com.google.common.util.concurrent.Striped
import io.lettuce.core.RedisClient
import io.lettuce.core.TrackingArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.support.caching.CacheFrontend
import io.lettuce.core.support.caching.ClientSideCaching
import java.util.concurrent.locks.Lock
import org.springframework.data.redis.cache.RedisCacheConfiguration

class RedisLocalCacheSynchronizer(
    cacheName: String,
    localCache: Cache<Any, Any?>,
    localCacheModificationLock: Striped<Lock>,
    redisCacheConfig: RedisCacheConfiguration,
    redisClient: RedisClient
) {
    private val cacheAccessor = CaffeineCacheAccessor(
        localCache,
        cacheName,
        localCacheModificationLock
    ) { redisCacheConfig.keySerializationPair.read(it) }

    private val cacheFrontendDelegate = lazy {
        ClientSideCaching.enable(
            cacheAccessor,
            redisClient.statefulRedisConnection(),
            TrackingArgs.Builder.enabled().prefixes(redisCacheConfig.getKeyPrefixFor(cacheName)).bcast()
        )
    }
    private val cacheFrontend: CacheFrontend<Any, Any?> by cacheFrontendDelegate

    private fun RedisClient.statefulRedisConnection(): StatefulRedisConnection<Any, Any?> =
        connect(ByteArrayCodec.INSTANCE) as StatefulRedisConnection<Any, Any?>

    fun start() {
        cacheFrontend
    }

    fun stop() {
        if (cacheFrontendDelegate.isInitialized()) {
            cacheFrontend.close()
        }
    }
}

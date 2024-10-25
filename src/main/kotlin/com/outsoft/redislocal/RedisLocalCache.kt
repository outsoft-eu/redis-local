package com.outsoft.redislocal

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.util.concurrent.Striped
import io.lettuce.core.RedisClient
import java.nio.ByteBuffer
import java.util.concurrent.locks.Lock
import mu.KotlinLogging
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheWriter


/**
 * Redis-based cache that contains a local cache as a front proxy.
 *
 * Cache keeps connection to a Redis and listening an updates for a specific keys which makes it possible to update
 * the local cache if the corresponding values have been changed or removed on the Redis side.
 *
 * Use [RedisLocalCacheManager] to create RedisLocalCache instances.
 */
class RedisLocalCache(
    name: String,
    redisClient: RedisClient,
    cacheWriter: RedisCacheWriter,
    redisCacheConfig: RedisCacheConfiguration,
    localCacheConfig: LocalCacheConfiguration
) : RedisCache(name, cacheWriter, redisCacheConfig) {

    private val localCache: Cache<Any, Any?> = Caffeine.newBuilder()
        .maximumSize(localCacheConfig.maximumSize)
        .initialCapacity(localCacheConfig.initialCapacity)
        .expireAfterWrite(localCacheConfig.expireAfterWrite)
        .build()

    private val localCacheModificationLock: Striped<Lock> = Striped.lazyWeakLock(DEF_STRIPES_NUMBER)
    private val redisLocalCacheSynchronizer = RedisLocalCacheSynchronizer(
        cacheName = name,
        localCache = localCache,
        localCacheModificationLock = localCacheModificationLock,
        redisCacheConfig = redisCacheConfig,
        redisClient = redisClient
    )

    private val log = KotlinLogging.logger {}

    /**
     * Tries to find a value in a local cache first, if it is null - then performs a search in the Redis.
     * If the value found the local cache will be updated then.
     */
    override fun lookup(key: Any): Any? {
        // init sync between Redis and local cache on the first lookup
        redisLocalCacheSynchronizer.start()

        val serializedKey = getSerializedCacheKey(key)
        return localCache.getIfPresent(serializedKey)
            ?.takeUnless { it == RACE_CONDITION_PROTECT_LOCAL_CACHE_VALUE }
            ?.also { log.trace { "Found value by key $key in Redis local cache [$name]: $it" } }
            ?: lookupServer(serializedKey, key)
    }

    private fun lookupServer(serializedKey: ByteBuffer?, key: Any): Any? {
        localCache.put(serializedKey, RACE_CONDITION_PROTECT_LOCAL_CACHE_VALUE)
        return super.lookup(key)
            ?.also { updateLocalCache(serializedKey, key, it) }
            ?: run {
                localCache.invalidate(serializedKey)
                null
            }
    }

    private fun updateLocalCache(serializedKey: ByteBuffer?, key: Any, value: Any) {
        // Skip the local cache update if the lock have been acquired on eviction operation
        // So if eviction started a bit before the updating started, the last one will be skipped
        // If updating started a bit before the eviction started, the last one will be waiting and executed after the updating done.
        val lock = localCacheModificationLock.get(key)
        if (lock.tryLock()) {
            try {
                if (localCache.getIfPresent(serializedKey) == RACE_CONDITION_PROTECT_LOCAL_CACHE_VALUE) {
                    localCache.put(serializedKey, value)
                    log.trace { "Update Redis local cache [$name] with pair [key=$key, value=$value]" }
                }
            } finally {
                lock.unlock()
            }
        } else {
            log.trace {
                "Skip update Redis local cache [$name] with pair [key=$key, value=$value] " +
                    "as the value was updated or evicted on the server side"
            }
        }
    }

    /**
     * Overrides parent method in order to evict a local cache by key. It is not necessary as eventually the local cache
     * will be evicted on the event from the Redis (see [CaffeineCacheAccessor.evict]), but helps to prevent situation
     * when a read operation, which called immediately after the eviction, returns a value:
     *
     * ```
     *  cache.evict(key)
     *  cache.get(key) // still can return the value despite eviction
     * ```
     */
    override fun evict(key: Any) {
        super.evict(key)
        localCache.invalidate(getSerializedCacheKey(key))
        log.trace { "Both Redis and local cache [$name] evicted by key $key" }
    }

    /**
     * Overrides parent method in order to clear a local cache. It is not necessary as eventually the local cache
     * will be cleared on the event from the Redis (see [CaffeineCacheAccessor.evict]), but helps to prevent situation
     * when a read operation, which called immediately after the cleanup, returns a value:
     *
     * ```
     *  cache.clear()
     *  cache.get(key) // still can return the value despite clearing
     * ```
     */
    override fun clear() {
        super.clear()
        localCache.invalidateAll()
        log.trace { "Both Redis and local cache [$name] cleared" }
    }

    fun stopRedisLocalSync() {
        redisLocalCacheSynchronizer.stop()
        log.trace { "Sync for Local cache [$name] stopped" }
    }

    fun clearLocalCache() {
        localCache.invalidateAll()
        log.trace { "Local cache [$name] cleared" }
    }

    private fun getSerializedCacheKey(key: Any): ByteBuffer =
        ByteBuffer.wrap(super.serializeCacheKey(createCacheKey(key)))

    companion object {
        const val RACE_CONDITION_PROTECT_LOCAL_CACHE_VALUE = "RACE_CONDITION_PROTECT"
        private const val DEF_STRIPES_NUMBER = 100
    }
}

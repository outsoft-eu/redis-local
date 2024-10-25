package com.outsoft.redislocal

import io.lettuce.core.RedisChannelHandler
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisConnectionStateListener
import mu.KotlinLogging
import org.springframework.context.SmartLifecycle
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory

/**
 * Extended [RedisCacheManager] that creates [RedisLocalCache] instead of common [RedisCache]
 */
class RedisLocalCacheManager(
    private val lettuceConnectionFactory: LettuceConnectionFactory,
    private val redisLocalCacheConfigurationProvider: RedisLocalCacheConfigurationProvider,
    private val cacheWriter: RedisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(lettuceConnectionFactory)
) : RedisCacheManager(
    cacheWriter,
    redisLocalCacheConfigurationProvider.defaultRedisCacheConfiguration,
    redisLocalCacheConfigurationProvider.initialRedisCacheConfigurations,
    redisLocalCacheConfigurationProvider.allowInFlightCacheCreation
), SmartLifecycle {

    private val log = KotlinLogging.logger {}

    private var running = false

    override fun createRedisCache(name: String, cacheConfig: RedisCacheConfiguration?): RedisCache {
        log.debug { "Creating Redis local cache [$name]" }
        return RedisLocalCache(
            name,
            redisClient(),
            cacheWriter,
            cacheConfig ?: redisLocalCacheConfigurationProvider.defaultRedisCacheConfiguration,
            redisLocalCacheConfigurationProvider.getLocalCacheConfiguration(name)
        )
            .also { log.debug { "Redis local cache [$name] created" } }
    }

    override fun afterPropertiesSet() {
        // disable cache initializing on 'post construct' stage as it cause "stuck" when connecting to Redis
        // Initialization moved to SmartLifeCycle#start function
    }

    override fun start() {
        log.debug { "Initialize caches" }
        running = true
        redisClient().addListener(localCachesInvalidatorListener())
        initializeCaches()
    }

    private fun redisClient() = (lettuceConnectionFactory.nativeClient as RedisClient)

    /**
     * Invalidates local caches when the Redis client disconnected from server
     * in order to avoid keeping potentially outdated data.
     */
    private fun localCachesInvalidatorListener() = object : RedisConnectionStateListener {
        override fun onRedisDisconnected(connection: RedisChannelHandler<*, *>?) {
            log.debug { "Disconnected from Redis. Start clearing all local caches" }
            forEachRedisLocalCache { it.clearLocalCache() }
        }
    }

    override fun stop() {
        log.debug { "Close client side caching for all caches" }
        closeClientSideCachingForAllCaches()
        running = false
    }

    private fun closeClientSideCachingForAllCaches() =
        forEachRedisLocalCache { it.stopRedisLocalSync() }

    private fun forEachRedisLocalCache(action: (RedisLocalCache) -> Unit) = cacheNames
        .mapNotNull { getCache(it) }
        .filterIsInstance<RedisLocalCache>()
        .forEach(action)

    override fun isRunning(): Boolean = running

    override fun getPhase(): Int = START_PHASE

    companion object {
        private const val START_PHASE = Integer.MAX_VALUE - 1001
        const val NAME = "redisLocalCacheManager"
        const val CACHE_RESOLVER_NAME = "propertyBasedRedisLocalCacheResolver"
    }
}

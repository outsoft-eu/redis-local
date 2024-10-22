package com.redislocalcache.helpers

import com.redislocalcache.RedisLocalCacheManager
import java.io.Serializable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class CacheableTestService(
    @Autowired(required = false) val cacheValueProvider: CacheValueProvider?
) {

    @Cacheable("\${redisLocalCache.configs.testCache.name}", cacheResolver = RedisLocalCacheManager.CACHE_RESOLVER_NAME)
    fun getValueFromTestCache(key: String): CacheValue? =
        cacheValueProvider?.get(key)

    @CachePut("\${redisLocalCache.configs.testCache.name}", cacheResolver = RedisLocalCacheManager.CACHE_RESOLVER_NAME)
    fun putValueToTestCache(key: String): CacheValue? =
        cacheValueProvider?.get(key)

    @Cacheable(
        "\${redisLocalCache.configs.testCacheLowTtl.name}",
        cacheResolver = RedisLocalCacheManager.CACHE_RESOLVER_NAME
    )
    fun getValueFromTestCacheLowTtl(key: String): CacheValue? =
        cacheValueProvider?.get(key)

    @CacheEvict(
        "\${redisLocalCache.configs.testCache.name}",
        cacheResolver = RedisLocalCacheManager.CACHE_RESOLVER_NAME
    )
    fun evictTestCache(key: String) {
        // noop
    }

    @CacheEvict(
        "\${redisLocalCache.configs.testCache.name}",
        cacheResolver = RedisLocalCacheManager.CACHE_RESOLVER_NAME,
        allEntries = true
    )
    fun evictTestCacheAllEntries(key: String) {
        // noop
    }

    @CacheEvict(
        "\${redisLocalCache.configs.testCacheSameName.name}",
        cacheResolver = RedisLocalCacheManager.CACHE_RESOLVER_NAME
    )
    fun evictTestCacheSameName(key: String) {
        // noop
    }
}

data class CacheValue(val val1: String = "val1", val val2: Int = 2) : Serializable

interface CacheValueProvider {
    fun get(key: String): CacheValue
}

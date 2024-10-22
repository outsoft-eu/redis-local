package com.redislocalcache

import com.redislocalcache.configuration.properties.RedisLocalCacheProperties
import java.time.Duration
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.cache.CacheProperties
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext

class RedisLocalCacheConfigurationProvider(
    val redisLocalCacheProperties: RedisLocalCacheProperties,
    private val cacheProperties: CacheProperties,
    private val classLoader: ClassLoader,
    private val redisCacheConfigurationProvider: ObjectProvider<RedisCacheConfiguration>,
    val allowInFlightCacheCreation: Boolean = true
) {

    val defaultRedisCacheConfiguration: RedisCacheConfiguration = buildDefaultRedisCacheConfiguration()
    val initialRedisCacheConfigurations: Map<String, RedisCacheConfiguration> = buildInitialRedisCacheConfigurations()

    fun getLocalCacheConfiguration(name: String): LocalCacheConfiguration =
        redisLocalCacheProperties.configs.values
            .firstOrNull { it.name == name }
            ?.toLocalCacheConfiguration()
            ?: buildDefaultLocalCacheConfiguration()

    private fun buildDefaultRedisCacheConfiguration(): RedisCacheConfiguration =
        redisCacheConfigurationProvider.getIfAvailable {
            val redisProperties = cacheProperties.redis
            var config = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        JdkSerializationRedisSerializer(
                            classLoader
                        )
                    )
                )

            if (redisProperties.timeToLive != null) {
                config = config.entryTtl(redisProperties.timeToLive)
            }
            if (redisProperties.keyPrefix != null) {
                config = config.prefixCacheNameWith(redisProperties.keyPrefix)
            }
            if (!redisProperties.isCacheNullValues) {
                config = config.disableCachingNullValues()
            }
            if (!redisProperties.isUseKeyPrefix) {
                config = config.disableKeyPrefix()
            }
            config
        }

    private fun buildInitialRedisCacheConfigurations(): Map<String, RedisCacheConfiguration> =
        redisLocalCacheProperties.configs.values
            .associate { it.name to copyDefaultRedisCacheConfiguration(it.expireAfterWrite) }

    private fun copyDefaultRedisCacheConfiguration(ttl: Duration? = null): RedisCacheConfiguration {
        return ttl?.let { defaultRedisCacheConfiguration.entryTtl(it) } ?: defaultRedisCacheConfiguration
    }

    private fun buildDefaultLocalCacheConfiguration() =
        LocalCacheConfiguration(DEF_LOCAL_CACHE_MAX_SIZE, DEF_LOCAL_CACHE_INITIAL_CAPACITY, null)

    private fun RedisLocalCacheProperties.CacheConfig.toLocalCacheConfiguration() =
        LocalCacheConfiguration(maximumLocalSize, initialLocalCapacity, expireAfterWrite)

    companion object {
        const val DEF_LOCAL_CACHE_MAX_SIZE = 500L
        const val DEF_LOCAL_CACHE_INITIAL_CAPACITY = 100
    }
}

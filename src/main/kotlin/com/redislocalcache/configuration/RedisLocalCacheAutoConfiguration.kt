package com.redislocalcache.configuration

import com.redislocalcache.PropertyBasedCacheResolver
import com.redislocalcache.RedisLocalCacheConfigurationProvider
import com.redislocalcache.RedisLocalCacheManager
import com.redislocalcache.configuration.properties.RedisLocalCacheProperties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.cache.CacheProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.CacheResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory

@AutoConfiguration
@ConditionalOnProperty("redisLocalCache.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(value = [RedisLocalCacheProperties::class, CacheProperties::class])
@Import(value = [LettuceKeepAliveConfiguration::class, LoggingCacheErrorHandlerConfiguration::class])
@EnableCaching
class RedisLocalCacheAutoConfiguration {

    @Bean(RedisLocalCacheManager.CACHE_RESOLVER_NAME)
    fun propertyBasedRedisLocalCacheResolver(
        @Qualifier(RedisLocalCacheManager.NAME) redisCacheManager: CacheManager,
        environment: Environment
    ): CacheResolver = PropertyBasedCacheResolver(redisCacheManager, environment)

    @Bean(RedisLocalCacheManager.NAME)
    fun redisLocalCacheManager(
        cacheProperties: CacheProperties,
        lettuceConnectionFactory: LettuceConnectionFactory,
        redisCacheConfiguration: ObjectProvider<RedisCacheConfiguration>,
        resourceLoader: ResourceLoader,
        redisLocalCacheProperties: RedisLocalCacheProperties
    ): RedisCacheManager = RedisLocalCacheManager(
        lettuceConnectionFactory,
        RedisLocalCacheConfigurationProvider(
            redisLocalCacheProperties,
            cacheProperties,
            resourceLoader.classLoader!!,
            redisCacheConfiguration
        )
    )
}

package com.outsoft.redislocal.configuration

import com.outsoft.redislocal.LoggingCacheErrorHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("redisLocalCache.suppressCacheErrors", havingValue = "true", matchIfMissing = false)
class LoggingCacheErrorHandlerConfiguration : CachingConfigurer {

    override fun errorHandler(): CacheErrorHandler = LoggingCacheErrorHandler()
}

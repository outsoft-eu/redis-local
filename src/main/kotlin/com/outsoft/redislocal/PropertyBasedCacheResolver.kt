package com.outsoft.redislocal

import org.springframework.cache.CacheManager
import org.springframework.cache.interceptor.CacheOperationInvocationContext
import org.springframework.cache.interceptor.SimpleCacheResolver
import org.springframework.core.env.PropertyResolver

class PropertyBasedCacheResolver(
    cacheManager: CacheManager,
    private val propertyResolver: PropertyResolver
) : SimpleCacheResolver(cacheManager) {

    override fun getCacheNames(context: CacheOperationInvocationContext<*>): Collection<String> =
        super.getCacheNames(context).map { propertyResolver.resolveRequiredPlaceholders(it) }
}

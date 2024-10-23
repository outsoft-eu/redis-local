package com.redislocalcache

import mu.KotlinLogging
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.CacheErrorHandler

class LoggingCacheErrorHandler : CacheErrorHandler {
    private val log = KotlinLogging.logger {}

    override fun handleCacheGetError(exception: RuntimeException, cache: Cache, key: Any) {
        log.error("Error on getting value by key $key from cache ${cache.name}", exception)
    }

    override fun handleCachePutError(exception: RuntimeException, cache: Cache, key: Any, value: Any?) {
        log.error("Error on putting value $value by key $key to cache ${cache.name}", exception)
    }

    override fun handleCacheEvictError(exception: RuntimeException, cache: Cache, key: Any) {
        log.error("Error on evicting cache ${cache.name} by key $key", exception)
    }

    override fun handleCacheClearError(exception: RuntimeException, cache: Cache) {
        log.error("Error on clearing cache ${cache.name}", exception)
    }
}
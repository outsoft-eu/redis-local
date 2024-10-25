package com.outsoft.redislocal

import com.ninjasquad.springmockk.MockkBean
import com.outsoft.redislocal.configuration.properties.RedisLocalCacheProperties
import com.outsoft.redislocal.helpers.CacheValue
import com.outsoft.redislocal.helpers.CacheValueProvider
import com.outsoft.redislocal.helpers.CacheableTestService
import com.outsoft.redislocal.helpers.sleep
import com.outsoft.redislocal.helpers.waitUntilAssertion
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
class RedisLocalCacheTest {

    @Value("\${redisLocalCache.configs.testCacheLowTtl.name}")
    private lateinit var testCacheLowTtlName: String

    @MockkBean
    private lateinit var cacheValueProvider: CacheValueProvider

    @Autowired
    private lateinit var redisLocalCacheProperties: RedisLocalCacheProperties

    @Autowired
    private lateinit var cacheableTestService: CacheableTestService

    @Autowired
    @Qualifier(RedisLocalCacheManager.NAME)
    private lateinit var cacheManager: CacheManager

    @AfterEach
    fun cleanup() {
        cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
    }

    @Test
    fun `should cache value after the first call`() {
        // given
        val key = "k1"
        val value = CacheValue("ww", 22)
        every { cacheValueProvider.get(key) } returns value

        // when
        cacheableTestService.getValueFromTestCache(key)
        val cachedValue = cacheableTestService.getValueFromTestCache(key)

        // then
        verify(exactly = 1) { cacheValueProvider.get(key) }
        assertThat(cachedValue).isEqualTo(value)
    }

    @Test
    fun `should evict cache and return new value on the next call`() {
        // given
        val key = "k2"
        val value = CacheValue()
        val newValue = CacheValue("a", 22)
        every { cacheValueProvider.get(key) } returns value andThen newValue

        // when
        // repeat several times in order to init local cache
        repeat(LOCAL_CACHE_INIT_AFTER) { cacheableTestService.getValueFromTestCache(key) }

        cacheableTestService.evictTestCache(key)
        val nextCallResult = cacheableTestService.getValueFromTestCache(key)

        // then
        verify(exactly = 2) { cacheValueProvider.get(key) }
        assertThat(nextCallResult).isEqualTo(newValue)
    }

    @Test
    fun `should update cache and return new value on the next call`() {
        // given
        val key = "k22"
        val value = CacheValue()
        val newValue = CacheValue("123", 2211)
        every { cacheValueProvider.get(key) } returns value andThen newValue

        // when
        repeat(LOCAL_CACHE_INIT_AFTER) { cacheableTestService.getValueFromTestCache(key) }

        cacheableTestService.putValueToTestCache(key)
        waitUntilAssertion {
            val nextCallResult = cacheableTestService.getValueFromTestCache(key)

            // then
            verify(exactly = 2) { cacheValueProvider.get(key) }
            assertThat(nextCallResult).isEqualTo(newValue)
        }
    }

    @Test
    fun `should evict cache all entries and return new value on the next call for each key`() {
        // given
        val value1 = CacheValue("k1")
        val newValue1 = CacheValue("k11", 22)
        val value2 = CacheValue("k2")
        val newValue2 = CacheValue("k22", 111)
        every { cacheValueProvider.get(value1.val1) } returns value1 andThen newValue1
        every { cacheValueProvider.get(value2.val1) } returns value2 andThen newValue2

        // when
        repeat(LOCAL_CACHE_INIT_AFTER) { cacheableTestService.getValueFromTestCache(value1.val1) }
        repeat(LOCAL_CACHE_INIT_AFTER) { cacheableTestService.getValueFromTestCache(value2.val1) }

        cacheableTestService.evictTestCacheAllEntries("")
        val nextCallResult1 = cacheableTestService.getValueFromTestCache(value1.val1)
        val nextCallResult2 = cacheableTestService.getValueFromTestCache(value2.val1)

        // then
        verify(exactly = 2) { cacheValueProvider.get(value1.val1) }
        verify(exactly = 2) { cacheValueProvider.get(value2.val1) }
        assertThat(nextCallResult1).isEqualTo(newValue1)
        assertThat(nextCallResult2).isEqualTo(newValue2)
    }

    @Test
    fun `should expire cache value after the ttl passed`() {
        // given
        val key = "k3"
        val value = CacheValue()
        val newValue = CacheValue("11", 11)
        every { cacheValueProvider.get(key) } returns value andThen newValue

        // when
        repeat(LOCAL_CACHE_INIT_AFTER) { cacheableTestService.getValueFromTestCacheLowTtl(key) }
        val ttl = redisLocalCacheProperties.configs[testCacheLowTtlName]?.expireAfterWrite?.toMillis() ?: 0
        sleep(ttl + 10)
        val nextCallResult = cacheableTestService.getValueFromTestCacheLowTtl(key)

        // then
        verify(exactly = 2) { cacheValueProvider.get(key) }
        assertThat(nextCallResult).isEqualTo(newValue)
    }

    @Test
    fun `should evict all caches with the same name and return new value on the next call`() {
        // given
        val key = "k4"
        val value = CacheValue()
        val newValue = CacheValue("new", 44)
        every { cacheValueProvider.get(key) } returns value andThen newValue

        // when
        // repeat 3 times in order to init local cache
        repeat(LOCAL_CACHE_INIT_AFTER) { cacheableTestService.getValueFromTestCache(key) }

        cacheableTestService.evictTestCacheSameName(key)
        val nextCallResult = cacheableTestService.getValueFromTestCache(key)

        // then
        verify(exactly = 2) { cacheValueProvider.get(key) }
        assertThat(nextCallResult).isEqualTo(newValue)
    }

    companion object {
        const val LOCAL_CACHE_INIT_AFTER = 3
    }
}

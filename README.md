# redis-local-cache

This is a solution for maintaining a local in-memory cache that minimizes the need to access Redis for every read operation. It ensures synchronization between the local cache and Redis, so that any changes made by one instance are immediately reflected across all other instances. This results in reduced latency, lower load on Redis, and consistent data across distributed systems.

## Features

- 🚀 **In-Memory Caching**: Keep a local copy of cache in memory for fast read access.
- 🔄 **Redis Synchronization**: Synchronize cache updates across multiple instances.
- 📉 **Reduced Latency**: Avoid frequent round trips to Redis by reading from the local cache.
- ⚙️ **Automatic Updates**: Automatically update the local cache when changes are detected in Redis.
- 💡 **Easy Integration**: Integrates seamlessly with existing Redis-based applications.

## Installation

To add this library to your project, use one of the following methods:

For Maven:
```xml
<dependency>
    <groupId>com</groupId>
    <artifactId>redis-local-cache</artifactId>
    <version>1.0.0</version>
</dependency>
```

For Gradle:

```groovy
implementation 'com:redis-local-cache:1.0.0'
```

## Configuration
It can be configured using environment variables or configuration files to set up Redis connections and other cache properties:

### Configuration Parameters

- **enabled**: Enables or disables the local caching feature. Set to `true` to use the redis local caching mechanism. Enabled by default.

- **suppressCacheErrors**: If set to `true`, errors related to cache operations will be suppressed. This can be useful for graceful degradation in case of Redis downtime. Disabled by default.

- **configs**: Contains configurations for different caches.

    - **testCache**:
        - **name**: The name of the cache, used for annotations in your service methods.
        - **expireAfterWrite**: The duration after which the cache entry will expire (e.g., `PT2S` means 2 seconds).
        - **maximumLocalSize**: The maximum number of entries allowed in the local cache.
        - **initialLocalCapacity**: The initial capacity of the local cache.

```yaml
redisLocalCache:
  enabled: true
  suppressCacheErrors: true
  configs:
    testCacheLowTtl:
      name: testCacheLowTtl
      expireAfterWrite: PT2S        
      maximumLocalSize: 10           
      initialLocalCapacity: 1        
    testCache:
      name: testCache
      expireAfterWrite: PT1H        
      maximumLocalSize: 10           
      initialLocalCapacity: 1        
```

## Usage

Example of usage with Spring's caching annotations to interact with the local cache:

```kotlin
@Service
class CacheableTestService(
    private val valueProvider: ValueProvider?
) {

    @Cacheable("${redisLocalCache.configs.testCache.name}", cacheResolver = RedisLocalCacheManager.CACHE_RESOLVER_NAME)
    fun getValueFromTestCache(key: String): CacheValue? =
        valueProvider?.get(key)

    @CachePut("${redisLocalCache.configs.testCache.name}", cacheResolver = RedisLocalCacheManager.CACHE_RESOLVER_NAME)
    fun putValueToTestCache(key: String): CacheValue? =
        valueProvider?.get(key)

    @CacheEvict("${redisLocalCache.configs.testCache.name}", cacheResolver = RedisLocalCacheManager.CACHE_RESOLVER_NAME)
    fun evictTestCache(key: String) {
        // noop
    }
}
```

## Keep-Alive Configuration for Redis

### Configuration Parameters

- **enabled**: A Boolean flag that enables or disables the keep-alive functionality. Set to `true` to enable. Enabled by default.

- **connectionIdleBeforeCheck**: Specifies the duration to wait before checking for idle connections. The default value is `PT30S`(means 30 seconds).

- **probesCount**: Maximum number of keepalive probes TCP should send before dropping the connection. The default value is `3`.

- **probesInterval**: Defines the interval between each probe. The default value is `PT2S` (means 2 seconds).


```yaml
spring:
  data:
    redis:
      socket:
        keep-alive:
          enabled: true                             
          connectionIdleBeforeCheck: PT30S
          probesCount: 3                    
          probesInterval: PT2S 

```

### Dependencies

The following versions of dependencies are used in this lib:

- **Spring Boot**: `3.0.4`
- **Java**: `1.7.22`
- **Kotlin**: `1.7.22`
- **Guava**: `32.1.3-jre`
- **Caffeine**: `3.1.8`
- **Kotlin Logger**: `3.0.5`

## Testing

To run the tests for this project, use the following command:

```bash
./gradlew test
```

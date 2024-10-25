package com.outsoft.redislocal.config

import org.testcontainers.containers.GenericContainer

object RedisTestContainerConfig {
    private const val IMAGE_VERSION = "library/redis:6.0.9"
    private const val EXPOSED_PORT = 6379

    val container = startContainer()

    private fun startContainer() = RedisContainer().withExposedPorts(EXPOSED_PORT).also {
        it.start()
        System.setProperty("redis.url", it.host)
        System.setProperty("redis.port", it.firstMappedPort.toString())
    }

    class RedisContainer : GenericContainer<RedisContainer>(IMAGE_VERSION)
}

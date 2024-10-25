package com.outsoft.redislocal.configuration.properties

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(value = "spring.data.redis.socket.keep-alive")
data class RedisKeepaliveOptionProperties(
    val enabled: Boolean = true,
    val connectionIdleBeforeCheck: Duration = Duration.ofSeconds(30),
    val probesCount: Int = 3,
    val probesInterval: Duration = Duration.ofSeconds(2)
)

package com.redislocalcache.configuration

import com.redislocalcache.configuration.properties.RedisKeepaliveOptionProperties
import io.lettuce.core.ClientOptions
import io.lettuce.core.SocketOptions
import io.lettuce.core.TimeoutOptions
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("spring.data.redis.socket.keep-alive.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(value = [RedisKeepaliveOptionProperties::class])
class LettuceKeepAliveConfiguration {

    @Bean
    fun lettuceSocketKeepAliveCustomizer(
        redisProperties: RedisProperties,
        redisKeepaliveOptionProperties: RedisKeepaliveOptionProperties
    ): LettuceClientConfigurationBuilderCustomizer =
        LettuceClientConfigurationBuilderCustomizer { builder: LettuceClientConfigurationBuilder ->
            builder.clientOptions(createClientOptions(redisProperties, redisKeepaliveOptionProperties))
        }

    private fun createClientOptions(
        redisProperties: RedisProperties,
        redisKeepaliveOptionProperties: RedisKeepaliveOptionProperties
    ): ClientOptions {
        val socketOptionsBuilder = SocketOptions.builder()
        redisProperties.connectTimeout?.let { socketOptionsBuilder.connectTimeout(it) }
        return ClientOptions.builder()
            .socketOptions(
                socketOptionsBuilder
                    .keepAlive(
                        SocketOptions.KeepAliveOptions.builder()
                            .enable(redisKeepaliveOptionProperties.enabled)
                            .idle(redisKeepaliveOptionProperties.connectionIdleBeforeCheck)
                            .count(redisKeepaliveOptionProperties.probesCount)
                            .interval(redisKeepaliveOptionProperties.probesInterval)
                            .build()
                    )
                    .build()
            )
            .timeoutOptions(TimeoutOptions.enabled())
            .build()
    }
}

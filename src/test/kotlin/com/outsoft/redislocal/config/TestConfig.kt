package com.outsoft.redislocal.config

import org.springframework.context.annotation.Configuration

@Configuration
class TestConfig {

    companion object {
        init {
            RedisTestContainerConfig.container
        }
    }
}

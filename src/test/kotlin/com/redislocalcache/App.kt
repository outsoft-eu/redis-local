package com.redislocalcache

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication(proxyBeanMethods = false)
@EnableCaching
class App

@SuppressWarnings("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<App>(*args)
}

package com.redislocalcache.helpers

import java.time.Duration
import java.util.concurrent.TimeUnit
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.withPollInterval

private const val DEFAULT_WAIT_SECONDS = 2L
private const val DEFAULT_POLL_INTERVAL_MS = 200L

fun waitUntilAssertion(
    waitSeconds: Long = DEFAULT_WAIT_SECONDS,
    pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
    assertion: () -> Unit
) {
    await atMost Duration.ofSeconds(waitSeconds) withPollInterval
        Duration.ofMillis(pollIntervalMs) untilAsserted assertion
}

fun sleep(timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) {
    unit.sleep(timeout)
}

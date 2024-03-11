package org.acme

import io.quarkus.logging.Log
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.RequestScoped
import java.util.concurrent.atomic.AtomicInteger


@RequestScoped
class DbSource {
    private var isAcquired = false

    fun acquire() {
        Log.info("Acquire...")
        val maxAttempts = 3
        for (i in 0 until maxAttempts) {
            if (DbSource.Companion.counter.decrementAndGet() < 0) {
                DbSource.Companion.counter.incrementAndGet()
                try {
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
                continue
            }
            isAcquired = true
            Log.info("Acquire OK!" + DbSource.Companion.counter.get())
            return
        }
        throw RuntimeException("Could not acquire on time")
    }

    fun release() {
        Log.info("Release")
        if (!isAcquired) throw RuntimeException("Bad release")
        isAcquired = false
        DbSource.Companion.counter.incrementAndGet()
    }

    // SUPER IMPORTANT: called by Quarkus when the scope ends (request scope)
    @PreDestroy
    fun endOfRequest() {
        Log.warn("endOfRequest called")
        if (isAcquired) release()
    }

    companion object {
        private val counter = AtomicInteger(2)
    }
}

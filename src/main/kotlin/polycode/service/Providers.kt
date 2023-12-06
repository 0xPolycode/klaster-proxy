package polycode.service

import io.micrometer.core.instrument.util.NamedThreadFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

interface FixedScheduler {
    fun scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit)
    fun shutdown()
}

interface ScheduledExecutorServiceProvider {
    fun newSingleThreadScheduledExecutor(threadPrefix: String): FixedScheduler
}

@Service
class DefaultScheduledExecutorServiceProvider : ScheduledExecutorServiceProvider {
    override fun newSingleThreadScheduledExecutor(threadPrefix: String): FixedScheduler =
        object : FixedScheduler {
            private val executor = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory(threadPrefix))

            override fun scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit) {
                executor.scheduleAtFixedRate(command, initialDelay, period, unit)
            }

            override fun shutdown() = executor.shutdown()
        }
}

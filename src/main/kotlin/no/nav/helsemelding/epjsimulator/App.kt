package no.nav.helsemelding.epjsimulator

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.result
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import arrow.resilience.Schedule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.netty.Netty
import io.ktor.utils.io.CancellationException
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import no.nav.helsemelding.epjsimulator.plugin.configureMetrics
import no.nav.helsemelding.epjsimulator.plugin.configureRoutes
import no.nav.helsemelding.epjsimulator.processor.MessagesProcessor
import no.nav.helsemelding.epjsimulator.util.coroutineScope

private val log = KotlinLogging.logger {}

fun main() = SuspendApp {
    result {
        resourceScope {
            val deps = dependencies()

            server(
                Netty,
                port = config().server.port.value,
                preWait = config().server.preWait,
                module = epjSimulatorModule(deps.meterRegistry)
            )

            val messagesProcessor = MessagesProcessor(deps.ediAdapterClient)

            scheduleProcessMessages(messagesProcessor)

            awaitCancellation()
        }
    }
        .onFailure { error -> if (error !is CancellationException) logError(error) }
}

internal fun epjSimulatorModule(
    meterRegistry: PrometheusMeterRegistry
): Application.() -> Unit {
    return {
        configureMetrics(meterRegistry)
        configureRoutes(meterRegistry)
    }
}

private suspend fun ResourceScope.scheduleProcessMessages(processor: MessagesProcessor) {
    val scheduleConfig = config().apprec
    if (!scheduleConfig.enabled) {
        return
    }
    val scope = coroutineScope(currentCoroutineContext())
    Schedule
        .spaced<Unit>(scheduleConfig.interval)
        .repeat { processor.processMessages(scope) }
}

private fun logError(t: Throwable) = log.error { "Shutdown epj-simulator due to: ${t.stackTraceToString()}" }

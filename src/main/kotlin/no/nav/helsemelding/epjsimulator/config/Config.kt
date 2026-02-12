package no.nav.helsemelding.epjsimulator.config

import kotlin.time.Duration

data class Config(
    val server: Server,
    val ediAdapter: EdiAdapter,
    val apprec: Apprec
)

data class Server(
    val port: Port,
    val preWait: Duration
)

data class EdiAdapter(
    val scope: Scope
) {
    @JvmInline
    value class Scope(val value: String)
}

data class Apprec(
    val enabled: Boolean,
    val interval: Duration
)

@JvmInline
value class Port(val value: Int)

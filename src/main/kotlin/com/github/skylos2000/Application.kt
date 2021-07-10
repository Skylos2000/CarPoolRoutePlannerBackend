package com.github.skylos2000

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.github.skylos2000.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureSecurity()
        configureSerialization()
    }.start(wait = true)
}

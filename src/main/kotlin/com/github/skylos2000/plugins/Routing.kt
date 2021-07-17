package com.github.skylos2000.plugins

import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.configureRouting() {
    install(Locations)
}

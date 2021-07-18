package com.github.skylos2000

import com.github.skylos2000.plugins.getUsername
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.Database

fun Application.initRoutes(db: Database) {
    routing {
        authenticate("auth-basic") {
            get("/example/") {
                call.respondText("Hello, ${call.getUsername()}")
            }

            get("/example/json") {
                call.respond(/* Any serializable object can be placed here */ mapOf(
                    "a" to 1,
                    "b" to 2
                ))
            }
        }
    }
}
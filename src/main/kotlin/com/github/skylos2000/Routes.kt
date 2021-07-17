package com.github.skylos2000

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.Database

fun Application.initRoutes(db: Database) {
    routing {
        get("/example/") {
            call.respondText("Hello")
        }

        get("/example/json") {
            call.respond(/* Any serializable object can be placed here */ mapOf(
                "a" to 1,
                "b" to 2
            ))
        }
    }
}
package com.github.skylos2000

import com.github.skylos2000.db.*
import com.github.skylos2000.plugins.configureRouting
import com.github.skylos2000.plugins.configureSecurity
import com.github.skylos2000.plugins.configureSerialization
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.selects.select
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    val db = initDb()

    configureRouting()
    configureSecurity(db)
    configureSerialization()

    initRoutes(db)

    routing {
        get("/") {
            val rr = transaction(db) {
                User1.selectAll().toList()
            }
            //println(rr[0][User1.Email])
            call.respondText(rr[0][User1.Email])
        }
    }
}
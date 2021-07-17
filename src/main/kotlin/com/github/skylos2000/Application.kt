package com.github.skylos2000

import com.github.skylos2000.db.initDb
import com.github.skylos2000.plugins.configureRouting
import com.github.skylos2000.plugins.configureSecurity
import com.github.skylos2000.plugins.configureSerialization
import io.ktor.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    configureRouting()
    configureSecurity()
    configureSerialization()

    val db = initDb()
    initRoutes(db)
}
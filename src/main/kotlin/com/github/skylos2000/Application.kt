package com.github.skylos2000

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.skylos2000.db.*
import com.github.skylos2000.plugins.configureRouting
import com.github.skylos2000.plugins.configureSecurity
import com.github.skylos2000.plugins.configureSerialization
import com.github.skylos2000.plugins.getLoggedInUser
//import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type.Text
import com.github.skylos2000.routes.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.locations.*
import io.ktor.server.netty.*

fun main(args: Array<String>) = EngineMain.main(args)

@KtorExperimentalLocationsAPI
fun Application.module(testing: Boolean = false) {
    val db = initDb()

    configureRouting()
    configureSecurity(db)
    configureSerialization()

    initRoutes(db)

    initGroupRoutes(db)
    initMiscRoutes(db)
    initVotingRoutes(db)
    initUserRoutes(db)
    initGroupInviteRoutes(db)
}
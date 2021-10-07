package com.github.skylos2000

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.skylos2000.db.*
import com.github.skylos2000.plugins.configureRouting
import com.github.skylos2000.plugins.configureSecurity
import com.github.skylos2000.plugins.configureSerialization
import com.github.skylos2000.plugins.getLoggedInUser
//import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type.Text
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.selects.select
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Frame
import java.util.*
import kotlin.collections.LinkedHashSet

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    val db = initDb()

    configureRouting()
    configureSerialization()
    configureSecurity(db)

    initRoutes(db)

    routing {
        get("/") {
            /*val rr = transaction(db) {
                User1.selectAll().toList()
            }
            call.respondText(rr[0][User1.Email])*/
            call.respondText("Hello World!")
        }
    }

    install(WebSockets)
    routing{
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/dm"){
            println("Adding user!")
            val thisConnection = Connection(this)
            connections += thisConnection
            try {
                send("You are connected! There are ${connections.count()} users here.")
                for (frame in incoming) {
                    frame as? io.ktor.http.cio.websocket.Frame.Text ?: continue
                    val recievedText = frame.readText()

                    val textWithUsername = "[${thisConnection.name}]: $recievedText"
                    connections.forEach {
                        it.session.send(textWithUsername)
                    }
                }
            }
            catch (e: Exception) {
                println(e.localizedMessage)
            }
            finally {
                println("Removing $thisConnection!")
                connections -= thisConnection
            }
        }
    }
}
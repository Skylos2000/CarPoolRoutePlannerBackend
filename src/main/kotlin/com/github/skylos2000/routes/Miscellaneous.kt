package com.github.skylos2000.routes

import com.github.skylos2000.db.User1
import com.github.skylos2000.plugins.getLoggedInUser
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun Application.initMiscRoutes(db: Database) {
    routing {
        authenticate("auth-jwt") {
            post("/set_my_pickup_location_by_text") {
                val me = call.getLoggedInUser()!!
                // Should be in form: <double lat>,<double long>
                val params = call.receiveText()

                val lat = params.substringBefore(',').toDouble()
                val long = params.substringAfter(',').toDouble()

                transaction(db) {
                    User1.update({ User1.Uid eq me.id }) {
                        it[Default_Pickup_Lat] = lat
                        it[Default_Pickup_Long] = long
                    }
                }
                call.respondText("Success")
            }
        }
    }
}
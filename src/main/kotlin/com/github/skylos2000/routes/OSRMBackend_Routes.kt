package com.github.skylos2000.routes

import com.github.skylos2000.GetGroupMembersLocation
import com.github.skylos2000.GetUserPickups
import com.github.skylos2000.db.Group1
import com.github.skylos2000.db.Group_Destinations
import com.github.skylos2000.db.Group_Membership
import com.github.skylos2000.db.User1
import com.github.skylos2000.plugins.getLoggedInUser
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


@KtorExperimentalLocationsAPI
@Location("/optimize_route/{groupId}")
data class OptimizeRoute(val groupId: Int)


fun Application.initOSRMRoutes(db: Database) {
    routing {
        authenticate("auth-jwt") {
            get<OptimizeRoute> {
                // Pull group destinations from db *** done
                // parse destinations into string format for OSRM("lat,long;lat,long;")
                // call osrm trip service using parsed string
                // parse returned object and send back in call respond

                val dest_points = transaction(db) {
                    val groupDestinations = Group_Destinations.select { Group_Destinations.Group_id eq it.groupId }
                        .map { Pair(it[Group_Destinations.Destination_Lat], it[Group_Destinations.Destination_Long]) }

                    groupDestinations
                }






                call.respond(transaction(db) {

                    // formatted OSRM response goes here <---------
                    val response = "WIP"
                    response

                })
            }
        }
    }
}
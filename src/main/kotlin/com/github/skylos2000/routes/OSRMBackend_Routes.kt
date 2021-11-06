package com.github.skylos2000.routes

import com.github.skylos2000.db.Group_Destinations
import com.github.skylos2000.getTripService
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction


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

                var dest_points = transaction(db) {
                    val groupDestinations = Group_Destinations.select { Group_Destinations.Group_id eq it.groupId }
                        .map { Pair(it[Group_Destinations.Destination_Lat], it[Group_Destinations.Destination_Long]) }

                    groupDestinations
                }

                val labels = transaction(db) {
                    Group_Destinations.select { Group_Destinations.Group_id eq it.groupId }
                        .map { Pair(it[Group_Destinations.Label], Pair(it[Group_Destinations.Destination_Lat], it[Group_Destinations.Destination_Long]))  }
                }

                val optimized_points = getTripService(dest_points).map { Pair(it.getLabel(labels), Pair(it.location[1], it.location[0])) }


                call.respond(optimized_points)
            }
        }
    }
}
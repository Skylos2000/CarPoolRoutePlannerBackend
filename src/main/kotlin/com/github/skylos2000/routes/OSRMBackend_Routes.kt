package com.github.skylos2000.routes

import com.github.skylos2000.GroupDestination
import com.github.skylos2000.db.Group_Destinations
import com.github.skylos2000.getTripService
import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.pow
import kotlin.math.sqrt

@KtorExperimentalLocationsAPI
fun Application.initOSRMRoutes(db: Database) {
    routing {
//        authenticate("auth-jwt") {
            @KtorExperimentalLocationsAPI
            @Location("/optimize_route/{groupId}")
            data class OptimizeRoute(val groupId: Int)
            get<OptimizeRoute> {
                // Pull group destinations from db *** done
                // parse destinations into string format for OSRM("lat,long;lat,long;")
                // call osrm trip service using parsed string
                // parse returned object and send back in call respond

                val destPoints = transaction(db) {
                    val groupDestinations = Group_Destinations.select { Group_Destinations.Group_id eq it.groupId }
                        .map { Pair(it[Group_Destinations.Destination_Lat], it[Group_Destinations.Destination_Long]) }

                    groupDestinations
                }

                val destinations = transaction(db) {
                    Group_Destinations.select { Group_Destinations.Group_id eq it.groupId }
                        .map { GroupDestination(
                            it[Group_Destinations.Destination_id],
                            it[Group_Destinations.Group_id],
                            it[Group_Destinations.Destination_Lat],
                            it[Group_Destinations.Destination_Long],
                            it[Group_Destinations.Label],
                            it[Group_Destinations.OrderNum],
                        ) }.sortedBy { it.orderNum }
                }

                val optimizedPoints = getTripService(destPoints).map { osrmWaypoint ->
                    val osrmLat = osrmWaypoint.location[1]
                    val osrmLong = osrmWaypoint.location[0]

                    destinations.minByOrNull { groupDestination ->
                        sqrt((osrmLat - groupDestination.lat).pow(2) + (osrmLong - groupDestination.long).pow(2))
                    }
                }

                call.respond(optimizedPoints)
            }
        }
    }
//}
package com.github.skylos2000.routes

import com.github.skylos2000.GroupDestination
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
import org.jetbrains.exposed.sql.update

@KtorExperimentalLocationsAPI
fun Application.initOSRMRoutes(db: Database) {
    routing {
        authenticate("auth-jwt") {
            @KtorExperimentalLocationsAPI
            @Location("/optimize_route/{groupId}")
            data class OptimizeRoute(val groupId: Int)
            get<OptimizeRoute> {
                // Pull group destinations from db *** done
                // parse destinations into string format for OSRM("lat,long;lat,long;")
                // call osrm trip service using parsed string
                // parse returned object and send back in call respond

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

                val optimizedPoints = getTripService(destinations)

                // Update orderNums in database
                transaction {
                    optimizedPoints.forEach { serialDestination ->
                        Group_Destinations.update({ Group_Destinations.Destination_id eq serialDestination.destinationId }) { dbDestination ->
                            dbDestination[OrderNum] = serialDestination.orderNum
                        }
                    }
                }

                call.respond(optimizedPoints)
            }
        }
    }
}
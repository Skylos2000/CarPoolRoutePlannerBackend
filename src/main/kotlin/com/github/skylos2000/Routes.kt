package com.github.skylos2000

import com.github.skylos2000.plugins.getLoggedInUser
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.Database

// http://ipaddress/list_members_of_group/123
@Location("/example/{groupId}")
data class ExampleLocation(val groupId: Int)

fun Application.initRoutes(db: Database) {
    routing {
        authenticate("auth-basic") {
            get("/example/what_is_my_name/") {
                call.respondText(call.getLoggedInUser()?.username ?: "no one")
            }

            get("/example/my_pickup_coords") {
                val loggedInUser = call.getLoggedInUser() ?: return@get

                // Any serializable object can be placed here
                call.respond(Pair(
                    loggedInUser.defaultPickupLatitude,
                    loggedInUser.defaultPickupLongitude
                ))
            }

            get<ExampleLocation> {
                call.respondText(it.groupId.toString())
            }
        }
    }
}
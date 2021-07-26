package com.github.skylos2000

import com.github.skylos2000.db.*
import com.github.skylos2000.plugins.getLoggedInUser
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

// http://ipaddress/example/123
@KtorExperimentalLocationsAPI
@Location("/get_group_members/{groupId}")
data class GetGroupMembersLocation(val groupId: Int)

@KtorExperimentalLocationsAPI
@Location("/get_user_info/{userId}")
data class GetUserInfoLocation(val userId: Int)

@KtorExperimentalLocationsAPI
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

            get<GetGroupMembersLocation> {
                // Get members of given group
                call.respond(transaction(db) {
                    val gid = Group1.select { Group1.Group_ID eq it.groupId }.first()[Group1.Group_ID]

                    (User1 innerJoin Group_Membership).slice(User1.Uid)
                        .select { Group_Membership.Gid eq gid }
                        .map { it[User1.Uid] }
                })
            }

            get<GetUserInfoLocation> {
                val rowUser = getUserFromResultRow(
                    transaction(db) {
                        User1.select { User1.Uid eq it.userId }.first()
                    }
                )

                call.respond(rowUser.copy(password = ""))
            }
        }
    }
}
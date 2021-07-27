package com.github.skylos2000

import com.github.skylos2000.db.*
import com.github.skylos2000.plugins.getLoggedInUser
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

// http://ipaddress/example/123
@KtorExperimentalLocationsAPI
@Location("/get_group_members/{groupId}")
data class GetGroupMembersLocation(val groupId: Int)

@KtorExperimentalLocationsAPI
@Location("/get_user_info/{userId}")
data class GetUserInfoLocation(val userId: Int)

@KtorExperimentalLocationsAPI
@Location("/get_user_groups/{userId}")
data class GetUserGroups(val userId: Int)

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
                        User1.select { User1.Uid eq it.userId }.single()
                    }
                )

                call.respond(rowUser.copy(password = ""))
            }

            get<GetUserGroups> {
                val me = call.getLoggedInUser()!!
                if (me.id == it.userId) {
                    //call.respond()
                }
            }

            post("/set_my_pickup_location") {
                val params = call.receiveParameters()
                val me = call.getLoggedInUser()!!

                transaction(db) {
                    User1.update({ User1.Uid eq me.id }) {
                        it[Default_Pickup_Lat] = params["newDefaultLatitude"]!!.toDouble()
                        it[Default_Pickup_Long] = params["newDefaultLongitude"]!!.toDouble()
                    }
                }
            }
        }

        post("/signup") {
            val params = call.receiveParameters()

            transaction(db) {
                User1.insert {
                    it[Username] = params["username"]!!
                    it[Password] = params["password"]!!
                    it[Email] = params["email"]!!
                }
            }

            call.respondText("Added user ${params["username"]} to db")
        }
    }
}
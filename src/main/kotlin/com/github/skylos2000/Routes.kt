package com.github.skylos2000

import com.github.skylos2000.db.*
import com.github.skylos2000.plugins.getLoggedInUser
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

// http://ipaddress/example/123
@KtorExperimentalLocationsAPI
@Location("/get_group_members/{groupId}")
data class GetGroupMembersLocation(val groupId: Int)

@KtorExperimentalLocationsAPI
@Location("/get_group_routes/{groupId}")
data class GetUserPickups(val groupId: Int)

@KtorExperimentalLocationsAPI
fun Application.initRoutes(db: Database) {
    routing {
        authenticate("auth-basic") {
            get<GetGroupMembersLocation> {
                // Get members of given group
                call.respond(transaction(db) {
                    val gid = Group1.select { Group1.Group_ID eq it.groupId }.first()[Group1.Group_ID]

                    (User1 innerJoin Group_Membership).slice(User1.Uid)
                        .select { Group_Membership.Gid eq gid }
                        .map { it[User1.Uid] }
                })
            }

            get<GetUserPickups> {
                call.respond(transaction(db) {
                    val userDestinations = Group_Membership
                        .select { Group_Membership.Gid eq it.groupId }
                        .map {
                            Pair(it[Group_Membership.User_Lat], it[Group_Membership.User_Long])
                        }

                    val groupDestinations = Group_Destinations.select { Group_Destinations.Group_id eq it.groupId }
                        .map { Pair(it[Group_Destinations.Destination_Lat], it[Group_Destinations.Destination_Long]) }

                    // (userDestinations + groupDestinations).joinToString("|") { "${it.first},${it.second}" }
                    groupDestinations
                })
            }
        }

        post("/signup_text/") {
            val (username, password, email) = call.receiveText().split(",")

            transaction(db) {
                User1.insert {
                    it[Username] = username
                    it[Password] = password
                    it[Email] = email
                }
            }

            call.respondText("Added user $username to db")
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


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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

// http://ipaddress/example/123
@KtorExperimentalLocationsAPI
@Location("/get_group_members/{groupId}")
data class GetGroupMembersLocation(val groupId: Int)

@KtorExperimentalLocationsAPI
@Location("/get_user_info/{userId}")
data class GetUserInfoLocation(val userId: Int)

@KtorExperimentalLocationsAPI
@Location("/get_group_routes/{groupId}")
data class GetUserPickups(val groupId: Int)

@KtorExperimentalLocationsAPI
@Location("/set_group_destination/{groupId}")
data class SetGroupDestinationLocation(val groupId: Int, val newLat: Double, val newLong: Double, val label: String)

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
                call.respondText(
                    loggedInUser.defaultPickupLatitude.toString() + ", " +
                            loggedInUser.defaultPickupLongitude.toString()

                    /*Pair(
                        loggedInUser.defaultPickupLatitude,
                        loggedInUser.defaultPickupLongitude
                    )*/
                )
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

            get("/list_my_groups/") {
                val me = call.getLoggedInUser()!!
                call.respond(
                    transaction(db) {
                        Group_Membership
                            .select { Group_Membership.Uid eq me.id }
                            .map { row -> row[Group_Membership.Gid] }
                    }
                )
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

            get<SetGroupDestinationLocation> { pathParams ->
                transaction(db) {
                    Group_Destinations.deleteWhere { Group_Destinations.Group_id eq pathParams.groupId }
                    Group_Destinations.insert {
                        it[Group_id] = pathParams.groupId
                        it[Destination_Lat] = pathParams.newLat
                        it[Destination_Long] = pathParams.newLong
                        it[Label] = pathParams.label
                    }
                }

                call.respondText("Success")
            }

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

            post("/set_my_pickup_location") {
                // TODO: Figure out how to use this on the frontend
                //val params = call.receiveParameters()
                val params = call.receiveText()
                val me = call.getLoggedInUser()!!
                println("Posting is possible")
                println(params)
                /*transaction(db) {
                    val
                            update = User1.update({ User1.Uid eq me.id }) {
                        it[Default_Pickup_Lat] = params["newDefaultLatitude"]!!.toDouble()
                        it[Default_Pickup_Long] = params["newDefaultLongitude"]!!.toDouble()
                    }
                }*/
            }

            post("/create_group/") {
                val me = call.getLoggedInUser()!!

                transaction(db) {
                    val insertStatment = Group1.insert {
                        it[isTemp] = false
                        it[group_leader] = me.id
                    }
                    //val group = insertStatment.resultedValues!!.first()
//                    /*Group1.select{ Group1.Group_ID }.andWhere { Group1.group_leader eq me.id }
//                    Group1.max()
//                    Group1.max(Group_ID)*/

                    //val largestGid = Group1.select { Group1.Group_ID.max }
                    //val groupNum1 = Group1.select { Group1.Group_ID eq largestGid }.first()[Group1.Group_ID]
                    val groupNum1 = Group1.select{Group1.group_leader eq me.id}.toList()
                    //val dumbass = groupNum1.max()

                    Group_Membership.insert {
                        it[Gid] = groupNum1.last()[Group1.Group_ID]
                        it[Uid] = me.id
                        it[User_Lat] = me.defaultPickupLatitude
                        it[User_Long] = me.defaultPickupLongitude
                    }
                }
            }



            post("/submit_location") {
                val (group_id, lat, long, priority, label) = call.receiveText().split(",")

                transaction(db) {
                    Group_Destinations.insert {
                        it[Group_id] = group_id.toInt()
                        it[Destination_Lat] = lat.toDouble()
                        it[Destination_Long] = long.toDouble()
                        it[isPriority] = priority.toBoolean()
                        it[Label] = label
                    }
                }
                call.respondText("Success")
            }

            post("/remove_location"){
                val (group_id, lat, long) = call.receiveText().split(",")

                transaction(db){
                    Group_Destinations.deleteWhere {
                        (Group_Destinations.Group_id eq group_id.toInt()) and
                                (Group_Destinations.Destination_Lat eq lat.toDouble()) and
                                (Group_Destinations.Destination_Long eq long.toDouble())
                    }
                }
            }

            post("/delete_group"){
                val group_id = call.receiveText().toInt()

                transaction(db) {
                    Group1.deleteWhere { Group1.Group_ID eq group_id }
                }
                call.respondText { "Success" }
            }
            
            get("/join_group/{inviteCode}") {
                val inviteCode = call.parameters["inviteCode"]
                var message = "EMPTY"
                val me = call.getLoggedInUser()!!

                transaction(db){
                    val gid = GroupInvites.select{GroupInvites.InviteId eq inviteCode.toString()}.map{
                        it[GroupInvites.Gid]
                    }
                    if(gid.count() > 0)
                    {
                        message = (gid[0].toString())
                        val mid  =  Group_Membership.select{(Group_Membership.Gid eq gid[0]) and (Group_Membership.Uid eq me.id)}.map {
                        it[Group_Membership.Uid]
                        }

                        if(mid.count()==0) {
                        Group_Membership.insert{
                            it[Gid] = gid[0]
                            it[Uid] = me.id
                            it[User_Lat] = 0.0
                            it[User_Long] = 0.0
                        }
                        }
                    }
                }

                call.respondText(message)
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

        post("/create_invite/") {
            val params = call.receiveParameters()
            val gid = params["gid"]
            val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            val randomString = (1..8)
                .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
                .map(charPool::get)
                .joinToString("");
            if (gid == null) {
                call.respondText("Error: No Gid Mentioned")
            } else {
                transaction(db) {
                    GroupInvites.insert {
                        it[Gid] = gid.toInt()
                        it[InviteId] = randomString
                    }
                }
                val respond = application.locations.href("/invite/$randomString")
                call.respondText(respond)
            }
        }
    }
}

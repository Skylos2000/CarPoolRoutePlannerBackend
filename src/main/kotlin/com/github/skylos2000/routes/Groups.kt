package com.github.skylos2000.routes

import com.github.skylos2000.GroupDestination
import com.github.skylos2000.db.*
import com.github.skylos2000.plugins.me
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.locations.post as locationsPost

@KtorExperimentalLocationsAPI
fun Application.initGroupRoutes(db: Database) {
    routing {
        authenticate("auth-jwt") {
            // Create group
            @KtorExperimentalLocationsAPI
            @Location("/groups/create")
            data class CreateGroupLocation(val groupLabel: String = "")
            locationsPost<CreateGroupLocation> { pathParams ->
                transaction(db) {
                    Group1.insert {
                        it[label] = pathParams.groupLabel
                        it[group_leader] = me.id
                    }
                }
                call.respond(HttpStatusCode.OK)
            }

            // Delete group
            @KtorExperimentalLocationsAPI
            @Location("/groups/{groupId}/delete")
            data class DeleteGroupLocation(val groupId: Int)
            locationsPost<DeleteGroupLocation> { pathParams ->
                val groupDoesNotExist = transaction(db) {
                    Group1.select { Group1.Group_ID eq pathParams.groupId }
                        .empty()
                }

                if (groupDoesNotExist) {
                    call.respond(HttpStatusCode.NotFound, "Group does not exist")
                    return@locationsPost
                }

                // Only the group leader has permission to delete
                val userHasPermissionToDelete = transaction(db) {
                    val group = Group1.select { Group1.Group_ID eq pathParams.groupId }
                        .first()

                    return@transaction group[Group1.group_leader] == me.id
                }

                if (userHasPermissionToDelete) {
                    transaction(db) {
                        Group1.deleteWhere { Group1.Group_ID eq pathParams.groupId }
                    }
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.Forbidden, "User is not the group leader")
                }
            }

            // Add destination
            @KtorExperimentalLocationsAPI
            @Location("/groups/{groupId}/add_destinations")
            data class AddGroupDestinationsLocation(val groupId: Int)
            locationsPost<AddGroupDestinationsLocation> { pathParams ->
                val destinations = call.receive<List<GroupDestination>>()

                // TODO: This is a duplicate of the delete group route so this should probably be put in some kind of function
//                val groupDoesNotExist = transaction(db) {
//                    Group1.select { Group1.Group_ID eq pathParams.groupId }
//                        .empty()
//                }
//
//                if (groupDoesNotExist) {
//                    call.respond(HttpStatusCode.NotFound, "Group does not exist")
//                    return@locationsPost
//                }

                // Only the group leader has permission
                val userHasPermission = transaction(db) {
                    val group = Group1.select { Group1.Group_ID eq pathParams.groupId }
                        .first()

                    return@transaction group[Group1.group_leader] == me.id
                }
                // End of duplicate code

                if (userHasPermission) {
                    transaction(db) {
                        destinations.forEach { destination ->
                            Group_Destinations.insert { row ->
                                row[Group_id] = pathParams.groupId
                                row[Destination_Lat] = destination.lat
                                row[Destination_Long] = destination.long
                                row[Label] = destination.label
                                row[Destination_id] = destination.destinationId
                                row[OrderNum] = destination.orderNum
                            }
                        }
                    }
                }
                call.respond(HttpStatusCode.OK)
            }

            // Remove destination
            @KtorExperimentalLocationsAPI
            @Location("/groups/{groupId}/destinations/{destinationId}/delete")
            data class DeleteDestinationLocation(val groupId: Int, val destinationId: Int)
            locationsPost<DeleteDestinationLocation> {
                transaction(db) {
                    Group_Destinations.deleteWhere {
                        (Group_Destinations.Destination_id eq it.destinationId) and (Group_Destinations.Group_id eq it.groupId)
                    }
                }

                call.respond(HttpStatusCode.OK)
            }

            // Leave group

            // Get details of group
            @KtorExperimentalLocationsAPI
            @Location("/groups/{id}")
            data class GroupInfoLocation(val id: Int)
            get<GroupInfoLocation> {
                var responseCode = HttpStatusCode.InternalServerError
                var group: Group? = null

                transaction(db) {
                    val myGroupIds = Group_Membership
                        .select { Group_Membership.Uid eq me.id }
                        .map { it[Group_Membership.Gid] }

                    if (it.id in myGroupIds) {
                        group = Group1
                            .select { Group1.Group_ID eq it.id }
                            .firstOrNull()
                            ?.let {
                                Group(
                                    it[Group1.Group_ID],
                                    it[Group1.label],
                                    it[Group1.isTemp],
                                    it[Group1.group_leader],
                                )
                            }
                    } else {
                        responseCode = HttpStatusCode.Forbidden
                    }
                }

                if (group != null) {
                    call.respond(responseCode, group!!)
                } else {
                    call.respond(responseCode)
                }
            }

            @KtorExperimentalLocationsAPI
            @Location("/groups/{id}/destinations")
            data class GroupDestinationsLocation(val id: Int)
            get<GroupDestinationsLocation> { location ->
                val destinations = transaction(db) {
                    Group_Destinations.select { Group_Destinations.Group_id eq location.id }.map { GroupDestination(it[Group_Destinations.Destination_id], it[Group_Destinations.Group_id], it[Group_Destinations.Destination_Lat],
                            it[Group_Destinations.Destination_Long],
                            it[Group_Destinations.Label],
                            it[Group_Destinations.OrderNum]
                        ) }
                }

                call.respond(destinations)
            }

            @KtorExperimentalLocationsAPI
            @Location("/groups/{id}/members")
            data class GroupMembersLocation(val id: Int)
            get<GroupMembersLocation> { location ->
                val members = transaction(db) {
                    (Group_Membership innerJoin User1).select { Group_Membership.Gid eq location.id }
                        .map { Pair(it[User1.Username], it[User1.Uid]) }
                }

                call.respond(members)
            }

            @KtorExperimentalLocationsAPI
            @Location("/groups/{gid}/members/{uid}/delete")
            data class DeleteGroupMemberLocation(val gid: Int, val uid: Int)
            get<DeleteGroupMemberLocation> { location ->
                transaction(db) {
                    Group_Membership.deleteWhere {
                        Group_Membership.Gid eq location.gid and (Group_Membership.Uid eq location.uid)
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
            @KtorExperimentalLocationsAPI
            @Location("/groups/{id}/reorder_destinations")
            data class ReorderRoutesLocation(val id: Int)
            locationsPost<ReorderRoutesLocation> {
                // First number in the pair is the destination id and the second number is the new order num
                val newOrder = call.receive<List<Pair<Int, Int>>>()
                transaction(db) {
                    for ((destinationId, newOrderNum) in newOrder) {
                        Group_Destinations
                            .update({ (Group_Destinations.Group_id eq it.id) and (Group_Destinations.Destination_id eq destinationId) }) {
                                it[OrderNum] = newOrderNum
                            }
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

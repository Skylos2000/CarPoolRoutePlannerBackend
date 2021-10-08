package com.github.skylos2000.routes

import com.github.skylos2000.db.GroupInvites
import com.github.skylos2000.db.Group_Membership
import com.github.skylos2000.plugins.me
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun getRandomString(
    charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9'),
    length: Int = 8
) = (1..length)
    .map { kotlin.random.Random.nextInt(0, charPool.size) }
    .map(charPool::get)
    .joinToString("")

fun Application.initGroupInviteRoutes(db: Database) {
    routing {
        authenticate("auth-jwt") {
            route("/groups/invites") {
                // Get/create an invite
                // If an invite already exists for the given group that same invite is used
                post("/get_invite") {
                    // Retrieve group id from body of request
                    val groupId = call.receive<Int>()

                    // Check if this user has permission to create this group
                    // TODO: Should only the group leader be able to create invites? Maybe make this a setting?
                    val hasPermissionToCreateInvite = transaction(db) {
                        val myGroups = Group_Membership
                            .select { Group_Membership.Uid eq me.id }
                            .map { it[Group_Membership.Gid] }

                        return@transaction groupId in myGroups
                    }

                    if (!hasPermissionToCreateInvite) {
                        call.respond(HttpStatusCode.Forbidden, "The signed in user is not a part of this group")
                        return@post
                    }

                    // Get or create the invite code
                    // TODO: Check if the code itself already exists
                    val inviteCode = transaction(db) {
                        // Get existing invite codes for this group if there are any
                        val inviteCodes = GroupInvites
                            .select { GroupInvites.Gid eq groupId }
                            .map { it[GroupInvites.InviteId] }

                        // Check if there's an invite code for this group already
                        if (inviteCodes.isNotEmpty()) {
                            return@transaction inviteCodes[0]
                        } else {
                            // If an invite code does not exist make a new one
                            val newInviteCode = getRandomString()

                            // Insert invite code into db
                            GroupInvites.insert {
                                it[Gid] = groupId
                                it[InviteId] = newInviteCode
                            }

                            // Return invite code
                            return@transaction newInviteCode
                        }
                    }

                    // Return the invite code
                    call.respond(inviteCode)
                }

                // Join group
                post("/join_group") {
                    val inviteCode = call.receive<String>()

                    // TODO: Figure out a cleaner way to do this
                    data class Result(val statusCode: HttpStatusCode, val message: String)
                    val result = transaction(db) {
                        // Get the group id that corresponds to this invite
                        val groupId = GroupInvites
                            .select { GroupInvites.InviteId eq inviteCode }
                            .map { it[GroupInvites.Gid] }
                            .firstOrNull() ?: return@transaction Result(HttpStatusCode.BadRequest, "Invite code does not exist")

                        // Check if the user is already a member of that group
                        val notAlreadyMemberOfGroup = Group_Membership
                            .select { (Group_Membership.Gid eq groupId) and (Group_Membership.Uid eq me.id) }
                            .map { it[Group_Membership.Uid] }
                            .isEmpty()

                        // If they aren't add them to it
                        if (notAlreadyMemberOfGroup) {
                            Group_Membership.insert {
                                it[Gid] = groupId
                                it[Uid] = me.id
                                it[User_Lat] = me.defaultPickupLatitude
                                it[User_Long] = me.defaultPickupLongitude
                            }
                        } else {
                            // If they are already a part of the group respond with OK
                            return@transaction Result(HttpStatusCode.OK, "Already member of group")
                        }

                        // If everything went OK respond with OK
                        return@transaction Result(HttpStatusCode.OK, "")
                    }

                    call.respond(result.statusCode, result.message)
                }

                // Delete invite code
            }
        }
    }
}


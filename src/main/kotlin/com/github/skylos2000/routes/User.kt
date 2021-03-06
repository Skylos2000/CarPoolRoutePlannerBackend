package com.github.skylos2000.routes

import com.github.skylos2000.SerializedGroup
import com.github.skylos2000.SerializedUser
import com.github.skylos2000.db.Group1
import com.github.skylos2000.db.Group_Membership
import com.github.skylos2000.db.User1
import com.github.skylos2000.plugins.me
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class NewUser(val username: String, val password: String, val email: String)

fun Application.initUserRoutes(db: Database) {
    routing {
        authenticate("auth-jwt") {
            // Get user info
            get("/users/me") {
                val groups = transaction(db) {
                    val gids = Group_Membership
                        .select { Group_Membership.Uid eq me.id }
                        .map { it[Group_Membership.Gid] }

                    Group1.select { Group1.Group_ID inList gids }
                        .map { SerializedGroup(it[Group1.Group_ID], it[Group1.label], it[Group1.group_leader]) }
                }

                call.respond(SerializedUser(me.id, me.email, me.username, groups))
            }

            // Get user groups
            get("/users/me/groups") {
                val groupIds = transaction(db) {
                    Group_Membership
                        .select { Group_Membership.Uid eq me.id }
                        .map { it[Group_Membership.Gid] }
                }

                call.respond(groupIds.toString())
            }

            // Change email
            post("/users/me/change_email") {
                val newEmail = call.receive<String>()

                transaction(db) {
                    User1.update({ User1.Uid eq me.id}) { it[Email] = newEmail }
                }
            }

            // Change password
        }

        // Sign up
        post("/users/signup") {
            val newUserData = call.receive<NewUser>()

            // TODO: Are all these the right HTTP error code?

            var responseCode = HttpStatusCode.InternalServerError
            var message = "An unknown error may have occurred"

            transaction(db) {
                val userDoesNotExist = User1
                    .select { (User1.Username eq newUserData.username) or (User1.Email eq newUserData.email) }
                    .empty()

                if (userDoesNotExist) {
                    val insertedValues = User1.insert {
                        it[Username] = newUserData.username
                        it[Password] = newUserData.password
                        it[Email] = newUserData.email
                    }.resultedValues

                    if (insertedValues?.isNotEmpty() == true) {
                        responseCode = HttpStatusCode.OK
                        message = "User added"
                    } else {
                        responseCode = HttpStatusCode.InternalServerError
                        message = "User could not be added"
                    }
                } else {
                    responseCode = HttpStatusCode.BadRequest
                    message = "User already exists"
                }
            }

            call.respond(responseCode, message)
        }

        // Log in
        // To be implemented once JWT is done
    }
}

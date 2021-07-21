package com.github.skylos2000.plugins

import com.github.skylos2000.db.*
import io.ktor.application.*
import io.ktor.auth.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

data class UserPrincipal(val user: RowUser) : Principal

// Return a RowUser representing the user or null if the user is not authenticated.
fun ApplicationCall.getLoggedInUser() = principal<UserPrincipal>()?.user

fun Application.configureSecurity(db: Database) {
    install(Authentication) {
        basic("auth-basic") {
            realm = environment.config.property("auth.basic.realm").getString()

            validate { (username, password) ->
                return@validate transaction(db) {
                    // Select all users with the user name equal to the one given at login
                    // This returns a Enumerable (like a list but not a kotlin List) of results so we get the first one
                    //   since usernames should be unique.
                    val requestedUser = User1.select { User1.Username eq username }.firstOrNull()
                        ?: return@transaction null
                    // Get the password from that result row and compare it to the password given at login
                    if (requestedUser[User1.Password] == password) {
                        // Return either the UserIdPrincipal on successful login or null otherwise
                        // This is returned to transaction which is then returned to validate
                        return@transaction UserPrincipal(getUserFromResultRow(requestedUser))
                    } else return@transaction null
                }
            }
        }
    }

}

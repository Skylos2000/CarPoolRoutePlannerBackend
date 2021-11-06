package com.github.skylos2000.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.skylos2000.db.RowUser
import com.github.skylos2000.db.User1
import com.github.skylos2000.db.getUserFromResultRow
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

data class UserPrincipal(val user: RowUser) : Principal

@kotlinx.serialization.Serializable
data class User(val username: String, val password: String)

// Return a RowUser representing the user or null if the user is not authenticated.
fun ApplicationCall.getLoggedInUser() = principal<UserPrincipal>()?.user

// TODO: This should probably use a different exception type
val PipelineContext<Unit, ApplicationCall>.me
    get() = call.principal<UserPrincipal>()?.user ?: throw UnsupportedOperationException("User is not logged in")

fun Application.configureSecurity(db: Database) {

    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()

    // handles the actual validation of the received token
    install(Authentication) {
        jwt("auth-jwt") {
            realm = myRealm

            // verifies token format and signature
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )

            // performs additional validations on the token payload (i.e., checks that the user is in the database)
            validate { credential ->
//                if (credential.payload.getClaim("username").asString() != "") {
//                    JWTPrincipal(credential.payload)
//                }
//                else {
//                    null
//                }
                return@validate transaction(db) {
                    // Select all users with the user name equal to the one given at login
                    // This returns a Enumerable (like a list but not a kotlin List) of results so we get the first one
                    //   since usernames should be unique.
                    val requestedUser = User1.select { User1.Username eq credential.payload.getClaim("username").asString() }.firstOrNull()
                        ?: return@transaction null
                    // Get the password from that result row and compare it to the password given at login
                    if (requestedUser != null) {
                        // Return either the UserIdPrincipal on successful login or null otherwise
                        // This is returned to transaction which is then returned to validate
                        return@transaction UserPrincipal(getUserFromResultRow(requestedUser))
                    } else return@transaction null
                }
            }
        }

//        oauth("auth-oath-facebook") {
//            providerLookup = {
//                OAuthServerSettings.OAuth2ServerSettings(
//                    name = "facebook",
//                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
//                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
//                    requestMethod = HttpMethod.Post,
//                    clientId = System.getenv("GOOGLE_CLIENT_ID"),
//                    clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
//                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile")
//                )
//            }
//        }
    }

    routing {
        post("/login") {
            //val user = call.getLoggedInUser()
            val user = call.receive<User>()
            // Check username and password
            val verification = transaction(db) {
                // Select all users with the user name equal to the one given at login
                // This returns a Enumerable (like a list but not a kotlin List) of results so we get the first one
                //   since usernames should be unique.
                val requestedUser =
                    User1.select { User1.Username eq user.username }
                        .firstOrNull()
                        ?: return@transaction null
                // Get the password from that result row and compare it to the password given at login
                if (requestedUser[User1.Password] == user.password) {
                    // Return either the UserIdPrincipal on successful login or null otherwise
                    // This is returned to transaction which is then returned to validate
                    return@transaction UserPrincipal(getUserFromResultRow(requestedUser))
                } else return@transaction null
            }

            // if the verification is successful, then a token is generated and returned
            if (verification != null) {
                val token = JWT.create()
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .withClaim("username", user.username)
                    //.withExpiresAt(Date(System.currentTimeMillis() + 60000))
                    .sign(Algorithm.HMAC256(secret))
                call.respond(token)
                //call.respond(hashMapOf("token" to token))
            }
        }
    }
}

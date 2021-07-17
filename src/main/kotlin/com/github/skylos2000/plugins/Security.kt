package com.github.skylos2000.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

// Return the username or null if the user is not authenticated.
fun ApplicationCall.getUsername() = principal<UserIdPrincipal>()?.name

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            // realm = environment.config.property("jwt.realm").toString()

            val secret = environment.config.property("auth.jwt.secret").getString()
            verifier(JWT
                .require(Algorithm.HMAC256(secret))
                .build()
            )

            validate {
                val username = it.payload.getClaim("username").toString()
                val password = it.payload.getClaim("password").toString()
                if (password == "hunter2"){
                    UserIdPrincipal(username)
                }
                else null
            }
        }
        oauth("auth-oauth-google") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = System.getenv("GOOGLE_CLIENT_ID"),
                    clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile")
                )
            }
            client = HttpClient(Apache)
        }
    }

    routing {
        authenticate("auth-oauth-google") {
            get("login") {
                call.respondRedirect("/callback")
            }

            get("/callback") {
                call.respondRedirect("/hello")
            }
        }

        authenticate("auth-jwt") {
            get("/test_auth") {
                call.respondText("Yo yo wuddup, ${call.getUsername()}")
            }
        }
    }
}

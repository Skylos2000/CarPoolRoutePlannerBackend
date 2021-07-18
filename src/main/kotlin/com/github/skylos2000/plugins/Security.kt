package com.github.skylos2000.plugins

import io.ktor.application.*
import io.ktor.auth.*

// Return the username or null if the user is not authenticated.
fun ApplicationCall.getUsername() = principal<UserIdPrincipal>()?.name

fun Application.configureSecurity() {
    install(Authentication) {
        basic("auth-basic") {
            realm = environment.config.property("auth.basic.realm").getString()

            validate {
                // TODO: Integrate with database user and password storage
                if (it.name == "bob" && it.password == "password") {
                    UserIdPrincipal(it.name)
                } else null
            }
        }
    }

}

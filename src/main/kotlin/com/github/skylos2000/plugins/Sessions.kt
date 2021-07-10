package com.github.skylos2000.plugins

import io.ktor.application.*
import io.ktor.sessions.*

data class UserSession(val token: String)

fun Application.configureSessions() {
    install(Sessions) {
        header<UserSession>("user_session")
    }
}
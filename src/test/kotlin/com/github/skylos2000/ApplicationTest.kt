package com.github.skylos2000


import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.server.testing.*
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.assertEquals
import org.junit.Test

// Source: https://github.com/ktorio/ktor/blob/main/ktor-features/ktor-auth/jvm/test/io/ktor/tests/auth/OAuth2.kt
@InternalAPI // TODO: Figure out how to encode into base64 without using internal APIs
private fun TestApplicationEngine.handleRequestWithBasic(url: String, user: String, pass: String) =
    handleRequest {
        uri = url

        val up = "$user:$pass"
        val encoded = up.toByteArray(Charsets.ISO_8859_1).encodeBase64()
        addHeader(HttpHeaders.Authorization, "Basic $encoded")
    }


@InternalAPI
class ApplicationTest {

    val user = "aaa"
    val pass = "eee"

    private val testEnv = createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }
    @Test
    fun testRoot() {
        withApplication(testEnv) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Hello World!", response.content)
            }
        }
    }
}